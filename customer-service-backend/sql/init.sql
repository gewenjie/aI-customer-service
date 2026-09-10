-- =============================================================
-- 智能客服系统 数据库初始化脚本
-- MySQL 8.0+
-- 执行方式: mysql -uroot -p < init.sql
-- =============================================================

CREATE DATABASE IF NOT EXISTS `customer_service`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `customer_service`;

-- -------------------------------------------------------------
-- 客服/管理员账号表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `username`   VARCHAR(64)  NOT NULL COMMENT '登录名',
  `password`   VARCHAR(128) NOT NULL COMMENT 'BCrypt 密码',
  `nickname`   VARCHAR(64)  DEFAULT NULL COMMENT '昵称/姓名',
  `avatar`     VARCHAR(255) DEFAULT NULL COMMENT '头像',
  `role`       VARCHAR(20)  NOT NULL DEFAULT 'AGENT' COMMENT 'ADMIN / AGENT',
  `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客服/管理员';

-- -------------------------------------------------------------
-- 知识库分类表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `knowledge_category` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `name`       VARCHAR(64) NOT NULL COMMENT '分类名称',
  `sort`       INT         DEFAULT 0,
  `created_at` DATETIME    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库分类';

-- -------------------------------------------------------------
-- 知识库表（机器人问答依据）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `knowledge` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `category_id` BIGINT       DEFAULT NULL COMMENT '分类',
  `question`    VARCHAR(255) NOT NULL COMMENT '标准问题',
  `answer`      TEXT         NOT NULL COMMENT '答案',
  `keywords`    VARCHAR(255) DEFAULT NULL COMMENT '关键词，逗号分隔',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `hits`        INT          NOT NULL DEFAULT 0 COMMENT '命中次数',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库';

-- -------------------------------------------------------------
-- 会话表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat_session` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT,
  `session_no`   VARCHAR(32) NOT NULL COMMENT '会话编号',
  `visitor_id`   VARCHAR(64) DEFAULT NULL COMMENT '访客标识',
  `visitor_name` VARCHAR(64) DEFAULT NULL COMMENT '访客昵称',
  `agent_id`     BIGINT      DEFAULT NULL COMMENT '负责客服，NULL=机器人',
  `status`       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE机器人/WAITING排队/CLOSED已结束',
  `rating`       TINYINT     DEFAULT NULL COMMENT '满意度 1-5',
  `created_at`   DATETIME    DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `closed_at`    DATETIME    DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_no` (`session_no`),
  KEY `idx_status` (`status`),
  KEY `idx_agent` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话';

-- -------------------------------------------------------------
-- 消息表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat_message` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `session_id`  BIGINT      NOT NULL COMMENT '会话ID',
  `sender_type` VARCHAR(20) NOT NULL COMMENT 'CUSTOMER/AGENT/ROBOT/SYSTEM',
  `sender_id`   BIGINT      DEFAULT NULL COMMENT '发送人ID（客服）',
  `sender_name` VARCHAR(64) DEFAULT NULL COMMENT '发送人名称',
  `content`     TEXT        NOT NULL COMMENT '消息内容',
  `created_at`  DATETIME    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息';
