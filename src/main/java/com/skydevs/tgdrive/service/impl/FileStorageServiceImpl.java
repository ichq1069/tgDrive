package com.skydevs.tgdrive.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendAudio;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.request.SendVideo;
import com.pengrad.telegrambot.response.SendResponse;
import com.skydevs.tgdrive.dto.UploadFile;
import com.skydevs.tgdrive.entity.BigFileInfo;
import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.entity.User;
import com.skydevs.tgdrive.exception.user.InsufficientPermissionException;
import com.skydevs.tgdrive.exception.file.UploadFileIsNullException;
import com.skydevs.tgdrive.mapper.FileMapper;
import com.skydevs.tgdrive.mapper.TagMapper;
import com.skydevs.tgdrive.mapper.UserMapper;
import com.skydevs.tgdrive.result.PageResult;
import com.skydevs.tgdrive.service.FileStorageService;
import com.skydevs.tgdrive.service.TagRuleService;
import com.skydevs.tgdrive.service.TagService;
import com.skydevs.tgdrive.service.TelegramBotService;
import com.skydevs.tgdrive.utils.StringUtil;
import com.skydevs.tgdrive.utils.UserFriendly;
import com.skydevs.tgdrive.websocket.UploadProgressWebSocketHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于Telegram的文件存储服务实现
 */
