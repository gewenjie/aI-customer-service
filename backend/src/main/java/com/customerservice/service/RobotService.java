package com.customerservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.customerservice.entity.ChatMessage;
import com.customerservice.entity.Knowledge;
import com.customerservice.mapper.ChatMessageMapper;
import com.customerservice.mapper.KnowledgeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 知识库匹配的智能问答机器人
 */
@Service
@RequiredArgsConstructor
public class RobotService {

    private final KnowledgeMapper knowledgeMapper;
    private final ChatMessageMapper chatMessageMapper;

    @Value("${app.robot.match-threshold:45}")
    private int threshold;

    @Value("${app.robot.fallback-reply}")
    private String fallbackReply;

    /**
     * 根据访客问题生成机器人回复并落库
     */
    public ChatMessage reply(Long sessionId, String question) {
        String answer = match(question);
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSenderType(ChatMessage.SENDER_ROBOT);
        message.setSenderName("智能客服");
        message.setContent(answer);
        chatMessageMapper.insert(message);
        return message;
    }

    private String match(String question) {
        List<Knowledge> list = knowledgeMapper.selectList(
                new LambdaQueryWrapper<Knowledge>().eq(Knowledge::getStatus, 1));
        if (list.isEmpty()) {
            return fallbackReply;
        }

        String q = normalize(question);
        Knowledge best = null;
        int bestScore = 0;

        for (Knowledge k : list) {
            int score = score(k, q);
            if (score > bestScore) {
                bestScore = score;
                best = k;
            }
        }

        if (best != null && bestScore >= threshold) {
            knowledgeMapper.update(null, new LambdaUpdateWrapper<Knowledge>()
                    .eq(Knowledge::getId, best.getId())
                    .setSql("hits = hits + 1"));
            return best.getAnswer();
        }
        return fallbackReply;
    }

    private int score(Knowledge k, String q) {
        int score = 0;
        String kq = normalize(k.getQuestion());

        if (kq.equals(q)) {
            return 100;
        }
        if (!kq.isEmpty() && (kq.contains(q) || q.contains(kq))) {
            score += 60;
        }
        if (k.getKeywords() != null && !k.getKeywords().isBlank()) {
            for (String kw : k.getKeywords().split("[,，;；]")) {
                String key = normalize(kw);
                if (!key.isEmpty() && q.contains(key)) {
                    score += 30;
                }
            }
        }
        // 基于字符 bigram 的相似度小加分
        score += Math.min(bigramOverlap(kq, q) * 4, 20);
        return Math.min(score, 99);
    }

    private int bigramOverlap(String a, String b) {
        Set<String> sa = bigrams(a);
        Set<String> sb = bigrams(b);
        sa.retainAll(sb);
        return sa.size();
    }

    private Set<String> bigrams(String s) {
        Set<String> set = new HashSet<>();
        if (s.length() == 1) {
            set.add(s);
            return set;
        }
        for (int i = 0; i + 1 < s.length(); i++) {
            set.add(s.substring(i, i + 2));
        }
        return set;
    }

    private String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase()
                .replaceAll("\\s+", "")
                .replaceAll("[?？。.!！,，;；:：\"'“”‘’()（）]", "");
    }
}
