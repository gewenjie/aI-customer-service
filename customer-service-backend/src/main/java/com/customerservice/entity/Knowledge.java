package com.customerservice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge")
public class Knowledge {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String question;

    private String answer;

    private String keywords;

    private Integer status;

    private Integer hits;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 展示用：分类名称 */
    @TableField(exist = false)
    private String categoryName;
}
