package com.customerservice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {

    public static final String SENDER_CUSTOMER = "CUSTOMER";
    public static final String SENDER_AGENT = "AGENT";
    public static final String SENDER_ROBOT = "ROBOT";
    public static final String SENDER_SYSTEM = "SYSTEM";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private String senderType;

    private Long senderId;

    private String senderName;

    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
