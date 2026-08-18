package com.customerservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.customerservice.entity.ChatMessage;
import com.customerservice.entity.ChatSession;
import com.customerservice.entity.Knowledge;
import com.customerservice.entity.SysUser;
import com.customerservice.mapper.ChatMessageMapper;
import com.customerservice.mapper.ChatSessionMapper;
import com.customerservice.mapper.KnowledgeMapper;
import com.customerservice.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final KnowledgeMapper knowledgeMapper;
    private final SysUserMapper userMapper;

    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("totalSessions", sessionMapper.selectCount(null));
        data.put("waitingSessions", sessionMapper.selectCount(
                new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getStatus, ChatSession.STATUS_WAITING)));
        data.put("activeSessions", sessionMapper.selectCount(
                new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getStatus, ChatSession.STATUS_ACTIVE)));
        data.put("closedSessions", sessionMapper.selectCount(
                new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getStatus, ChatSession.STATUS_CLOSED)));

        LocalDate today = LocalDate.now();
        data.put("todaySessions", sessionMapper.selectCount(
                new LambdaQueryWrapper<ChatSession>().ge(ChatSession::getCreatedAt, today.atStartOfDay())));

        data.put("totalMessages", messageMapper.selectCount(null));
        data.put("robotMessages", messageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getSenderType, ChatMessage.SENDER_ROBOT)));

        data.put("knowledgeCount", knowledgeMapper.selectCount(null));
        data.put("agentCount", userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "AGENT")));

        List<Knowledge> knowledgeList = knowledgeMapper.selectList(null);
        long hits = knowledgeList.stream().mapToLong(k -> k.getHits() == null ? 0 : k.getHits()).sum();
        data.put("knowledgeHits", hits);
        return data;
    }

    public Map<String, Object> trend() {
        LocalDate start = LocalDate.now().minusDays(6);
        List<Map<String, Object>> rows = sessionMapper.countByDay(start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        Map<String, Long> counts = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object day = row.get("day");
            Object cnt = row.get("cnt");
            if (day != null) {
                counts.put(day.toString(), cnt == null ? 0L : ((Number) cnt).longValue());
            }
        }

        List<String> days = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < 7; i++) {
            LocalDate d = start.plusDays(i);
            String key = d.format(fmt);
            days.add(key);
            values.add(counts.getOrDefault(key, 0L));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("days", days);
        data.put("values", values);
        return data;
    }
}
