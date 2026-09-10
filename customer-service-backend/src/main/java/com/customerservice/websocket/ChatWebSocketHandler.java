package com.customerservice.websocket;

import com.customerservice.dto.ChatMessageDTO;
import com.customerservice.entity.ChatMessage;
import com.customerservice.entity.ChatSession;
import com.customerservice.service.ChatService;
import com.customerservice.service.RobotService;
import com.customerservice.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ChatSessionRegistry registry;
    private final ChatService chatService;
    private final RobotService robotService;
    private final JwtUtil jwtUtil;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private long replyDelayMs = 600;

    public ChatWebSocketHandler(ObjectMapper objectMapper,
                                ChatSessionRegistry registry,
                                ChatService chatService,
                                RobotService robotService,
                                JwtUtil jwtUtil) {
        this.objectMapper = objectMapper;
        this.registry = registry;
        this.chatService = chatService;
        this.robotService = robotService;
        this.jwtUtil = jwtUtil;
    }

    @Value("${app.robot.reply-delay-ms:600}")
    public void setReplyDelayMs(long replyDelayMs) {
        this.replyDelayMs = replyDelayMs;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, String> params = queryParams(session);
        String sessionIdStr = params.get("sessionId");
        String role = params.getOrDefault("role", "CUSTOMER");

        if (!StringUtils.hasText(sessionIdStr)) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        Long sessionId = Long.valueOf(sessionIdStr);
        session.getAttributes().put("sessionId", sessionId);
        session.getAttributes().put("role", role);

        if ("AGENT".equals(role)) {
            String token = params.get("token");
            try {
                Claims claims = jwtUtil.parse(token);
                session.getAttributes().put("userId", Long.valueOf(claims.getSubject()));
            } catch (Exception e) {
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
        }
        registry.add(sessionId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long sessionId = (Long) session.getAttributes().get("sessionId");
        String role = (String) session.getAttributes().get("role");

        JsonNode node = objectMapper.readTree(message.getPayload());
        String type = node.path("type").asText("CHAT");
        String content = node.path("content").asText("");

        if ("TRANSFER".equals(type)) {
            if ("CUSTOMER".equals(role)) {
                chatService.requestHuman(sessionId);
            }
            return;
        }

        if (!StringUtils.hasText(content)) {
            return;
        }

        ChatMessage saved;
        if ("AGENT".equals(role)) {
            Long agentId = (Long) session.getAttributes().get("userId");
            saved = chatService.saveAgentMessage(sessionId, agentId, content);
        } else {
            saved = chatService.saveCustomerMessage(sessionId, content);
        }
        registry.broadcast(sessionId, ChatMessageDTO.from(saved));

        // 访客消息且尚未接入人工 -> 机器人自动回复
        if (!"AGENT".equals(role)) {
            ChatSession current = chatService.getById(sessionId);
            if (current.getAgentId() == null) {
                final Long targetSessionId = sessionId;
                final String question = saved.getContent();
                executor.execute(() -> {
                    try {
                        Thread.sleep(replyDelayMs);
                        ChatMessage robot = robotService.reply(targetSessionId, question);
                        registry.broadcast(targetSessionId, ChatMessageDTO.from(robot));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        detach(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket 连接异常", exception);
        detach(session);
    }

    private void detach(WebSocketSession session) {
        Long sessionId = (Long) session.getAttributes().get("sessionId");
        if (sessionId != null) {
            registry.remove(sessionId, session);
        }
    }

    private Map<String, String> queryParams(WebSocketSession session) {
        URI uri = session.getUri();
        Map<String, String> map = new HashMap<>();
        if (uri == null || uri.getQuery() == null) {
            return map;
        }
        for (String pair : uri.getQuery().split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                map.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
            }
        }
        return map;
    }
}
