package com.customerservice.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 维护「会话 -> 在线 WebSocket 连接」的注册表，负责广播消息
 */
@Component
public class ChatSessionRegistry {

    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public ChatSessionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void add(Long sessionId, WebSocketSession session) {
        sessions.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void remove(Long sessionId, WebSocketSession session) {
        Set<WebSocketSession> set = sessions.get(sessionId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                sessions.remove(sessionId);
            }
        }
    }

    public int onlineCount(Long sessionId) {
        Set<WebSocketSession> set = sessions.get(sessionId);
        return set == null ? 0 : set.size();
    }

    public void broadcast(Long sessionId, Object message) {
        Set<WebSocketSession> set = sessions.get(sessionId);
        if (set == null || set.isEmpty()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            return;
        }
        TextMessage text = new TextMessage(json);
        for (WebSocketSession ws : set) {
            if (ws.isOpen()) {
                // WebSocketSession 非线程安全，串行化发送
                synchronized (ws) {
                    try {
                        ws.sendMessage(text);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }
}