@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {
    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TelegramBotService telegramBotService;

    @Autowired
    private UploadProgressWebSocketHandler uploadProgressWebSocketHandler;

    @Autowired
    private TagRuleService tagRuleService;

    @Autowired
    private TagService tagService;

    @Autowired
    @Qualifier("uploadTaskExecutor")
    private ThreadPoolTaskExecutor uploadTaskExecutor;

    // tg bot接口限制20MB，传10MB是最佳实践
    private final int MAX_FILE_SIZE = 10 * 1024 * 1024;
    // 控制同时运行的任务数量
    private final int PERMITS = 5;

    @Override
    public UploadFile getUploadFile(MultipartFile multipartFile, HttpServletRequest request, Long userId) {
        UploadFile uploadFile = new UploadFile();
        String downloadUrl;
        if (multipartFile != null && !multipartFile.isEmpty()) {
            try (InputStream inputStream = multipartFile.getInputStream()) {
                // 优先使用自定义URL，如果没有配置则使用请求中的URL
                String prefix = StringUtil.getPrefix(request);
                String filename = multipartFile.getOriginalFilename();
                long size = multipartFile.getSize();

                // 计算文件哈希值用于去重检测
                String fileHash = calculateFileHash(multipartFile.getInputStream());
                
                // 检查是否已存在相同哈希的文件
                FileInfo existingFile = fileMapper.getFileByHash(fileHash);
                if (existingFile != null) {
                    log.info("检测到重复文件，返回已有记录: {} -> {}", filename, existingFile.getFileId());
                    uploadFile.setFileName(filename);
                    uploadFile.setDownloadLink(existingFile.getDownloadUrl());
                    return uploadFile;
                }
                
                // 使用FileStorageService上传文件
                String fileID = uploadFile(inputStream, filename, size);
                
                // 保存文件哈希到数据库
                fileMapper.updateFileHash(fileID, fileHash);
                
                // 无论大小，上传流程成功后发送完成消息
                uploadProgressWebSocketHandler.sendUploadComplete(filename);

                downloadUrl = prefix + "/d/" + fileID;

                // 保存文件信息到数据库
                String contentLevel = null;
                User user = userMapper.getUserById(userId);
                if (user != null && user.getMemberLevel() != null) {
                    contentLevel = user.getMemberLevel();
                }

                // 确定 library：vvip 内容直接进入私密库
                String targetLibrary = "vvip".equals(contentLevel) ? "private" : "tele";

                FileInfo fileInfo = FileInfo.builder()
                        .fileId(fileID)
                        .size(UserFriendly.humanReadableFileSize(size))
                        .fullSize(size)
                        .uploadTime(LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC))
                        .downloadUrl(downloadUrl)
                        .fileName(filename)
                        .userId(userId)
                        .library(targetLibrary)
                        .fileHash(fileHash)
                        .contentLevel(contentLevel)
                        .build();
                fileMapper.insertFile(fileInfo);

                // Auto-apply tags based on rules
                List<Long> matchedTagIds = tagRuleService.matchTagsForFile(filename);
                if (!matchedTagIds.isEmpty()) {
                    tagService.addFileTags(fileID, matchedTagIds);
                    log.info("自动打标签: {} -> tags={}", filename, matchedTagIds);
                }
            } catch (IOException e) {
                log.error("文件上传失败，响应信息：{}", e.getMessage());
                throw new RuntimeException("文件上传失败");
            }
        } else {
            throw new UploadFileIsNullException();
        }

        uploadFile.setFileName(multipartFile.getOriginalFilename());
        uploadFile.setDownloadLink(downloadUrl);
        return uploadFile;
    }

    public String uploadFile(InputStream inputStream, String filename, long size) {
        if (size > MAX_FILE_SIZE) {
            return uploadLargeFile(inputStream, filename, size);
        } else {
            return uploadSmallFile(inputStream, filename);
        }
    }

    /**
     * 计算文件SHA-256哈希值（用于去重检测）
     */
    private String calculateFileHash(InputStream inputStream) throws IOException {
        java.security.MessageDigest digest;
        try {
            digest = java.security.MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }
        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 检查文件是否为媒体类型
     */
    private boolean isImageFile(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") 
            || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp");
    }

    private boolean isVideoFile(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mkv") 
            || lower.endsWith(".mov") || lower.endsWith(".avi");
    }

    private boolean isAudioFile(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".flac") 
            || lower.endsWith(".aac") || lower.endsWith(".ogg");
    }

    private String uploadLargeFile(InputStream inputStream, String filename, long size) {
        try {
            List<String> fileIds = sendFileStreamInChunks(inputStream, filename);
            return createRecordFile(filename, size, fileIds);
        } catch (Exception e) {
            log.error("大文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("大文件上传失败", e);
        }
    }

    /**
     * 上传小文件
     */
    private String uploadSmallFile(InputStream inputStream, String filename) {
        try {
            // 发送单文件上传进度
            uploadProgressWebSocketHandler.sendUploadProgress(filename, 0, 0, 1);

            // 小于10MB的GIF会被TG转换为MP4，对文件后缀进行处理
            String uploadFilename = filename;
            if (filename != null && filename.endsWith(".gif")) {
                uploadFilename = filename.substring(0, filename.lastIndexOf(".gif"));
            }

            // 根据文件类型选择不同的发送方式
            Message message;
            if (isImageFile(uploadFilename)) {
                message = sendPhoto(inputStream, uploadFilename);
            } else if (isVideoFile(uploadFilename)) {
                message = sendVideo(inputStream, uploadFilename);
            } else if (isAudioFile(uploadFilename)) {
                message = sendAudio(inputStream, uploadFilename);
            } else {
                message = sendDocument(inputStream, uploadFilename);
            }
            
            String fileID = StringUtil.extractFileId(message);
            Integer messageID=message.messageId();

            // 发送上传完成进度
            uploadProgressWebSocketHandler.sendUploadProgress(filename, 100, 1, 1);
            uploadProgressWebSocketHandler.sendUploadComplete(filename);

            log.info("小文件上传成功，File ID：{}， 文件名：{}", fileID, filename);
            return fileID;
        } catch (Exception e) {
            log.error("小文件上传失败: {}", e.getMessage(), e);
            uploadProgressWebSocketHandler.sendUploadError(filename, "文件上传失败: " + e.getMessage());
            throw new RuntimeException("小文件上传失败", e);
        }
    }

    /**
     * 分块上传文件
     */
    private List<String> sendFileStreamInChunks(InputStream inputStream, String filename) {
        List<CompletableFuture<String>> futures = new ArrayList<>();
        Semaphore semaphore = new Semaphore(PERMITS);

        final AtomicInteger totalChunks = new AtomicInteger(0);
        final AtomicInteger completedChunks = new AtomicInteger(0);

        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
            byte[] buffer = new byte[MAX_FILE_SIZE];
            int partIndex = 0;
            List<byte[]> allChunks = new ArrayList<>();

            // 第一遍：读取所有分块数据
            while (true) {
                int offset = 0;
                while(offset < MAX_FILE_SIZE) {
                    int byteRead = bufferedInputStream.read(buffer, offset, MAX_FILE_SIZE - offset);
                    if (byteRead == -1) {
                        break;
                    }
                    offset += byteRead;
                }

                if (offset == 0) {
                    break;
                }

                byte[] chunkData = Arrays.copyOf(buffer, offset);
                allChunks.add(chunkData);
                partIndex++;
            }

            totalChunks.set(allChunks.size());
            log.info("文件 {} 将被分为 {} 个分块上传", filename, totalChunks.get());

            // 第二遍：提交所有上传任务
            for (int i = 0; i < allChunks.size(); i++) {
                final int chunkIndex = i;
                final byte[] chunkData = allChunks.get(i);
                final String partName = filename + "_part" + chunkIndex;

                semaphore.acquire();

                CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        Message message = sendDocument(chunkData, partName);
                        String fileID = StringUtil.extractFileId(message);

                        if (fileID != null) {
                            log.info("分块上传成功，File ID：{}， 文件名：{}", fileID, partName);

                            // 更新进度
                            int completed = completedChunks.incrementAndGet();
                            double percentage = (double) completed / totalChunks.get() * 100;
                            uploadProgressWebSocketHandler.sendUploadProgress(filename, percentage, completed, totalChunks.get());

                            return fileID;
                        } else {
                            throw new RuntimeException("分块 " + partName + " 上传失败：无法获取文件ID");
                        }
                    } catch (Exception e) {
                        uploadProgressWebSocketHandler.sendUploadError(filename, "分块 " + partName + " 上传失败");
                        throw new RuntimeException("分块 " + partName + " 上传失败", e);
                    } finally {
                        semaphore.release();
                    }
                }, uploadTaskExecutor);
                futures.add(future);
            }

            // 等待所有任务完成并按顺序获取结果
            List<String> fileIds = new ArrayList<>();
            try {
                for (CompletableFuture<String> future : futures) {
                    fileIds.add(future.join());
                }
                return fileIds;
            } catch (CompletionException e) {
                uploadProgressWebSocketHandler.sendUploadError(filename, "分块上传失败: " + e.getCause().getMessage());
                for (CompletableFuture<String> future : futures) {
                    future.cancel(true);
                }
                throw new RuntimeException("分块上传失败: " + e.getCause().getMessage(), e);
            }
        } catch (IOException | InterruptedException e) {
            log.error("文件流读取失败或上传失败：{}", e.getMessage());
            uploadProgressWebSocketHandler.sendUploadError(filename, "文件流读取失败或上传失败: " + e.getMessage());
            throw new RuntimeException("文件流读取失败或上传失败", e);
        }
    }

    /**
     * 创建记录文件
     */
    private String createRecordFile(String originalFileName, long fileSize, List<String> fileIds) throws IOException {
        BigFileInfo record = new BigFileInfo();
        record.setFileName(originalFileName);
        record.setFileSize(fileSize);
        record.setFileIds(fileIds);
        record.setRecordFile(true);

        // 创建一个系统临时文件
        Path tempDir = Files.createTempDirectory("tempDir");
        String hashString = DigestUtil.sha256Hex(originalFileName);
        Path tempFile = tempDir.resolve(hashString + ".record.json");
        Files.createFile(tempFile);

        try {
            String jsonString = JSON.toJSONString(record, true);
            Files.write(Paths.get(tempFile.toUri()), jsonString.getBytes());
        } catch (IOException e) {
            log.error("上传记录文件生成失败: {}", e.getMessage());
            throw new RuntimeException("上传文件生成失败", e);
        }

        // 上传记录文件到 Telegram
        byte[] fileBytes = Files.readAllBytes(tempFile);
        Message message = sendDocument(fileBytes, tempFile.getFileName().toString());
        String recordFileId = StringUtil.extractFileId(message);

        log.info("记录文件上传成功，File ID: {}", recordFileId);

        // 删除本地临时文件
        Files.deleteIfExists(tempFile);

        return recordFileId;
    }

    /**
     * 获取文件分页
     * @param page 页码
     * @param size 每页数量
     * @return 分页结果
     */
    @Override
    public PageResult getFileList(int page, int size, String keyword, Long userId, String role) {
//        todo 数据库丢了会很麻烦，1，文件无法展示，虽然现有的也图片展示也没什么用，但是无法获取图床链接还是很麻烦 2，不清楚webdav的同步机制是如何做的，核心问题问题在于fileinfo中是如何定义图片的链接，也就是从tg中获取文件，tg文件列表是否具备分级结构？
        /**
         * 1,假设数据库文件丢失
         * （1）真实文件： tg、webdav的本地挂载
         * （2）层级信息： 数据库、webdav的本地挂载
         *
         *
         *
         *   核心问题，备份文件的保存，无论如何这里只能解决webDAV的同步问题，如果层级信息本身无法保存，那么就无从恢复，
         *   想法1：
         *   场景： 数据库文件丢失，存在tg频道的真实文件，无webDAV
         *   使用第三方保存，例如github仓库，启动时恢复数据库
         *   场景2： 数据库文件丢失，tg频道未丢失，webDAV存在，
         *   webDAV同步至频道时的文件对应关系
         *   场景3： 数据库文件丢失，tg频道未丢失，webDAV未丢失，
         *   webDAV同步至频道时的文件对应关系
         *
         *

         *
         *   目前疑问：
         *   1，webDAV如何同步
         *   2，github设置定时任务同步仓库文件
         *
         *          *
         *          * 1，保证不同名，如果同名也不要紧，只要在tg频道中搜索同名文件即可
         *              问题： 挂载多次会导致上传多次，可能会触发tg频道的文件空间限制
         *          * 2，
         *          *
         *
         *   此外：
         *
         *  1，项目简介里的核心优势都是如何实现的？
         *
         *
         */

        PageHelper.startPage(page, size);
        List<FileInfo> fileInfoList = fileMapper.getFilteredFiles(keyword, userId, role);
        PageInfo<FileInfo> pageInfo = new PageInfo<>(fileInfoList);
        log.info("文件分页查询");
        return new PageResult((int) pageInfo.getTotal(), pageInfo.getList());
    }

    /**
     * 更新文件url
     */
    @Override
    public void updateUrl(HttpServletRequest request) {
        String prefix = StringUtil.getPrefix(request);
        fileMapper.updateUrl(prefix);
    }

    /**
     * 根据文件ID删除文件
     * @param fileId 文件ID
     */
    @Override
    public void deleteFile(String fileId, Long userId, String role) {
        FileInfo file = fileMapper.getFileByFileId(fileId);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }
        if ("admin".equals(role) || (file.getUserId() != null && file.getUserId().equals(userId))) {
            try {
                tagMapper.deleteAssociationsByFileId(fileId);
                fileMapper.deleteFile(fileId);
                log.info("文件删除成功，fileId: {}", fileId);
            } catch (Exception e) {
                log.error("文件删除失败，fileId: {}", fileId, e);
                throw new RuntimeException("文件删除失败", e);
            }
        } else {
            throw new InsufficientPermissionException("无权限删除此文件");
        }
    }

    @Override
    public void updateIsPublic(String fileId, boolean isPublic, Long userId, String role) {
        FileInfo file = fileMapper.getFileByFileId(fileId);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }
        if ("admin".equals(role) || (file.getUserId() != null && file.getUserId().equals(userId))) {
            fileMapper.updateIsPublic(fileId, isPublic);
        } else {
            throw new InsufficientPermissionException("无权限更新此文件");
        }
    }

    /**
     * Description:
     * 调用bot上传文件
     * @author SkyDev
     * @date 2025-08-01 17:36:24
     * @param fileData 文件
     * @param filename 文件名
     * @return 上传文件的返回信息
     */
    private Message sendDocument(byte[] fileData, String filename) {
        TelegramBot bot = telegramBotService.getBot();
        String chatId = telegramBotService.getChatId();
        int retryCount = 3;
        int baseDelay = 1000;

        if (bot == null) {
            throw new RuntimeException("Telegram Bot 未初始化，请先配置 Bot Token 和 Chat ID");
        }

        for (int i = 0; i < retryCount; i++) {
            try {
                SendDocument sendDocument = new SendDocument(chatId, fileData).fileName(filename);
                SendResponse response = bot.execute(sendDocument);

                if (response != null && response.isOk() && response.message() != null) {
                    return response.message();
                }

                // 记录 Telegram API 返回的具体错误
                String desc = response != null ? response.description() : "response is null";
                int errorCode = response != null ? response.errorCode() : -1;
                log.warn("发送文档失败 [文件: {}, 错误码: {}, 描述: {}], 正在准备第{}次重试",
                        filename, errorCode, desc, (i + 1));

                int exponentialDelay = baseDelay * (int)Math.pow(2, i);
                Thread.sleep(exponentialDelay);
            } catch (Exception e) {
                log.error("发送文档异常 [文件: {}, 第{}次尝试]: {}", filename, (i + 1), e.getMessage());
                if (i == retryCount - 1) {
                    throw new RuntimeException("发送文档失败，已达到最大重试次数: " + e.getMessage(), e);
                }
                try {
                    Thread.sleep((long) baseDelay * (int)Math.pow(2, i));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试等待被中断", ie);
                }
            }
        }

        throw new RuntimeException("发送文档失败，已达到最大重试次数");
    }

    /**
     * Description:
     * 流上传
     * @author SkyDev
     * @date 2025-08-01 17:37:53
     * @param inputStream 文件流
     * @param filename 文件名
     * @return 上传文件的返回信息
     */
    private Message sendDocument(InputStream inputStream, String filename) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192];
            int byteRead;
            while ((byteRead = inputStream.read(data)) != -1) {
                buffer.write(data, 0, byteRead);
            }
            return sendDocument(buffer.toByteArray(), filename);
        } catch (IOException e) {
            log.error("读取输入流失败: {}", e.getMessage());
            throw new RuntimeException("读取输入流失败", e);
        }
    }

    /**
     * 发送图片到Telegram（使用sendPhoto，图片会显示在相册标签中）
     */
    private Message sendPhoto(byte[] fileData, String filename) {
        TelegramBot bot = telegramBotService.getBot();
        String chatId = telegramBotService.getChatId();
        int retryCount = 3;
        int baseDelay = 1000;

        if (bot == null) {
            throw new RuntimeException("Telegram Bot 未初始化，请先配置 Bot Token 和 Chat ID");
        }

        for (int i = 0; i < retryCount; i++) {
            try {
                SendPhoto sendPhoto = new SendPhoto(chatId, fileData);
                SendResponse response = bot.execute(sendPhoto);

                if (response != null && response.isOk() && response.message() != null) {
                    return response.message();
                }

                String desc = response != null ? response.description() : "response is null";
                int errorCode = response != null ? response.errorCode() : -1;
                log.warn("发送图片失败 [文件: {}, 错误码: {}, 描述: {}], 正在准备第{}次重试",
                        filename, errorCode, desc, (i + 1));

                int exponentialDelay = baseDelay * (int)Math.pow(2, i);
                Thread.sleep(exponentialDelay);
            } catch (Exception e) {
                log.error("发送图片异常 [文件: {}, 第{}次尝试]: {}", filename, (i + 1), e.getMessage());
                if (i == retryCount - 1) {
                    throw new RuntimeException("发送图片失败，已达到最大重试次数: " + e.getMessage(), e);
                }
                try {
                    Thread.sleep((long) baseDelay * (int)Math.pow(2, i));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试等待被中断", ie);
                }
            }
        }

        throw new RuntimeException("发送图片失败，已达到最大重试次数");
    }

    private Message sendPhoto(InputStream inputStream, String filename) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192];
            int byteRead;
            while ((byteRead = inputStream.read(data)) != -1) {
                buffer.write(data, 0, byteRead);
            }
            return sendPhoto(buffer.toByteArray(), filename);
        } catch (IOException e) {
            log.error("读取输入流失败: {}", e.getMessage());
            throw new RuntimeException("读取输入流失败", e);
        }
    }

    /**
     * 发送视频到Telegram（使用sendVideo，视频会显示在视频标签中）
     */
    private Message sendVideo(byte[] fileData, String filename) {
        TelegramBot bot = telegramBotService.getBot();
        String chatId = telegramBotService.getChatId();
        int retryCount = 3;
        int baseDelay = 1000;

        if (bot == null) {
            throw new RuntimeException("Telegram Bot 未初始化，请先配置 Bot Token 和 Chat ID");
        }

        for (int i = 0; i < retryCount; i++) {
            try {
                SendVideo sendVideo = new SendVideo(chatId, fileData);
                SendResponse response = bot.execute(sendVideo);

                if (response != null && response.isOk() && response.message() != null) {
                    return response.message();
                }

                String desc = response != null ? response.description() : "response is null";
                int errorCode = response != null ? response.errorCode() : -1;
                log.warn("发送视频失败 [文件: {}, 错误码: {}, 描述: {}], 正在准备第{}次重试",
                        filename, errorCode, desc, (i + 1));

                int exponentialDelay = baseDelay * (int)Math.pow(2, i);
                Thread.sleep(exponentialDelay);
            } catch (Exception e) {
                log.error("发送视频异常 [文件: {}, 第{}次尝试]: {}", filename, (i + 1), e.getMessage());
                if (i == retryCount - 1) {
                    throw new RuntimeException("发送视频失败，已达到最大重试次数: " + e.getMessage(), e);
                }
                try {
                    Thread.sleep((long) baseDelay * (int)Math.pow(2, i));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试等待被中断", ie);
                }
            }
        }

        throw new RuntimeException("发送视频失败，已达到最大重试次数");
    }

    private Message sendVideo(InputStream inputStream, String filename) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192];
            int byteRead;
            while ((byteRead = inputStream.read(data)) != -1) {
                buffer.write(data, 0, byteRead);
            }
            return sendVideo(buffer.toByteArray(), filename);
        } catch (IOException e) {
            log.error("读取输入流失败: {}", e.getMessage());
            throw new RuntimeException("读取输入流失败", e);
        }
    }

    /**
     * 发送音频到Telegram（使用sendAudio，音频会显示在音频标签中）
     */
    private Message sendAudio(byte[] fileData, String filename) {
        TelegramBot bot = telegramBotService.getBot();
        String chatId = telegramBotService.getChatId();
        int retryCount = 3;
        int baseDelay = 1000;

        if (bot == null) {
            throw new RuntimeException("Telegram Bot 未初始化，请先配置 Bot Token 和 Chat ID");
        }

        for (int i = 0; i < retryCount; i++) {
            try {
                SendAudio sendAudio = new SendAudio(chatId, fileData);
                SendResponse response = bot.execute(sendAudio);

                if (response != null && response.isOk() && response.message() != null) {
                    return response.message();
                }

                String desc = response != null ? response.description() : "response is null";
                int errorCode = response != null ? response.errorCode() : -1;
                log.warn("发送音频失败 [文件: {}, 错误码: {}, 描述: {}], 正在准备第{}次重试",
                        filename, errorCode, desc, (i + 1));

                int exponentialDelay = baseDelay * (int)Math.pow(2, i);
                Thread.sleep(exponentialDelay);
            } catch (Exception e) {
                log.error("发送音频异常 [文件: {}, 第{}次尝试]: {}", filename, (i + 1), e.getMessage());
                if (i == retryCount - 1) {
                    throw new RuntimeException("发送音频失败，已达到最大重试次数: " + e.getMessage(), e);
                }
                try {
                    Thread.sleep((long) baseDelay * (int)Math.pow(2, i));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试等待被中断", ie);
                }
            }
        }

        throw new RuntimeException("发送音频失败，已达到最大重试次数");
    }

    private Message sendAudio(InputStream inputStream, String filename) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192];
            int byteRead;
            while ((byteRead = inputStream.read(data)) != -1) {
                buffer.write(data, 0, byteRead);
            }
            return sendAudio(buffer.toByteArray(), filename);
        } catch (IOException e) {
            log.error("读取输入流失败: {}", e.getMessage());
            throw new RuntimeException("读取输入流失败", e);
        }
    }
}
