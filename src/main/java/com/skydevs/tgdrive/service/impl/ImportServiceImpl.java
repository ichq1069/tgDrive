package com.skydevs.tgdrive.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.mapper.FileMapper;
import com.skydevs.tgdrive.service.FileStorageService;
import com.skydevs.tgdrive.service.ImportService;
import com.skydevs.tgdrive.utils.UserFriendly;
import com.skydevs.tgdrive.websocket.UploadProgressWebSocketHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImportServiceImpl implements ImportService {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private UploadProgressWebSocketHandler webSocketHandler;

    @Autowired
    @Qualifier("uploadTaskExecutor")
    private ThreadPoolTaskExecutor uploadTaskExecutor;

    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 60000;

    @Override
    public void importFromUrls(List<String> urls, Long userId) {
        int total = urls.size();
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        List<String> failedUrls = new CopyOnWriteArrayList<>();

        // 并发控制：根据URL数量动态调整并发度
        int concurrency = Math.min(5, Math.max(1, total));
        Semaphore semaphore = new Semaphore(concurrency);

        webSocketHandler.sendImportStart(total);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < total; i++) {
            final String url = urls.get(i);
            final int index = i;

            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String filename = extractFilename(url);
                    webSocketHandler.sendImportProgress(filename, index + 1, total, completed.get(), failed.get(), "downloading");

                    // 下载文件
                    byte[] fileBytes = downloadFile(url);
                    if (fileBytes == null || fileBytes.length == 0) {
                        throw new RuntimeException("下载文件为空");
                    }

                    // 计算文件哈希值用于去重检测
                    String fileHash = calculateFileHash(fileBytes);
                    
                    // 检查是否已存在相同哈希的文件
                    FileInfo existingFile = fileMapper.getFileByHash(fileHash);
                    if (existingFile != null) {
                        int skipCount = skipped.incrementAndGet();
                        log.info("URL导入跳过重复文件: {} -> 已有文件 {}", url, existingFile.getFileId());
                        webSocketHandler.sendImportProgress(filename, index + 1, total, completed.get(), failed.get(), "skipped");
                        return;
                    }

                    webSocketHandler.sendImportProgress(filename, index + 1, total, completed.get(), failed.get(), "uploading");

                    // 上传到 Telegram
                    InputStream inputStream = new ByteArrayInputStream(fileBytes);
                    String fileId = fileStorageService.uploadFile(inputStream, filename, (long) fileBytes.length);

                    // 构建下载链接
                    String downloadUrl = "/d/" + fileId;

                    // 入库
                    FileInfo fileInfo = FileInfo.builder()
                            .fileId(fileId)
                            .fileName(filename)
                            .size(UserFriendly.humanReadableFileSize(fileBytes.length))
                            .fullSize((long) fileBytes.length)
                            .uploadTime(LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC))
                            .downloadUrl(downloadUrl)
                            .userId(userId)
                            .library("tele")
                            .fileHash(fileHash)
                            .build();
                    fileMapper.insertFile(fileInfo);

                    int done = completed.incrementAndGet();
                    webSocketHandler.sendImportProgress(filename, index + 1, total, done, failed.get(), "completed");
                    log.info("URL导入成功: {} -> {}", url, filename);

                } catch (Exception e) {
                    int failCount = failed.incrementAndGet();
                    failedUrls.add(url);
                    webSocketHandler.sendImportProgress(url, index + 1, total, completed.get(), failCount, "failed");
                    log.error("URL导入失败: {} -> {}", url, e.getMessage());
                } finally {
                    semaphore.release();
                }
            }, uploadTaskExecutor);

            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((v, ex) -> {
                    webSocketHandler.sendImportComplete(total, completed.get(), failed.get(), failedUrls);
                    log.info("URL导入任务完成: 总计={}, 成功={}, 失败={}, 跳过重复={}", total, completed.get(), failed.get(), skipped.get());
                });
    }

    /**
     * 计算文件SHA-256哈希值
     */
    private String calculateFileHash(byte[] fileBytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(fileBytes);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private byte[] downloadFile(String fileUrl) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(fileUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestProperty("User-Agent", "tgDrive/1.0");
            conn.setInstanceFollowRedirects(true);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HTTP " + responseCode);
            }

            try (InputStream is = conn.getInputStream();
                 java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream()) {
                byte[] data = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, bytesRead);
                }
                return buffer.toByteArray();
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String extractFilename(String fileUrl) {
        try {
            URL url = new URL(fileUrl);
            String path = url.getPath();
            if (path == null || path.isEmpty() || "/".equals(path)) {
                return "imported_file_" + System.currentTimeMillis();
            }
            String filename = path.substring(path.lastIndexOf('/') + 1);
            if (filename.isEmpty()) {
                return "imported_file_" + System.currentTimeMillis();
            }
            // URL decode
            filename = URLDecoder.decode(filename, StandardCharsets.UTF_8.name());
            // 移除查询参数残留
            int queryIndex = filename.indexOf('?');
            if (queryIndex > 0) {
                filename = filename.substring(0, queryIndex);
            }
            return filename;
        } catch (Exception e) {
            return "imported_file_" + System.currentTimeMillis();
        }
    }
}
