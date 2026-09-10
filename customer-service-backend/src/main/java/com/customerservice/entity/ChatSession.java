package com.customerservice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_session")
public class ChatSession {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_CLOSED = "CLOSED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionNo;

    private String visitorId;

    private String visitorName;

    private Long agentId;

    private String status;

    private Integer rating;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private LocalDateTime closedAt;

    /** 展示用字段 */
    @TableField(exist = false)
    private String agentName;

    @TableField(exist = false)
    private String lastMessage;

    @TableField(exist = false)
    private LocalDateTime lastMessageTime;

    @TableField(exist = false)
    private Long messageCount;
}
