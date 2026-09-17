package com.skydevs.tgdrive.service.impl;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.GetUpdates;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.GetFileResponse;
import com.pengrad.telegrambot.response.GetMeResponse;
import com.pengrad.telegrambot.response.GetUpdatesResponse;
import com.skydevs.tgdrive.dto.ConfigForm;
import com.skydevs.tgdrive.exception.bot.BotNotSetException;
import com.skydevs.tgdrive.exception.config.ConfigFileNotFoundException;
import com.skydevs.tgdrive.exception.config.NoConfigException;
import com.skydevs.tgdrive.service.ConfigService;
import com.skydevs.tgdrive.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Telegram Bot服务实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramBotServiceImpl implements TelegramBotService {
    private final ConfigService configService;

    private String botToken;
    private String chatId;
    private String customUrl;
    private TelegramBot bot;

    @Override
    public TelegramBot getBot() {
        // 如果Bot未初始化，尝试自动初始化
        if (this.bot == null) {
            tryInitialize();
        }
        return this.bot;
    }

    /**
     * 尝试自动初始化Bot（从数据库加载最新配置）
     */
    private void tryInitialize() {
        try {
            List<com.skydevs.tgdrive.dto.ConfigForm> configForms = configService.getForms();
            if (configForms != null && !configForms.isEmpty()) {
                // 选择第一个可用的配置
                com.skydevs.tgdrive.dto.ConfigForm selectedConfig = configForms.get(0);
                
                // 优先选择名称包含"default"或"main"的配置
                for (com.skydevs.tgdrive.dto.ConfigForm config : configForms) {
                    String configName = config.getName().toLowerCase();
                    if (configName.contains("default") || configName.contains("main")) {
                        selectedConfig = config;
                        break;
                    }
                }
                
                initializeBot(selectedConfig.getName());
                log.info("自动初始化Bot成功: {}", selectedConfig.getName());
            }
        } catch (Exception e) {
            log.warn("自动初始化Bot失败: {}", e.getMessage());
        }
    }

    @Override
    public String getChatId() {
        return this.chatId;
    }

    @Override
    public void initializeBot(String name) {
        ConfigForm config = configService.get(name);
        if (config == null) {
            log.error("配置不存在");
            throw new ConfigFileNotFoundException();
        }
        this.botToken = config.getToken();
        this.chatId = config.getTarget();
        this.customUrl = config.getUrl();
        this.bot = new TelegramBot(botToken);
        log.info("Telegram Bot 初始化成功");
    }

    @Override
    public String getCustomUrl() {
        return customUrl;
    }

    @Override
    public File getFile(String fileId) {
        checkBotInitialized();
        
        GetFile getFile = new GetFile(fileId);
        try {
            GetFileResponse getFileResponse = bot.execute(getFile);
            return getFileResponse.file();
        } catch (NullPointerException e) {
            log.error("当前未加载配置文件！{}", e.getMessage());
            throw new NoConfigException("当前未加载配置文件！");
        }
    }

    @Override
    public String getFullFilePath(File file) {
        checkBotInitialized();
        return bot.getFullFilePath(file);
    }

    @Override
    public boolean sendMessage(String message) {
        checkBotInitialized();
        
        try {
            bot.execute(new SendMessage(chatId, message));
            log.info("消息发送成功");
            return true;
        } catch (Exception e) {
            log.error("消息发送失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isInitialized() {
        return bot != null && botToken != null && chatId != null;
    }

    /**
     * 检查Bot是否已初始化
     */
    private void checkBotInitialized() {
        if (!isInitialized()) {
            throw new BotNotSetException("Telegram Bot 未初始化");
        }
    }

    @Override
    public void deleteFile(Integer fileId) {
        DeleteMessage deleteMessage = new DeleteMessage(chatId, fileId);
        BaseResponse response = bot.execute(deleteMessage);

        if (!response.isOk()){
            log.error("删除原文件失败: {}， messageId: {}", response.description(), fileId);
        }
    }

    @Override
    public com.pengrad.telegrambot.model.User getMe() {
        checkBotInitialized();
        try {
            GetMeResponse response = bot.execute(new com.pengrad.telegrambot.request.GetMe());
            if (response.isOk()) {
                return response.user();
            }
            log.error("获取Bot信息失败: {}", response.description());
            return null;
        } catch (Exception e) {
            log.error("获取Bot信息异常: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public java.util.List<Update> getUpdates(Integer offset) {
        checkBotInitialized();
        try {
            GetUpdates request = new GetUpdates();
            if (offset != null) {
                request.offset(offset);
            }
            request.limit(100);
            GetUpdatesResponse response = bot.execute(request);
            if (response.isOk()) {
                return response.updates();
            }
            log.error("获取更新列表失败: {}", response.description());
            return java.util.Collections.emptyList();
        } catch (Exception e) {
            log.error("获取更新列表异常: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
}