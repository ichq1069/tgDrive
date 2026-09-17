package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.skydevs.tgdrive.dto.ConfigForm;
import com.skydevs.tgdrive.exception.config.ConfigFileNotFoundException;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.ConfigService;
import com.skydevs.tgdrive.service.TelegramBotService;
import com.skydevs.tgdrive.utils.StringUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Slf4j
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;
    private final TelegramBotService telegramBotService;

    @SaCheckRole("admin")
    @GetMapping()
    public Result<ConfigForm> getConfig(@NotBlank(message = "配置名不能为空") @RequestParam String name) {
        ConfigForm config = configService.get(name);
        if (config == null) {
            log.error("配置获取失败，请检查文件名是否错误");
            throw new ConfigFileNotFoundException();
        }
        log.info("获取数据成功");
        return Result.success(config);
    }

    @SaCheckRole("admin")
    @GetMapping("/configs")
    public Result<List<ConfigForm>> getConfigs() {
        List<ConfigForm> configForms = configService.getForms();
        return Result.success(configForms);
    }

    @SaCheckRole("admin")
    @PostMapping()
    public Result<String> submitConfig(@Valid @RequestBody ConfigForm configForm) {
        String name = configForm.getName();
        configForm.setName(name.trim());
        configService.save(configForm);

        try {
            telegramBotService.initializeBot(name);
            log.info("配置保存成功并已初始化Bot: {}", name);
        } catch (Exception e) {
            log.warn("配置保存成功但Bot初始化失败: {}", e.getMessage());
            return Result.success("配置保存成功，但Bot初始化失败: " + e.getMessage());
        }

        return Result.success("配置保存成功");
    }

    @SaCheckRole("admin")
    @DeleteMapping("/{name}")
    public Result<String> deleteConfig(@NotBlank(message = "配置名不能为空") @PathVariable("name") String name) {
        configService.delete(name);
        log.info("配置删除成功: {}", name);
        return Result.success("配置删除成功");
    }

    @SaCheckRole("admin")
    @GetMapping("/{name}")
    public Result<String> loadConfig(@NotBlank(message = "配置名不能为空") @PathVariable("name") String name) {
        telegramBotService.initializeBot(name);
        log.info("加载配置成功");
        return Result.success("配置加载成功");
    }

    @SaCheckRole("admin")
    @GetMapping("/bot-info")
    public Result<Map<String, Object>> getBotInfo() {
        if (!telegramBotService.isInitialized()) {
            return Result.success(null);
        }
        try {
            User user = telegramBotService.getMe();
            if (user == null) {
                return Result.success(null);
            }
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("id", user.id());
            info.put("username", user.username());
            info.put("firstName", user.firstName());
            info.put("lastName", user.lastName());
            info.put("canJoinGroups", user.canJoinGroups());
            info.put("canReadAllGroupMessages", user.canReadAllGroupMessages());
            info.put("supportsInlineQueries", user.supportsInlineQueries());
            return Result.success(info);
        } catch (Exception e) {
            log.error("获取Bot信息失败: {}", e.getMessage());
            return Result.success(null);
        }
    }

    @SaCheckRole("admin")
    @GetMapping("/chat-ids")
    public Result<List<Map<String, Object>>> getChatIds() {
        if (!telegramBotService.isInitialized()) {
            return Result.error("Bot未初始化");
        }
        try {
            List<Update> updates = telegramBotService.getUpdates(0);
            if (updates == null || updates.isEmpty()) {
                return Result.success(new ArrayList<>());
            }

            Map<Long, Map<String, Object>> chatMap = new LinkedHashMap<>();
            for (Update update : updates) {
                if (update.message() != null && update.message().chat() != null) {
                    com.pengrad.telegrambot.model.Chat chat = update.message().chat();
                    Long chatId = chat.id();
                    if (!chatMap.containsKey(chatId)) {
                        Map<String, Object> chatInfo = new LinkedHashMap<>();
                        chatInfo.put("id", chatId);
                        chatInfo.put("title", chat.title());
                        chatInfo.put("type", chat.type().toString());
                        chatInfo.put("username", chat.username());
                        chatMap.put(chatId, chatInfo);
                    }
                }
            }

            return Result.success(new ArrayList<>(chatMap.values()));
        } catch (Exception e) {
            log.error("获取群组ID失败: {}", e.getMessage());
            return Result.error("获取群组ID失败: " + e.getMessage());
        }
    }
}
