package com.customerservice.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.customerservice.common.Result;
import com.customerservice.dto.RateRequest;
import com.customerservice.entity.ChatMessage;
import com.customerservice.entity.ChatSession;
import com.customerservice.service.ChatService;
import com.customerservice.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final ChatService chatService;

    @GetMapping
    public Result<IPage<ChatSession>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(chatService.listSessions(page, size, status, keyword));
    }

    @GetMapping("/{id}/messages")
    public Result<List<ChatMessage>> messages(@PathVariable Long id) {
        return Result.ok(chatService.listMessages(id));
    }

    @PostMapping("/{id}/take")
    public Result<Void> take(@PathVariable Long id) {
        chatService.take(id, UserContext.userId());
        return Result.ok();
    }

    @PostMapping("/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        chatService.close(id);
        return Result.ok();
    }

    @PostMapping("/{id}/rate")
    public Result<Void> rate(@PathVariable Long id, @Valid @RequestBody RateRequest request) {
        chatService.rate(id, request.getRating());
        return Result.ok();
    }
}
