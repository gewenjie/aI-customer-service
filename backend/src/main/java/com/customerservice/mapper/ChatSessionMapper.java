package com.customerservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.customerservice.entity.ChatSession;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    /**
     * 分页查询会话，附带客服昵称、最近一条消息、消息数
     */
    @Select("<script>" +
            "SELECT s.id, s.session_no, s.visitor_id, s.visitor_name, s.agent_id, s.status, s.rating, " +
            "       s.created_at, s.updated_at, s.closed_at, " +
            "       u.nickname AS agent_name, " +
            "       lm.content AS last_message, lm.created_at AS last_message_time, " +
            "       (SELECT COUNT(*) FROM chat_message m WHERE m.session_id = s.id) AS message_count " +
            "FROM chat_session s " +
            "LEFT JOIN sys_user u ON u.id = s.agent_id " +
            "LEFT JOIN chat_message lm ON lm.id = (SELECT MAX(m2.id) FROM chat_message m2 WHERE m2.session_id = s.id) " +
            "<where>" +
            "  <if test='status != null and status != \"\"'> AND s.status = #{status}</if>" +
            "  <if test='agentId != null'> AND s.agent_id = #{agentId}</if>" +
            "  <if test='keyword != null and keyword != \"\"'> AND (s.session_no LIKE CONCAT('%', #{keyword}, '%') OR s.visitor_name LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            "</where>" +
            "ORDER BY s.updated_at DESC" +
            "</script>")
    IPage<ChatSession> selectPageWithDetail(Page<ChatSession> page,
                                            @Param("status") String status,
                                            @Param("agentId") Long agentId,
                                            @Param("keyword") String keyword);

    /**
     * 按天统计会话量（用于趋势图）
     */
    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS day, COUNT(*) AS cnt " +
            "FROM chat_session WHERE created_at >= #{start} " +
            "GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')")
    List<Map<String, Object>> countByDay(@Param("start") String start);
}
