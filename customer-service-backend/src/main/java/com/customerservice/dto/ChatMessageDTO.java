package com.customerservice.dto;

import com.customerservice.entity.ChatMessage;
import lombok.Data;

import java.time.format.DateTimeFormatter;

/**
 * WebSocket 消息载荷（收/发通用）
 */
@Data
public class ChatMessageDTO {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 消息类型：CHAT / TRANSFER / SYSTEM */
    private String type;

    private Long sessionId;

    private String senderType;

    private Long senderId;

    private String senderName;

    private String content;

    private Long messageId;

    private String timestamp;

    public static ChatMessageDTO from(ChatMessage m) {
        ChatMessageDTO d = new ChatMessageDTO();
        d.setType("CHAT");
        d.setMessageId(m.getId());
        d.setSessionId(m.getSessionId());
        d.setSenderType(m.getSenderType());
        d.setSenderId(m.getSenderId());
        d.setSenderName(m.getSenderName());
        d.setContent(m.getContent());
        d.setTimestamp(m.getCreatedAt() == null ? null : m.getCreatedAt().format(FMT));
        return d;
    }

    public static ChatMessageDTO system(ChatMessage m) {
        ChatMessageDTO d = from(m);
        d.setType("SYSTEM");
        return d;
    }
}
