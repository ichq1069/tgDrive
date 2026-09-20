package com.skydevs.tgdrive.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class ParseProgressWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, CopyOnWriteArrayList<WebSocketSession>> sessionsByPageUrl = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.debug("Parse progress WebSocket 连接建立: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.debug("Parse progress WebSocket 连接关闭: {}", session.getId());
        // 清理所有关联的 session
        sessionsByPageUrl.values().forEach(list -> list.removeIf(s -> s.getId().equals(session.getId())));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 前端发送订阅消息：{"pageUrl": "https://..."}
        try {
            String payload = message.getPayload();
            // 简单解析 pageUrl
            if (payload.contains("pageUrl")) {
                String pageUrl = extractPageUrl(payload);
                if (pageUrl != null) {
                    sessionsByPageUrl.computeIfAbsent(pageUrl, k -> new CopyOnWriteArrayList<>()).add(session);
                    log.debug("WebSocket 订阅解析进度: pageUrl={}", pageUrl);
                }
            }
        } catch (Exception e) {
            log.warn("处理 WebSocket 消息失败: {}", e.getMessage());
        }
    }

    public void sendProgress(String pageUrl, String status, String message) {
        CopyOnWriteArrayList<WebSocketSession> sessions = sessionsByPageUrl.get(pageUrl);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String json = String.format("{\"status\":\"%s\",\"message\":\"%s\",\"pageUrl\":\"%s\"}",
            escapeJson(status), escapeJson(message), escapeJson(pageUrl));

        TextMessage textMessage = new TextMessage(json);
        sessions.removeIf(s -> !s.isOpen());
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            } catch (Exception e) {
                log.debug("发送 WebSocket 消息失败: {}", e.getMessage());
            }
        }
    }

    public void removeSubscription(String pageUrl) {
        sessionsByPageUrl.remove(pageUrl);
    }

    private String extractPageUrl(String json) {
        // 简单提取 "pageUrl":"..." 的值
        int start = json.indexOf("\"pageUrl\"");
        if (start < 0) return null;
        int colon = json.indexOf(':', start);
        if (colon < 0) return null;
        int quoteStart = json.indexOf('"', colon + 1);
        if (quoteStart < 0) return null;
        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteEnd < 0) return null;
        return json.substring(quoteStart + 1, quoteEnd);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
