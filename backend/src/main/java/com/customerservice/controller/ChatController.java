package com.customerservice.controller;

import com.customerservice.common.Result;
import com.customerservice.entity.ChatMessage;
import com.customerservice.entity.ChatSession;
import com.customerservice.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 访客端公开接口（无需登录）
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/session")
    public Result<ChatSession> createSession(@RequestBody(required = false) Map<String, String> body) {
        String visitorName = body == null ? null : body.get("visitorName");
        return Result.ok(chatService.createSession(visitorName));
    }

    @GetMapping("/sessions/{id}/messages")
    public Result<List<ChatMessage>> messages(@PathVariable Long id) {
        return Result.ok(chatService.listMessages(id));
    }
}
