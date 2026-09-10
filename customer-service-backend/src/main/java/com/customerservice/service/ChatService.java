package com.customerservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.customerservice.common.BusinessException;
import com.customerservice.dto.ChatMessageDTO;
import com.customerservice.dto.LoginUser;
import com.customerservice.entity.ChatMessage;
import com.customerservice.entity.ChatSession;
import com.customerservice.entity.SysUser;
import com.customerservice.mapper.ChatMessageMapper;
import com.customerservice.mapper.ChatSessionMapper;
import com.customerservice.mapper.SysUserMapper;
import com.customerservice.util.UserContext;
import com.customerservice.websocket.ChatSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final SysUserMapper userMapper;
    private final ChatSessionRegistry registry;

    public ChatSession createSession(String visitorName) {
        ChatSession session = new ChatSession();
        session.setSessionNo(generateNo());
        session.setVisitorId("V" + System.currentTimeMillis());
        session.setVisitorName(StringUtils.hasText(visitorName) ? visitorName : "访客");
        session.setStatus(ChatSession.STATUS_ACTIVE);
        sessionMapper.insert(session);

        pushSystem(session.getId(), "您好，欢迎咨询！我是智能客服小智，您可以向我提问；如需人工服务请点击下方“转人工”。");
        return session;
    }

    private String generateNo() {
        return "CS" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 999);
    }

    public ChatSession getById(Long id) {
        ChatSession session = sessionMapper.selectById(id);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        return session;
    }

    public ChatMessage saveCustomerMessage(Long sessionId, String content) {
        ChatSession session = getById(sessionId);
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSenderType(ChatMessage.SENDER_CUSTOMER);
        message.setSenderName(session.getVisitorName());
        message.setContent(content);
        messageMapper.insert(message);
        return message;
    }

    public ChatMessage saveAgentMessage(Long sessionId, Long agentId, String content) {
        getById(sessionId);
        SysUser agent = userMapper.selectById(agentId);
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSenderType(ChatMessage.SENDER_AGENT);
        message.setSenderId(agentId);
        message.setSenderName(agent != null ? agent.getNickname() : "客服");
        message.setContent(content);
        messageMapper.insert(message);
        return message;
    }

    /** 访客请求转人工 */
    public void requestHuman(Long sessionId) {
        ChatSession session = getById(sessionId);
        if (session.getAgentId() != null) {
            pushSystem(sessionId, "人工客服已在线，请直接发送您的问题。");
            return;
        }
        session.setStatus(ChatSession.STATUS_WAITING);
        sessionMapper.updateById(session);
        pushSystem(sessionId, "已为您转接人工客服，请稍候，客服将尽快接入…");
    }

    /** 客服接入会话 */
    public void take(Long sessionId, Long agentId) {
        ChatSession session = getById(sessionId);
        if (ChatSession.STATUS_CLOSED.equals(session.getStatus())) {
            throw new BusinessException("会话已结束，无法接入");
        }
        SysUser agent = userMapper.selectById(agentId);
        session.setAgentId(agentId);
        session.setStatus(ChatSession.STATUS_ACTIVE);
        sessionMapper.updateById(session);
        pushSystem(sessionId, "客服 " + (agent != null ? agent.getNickname() : "") + " 已接入，为您服务。");
    }

    /** 客服关闭会话 */
    public void close(Long sessionId) {
        ChatSession session = getById(sessionId);
        session.setStatus(ChatSession.STATUS_CLOSED);
        session.setClosedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
        pushSystem(sessionId, "会话已结束，感谢您的咨询，期待再次为您服务。");
    }

    public void rate(Long sessionId, Integer rating) {
        ChatSession session = getById(sessionId);
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException("评分不合法");
        }
        session.setRating(rating);
        sessionMapper.updateById(session);
    }

    public List<ChatMessage> listMessages(Long sessionId) {
        return messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getId));
    }

    public IPage<ChatSession> listSessions(long page, long size, String status, String keyword) {
        LoginUser user = UserContext.get();
        Long agentId = null;
        String statusFilter = status;

        if (user != null && "AGENT".equals(user.getRole())) {
            if (ChatSession.STATUS_WAITING.equals(status)) {
                // 排队队列对所有客服可见
                statusFilter = status;
                agentId = null;
            } else {
                // 否则只看自己负责的会话
                agentId = user.getUserId();
                statusFilter = status;
            }
        }
        return sessionMapper.selectPageWithDetail(new Page<>(page, size), statusFilter, agentId, keyword);
    }

    private ChatMessage pushSystem(Long sessionId, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSenderType(ChatMessage.SENDER_SYSTEM);
        message.setSenderName("系统");
        message.setContent(content);
        messageMapper.insert(message);
        registry.broadcast(sessionId, ChatMessageDTO.system(message));
        return message;
    }
}
