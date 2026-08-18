# 智能客服系统 (Intelligent Customer Service System)

基于 **Vue 3 + Element Plus** 前端和 **Spring Boot 3 + MyBatis Plus** 后端实现的智能客服系统。

## 功能特性

- 🤖 **智能机器人问答**：基于知识库的关键词/相似度匹配自动回复
- 👨‍💼 **人工客服转接**：机器人无法解答时可一键转人工，客服实时接入
- 💬 **WebSocket 实时会话**：访客 ↔ 客服实时消息推送
- 📚 **知识库管理**：分类、问题、答案、关键词维护，命中次数统计
- 👥 **客服人员管理**：账号、角色、启用/禁用
- 📊 **数据统计**：会话量、消息量、知识命中、近 7 日趋势
- ⭐ **满意度评价**：会话结束后访客可打分

## 技术栈

| 层 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite、Element Plus、Pinia、Vue Router、Axios |
| 后端 | Spring Boot 3.2、MyBatis Plus 3.5、MySQL 8、WebSocket、JWT |
| 认证 | JWT（jjwt）+ BCrypt 密码加密 |

## 目录结构

```
ai-project/
├── backend/          # Spring Boot 后端
│   ├── sql/init.sql  # 建库建表脚本
│   └── src/main/java/com/customerservice/
└── frontend/         # Vue 3 前端
    └── src/
```

## 环境要求

- JDK 17+
- Maven 3.6+
- Node.js 18+ / pnpm
- MySQL 8.0+

## 快速开始

### 1. 初始化数据库

```bash
mysql -uroot -p < backend/sql/init.sql
```

> 首次启动后端时会自动写入默认账号和示例知识库数据（见下方默认账号）。

### 2. 启动后端

修改 `backend/src/main/resources/application.yml` 中的数据库账号密码，然后：

```bash
cd backend
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`。

### 3. 启动前端

```bash
cd frontend
pnpm install
pnpm run dev
```

前端默认运行在 `http://localhost:5173`。

## 默认账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `admin123` |
| 客服 | `agent01` | `123456` |

## 页面说明

- 访客端（客服聊天窗口）：`http://localhost:5173/#/chat`
- 管理端登录：`http://localhost:5173/#/login`
- 管理端工作台：登录后自动跳转

## 主要接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 登录 |
| POST | `/api/chat/session` | 访客创建会话 |
| GET | `/api/knowledge` | 知识库分页列表 |
| POST/PUT/DELETE | `/api/knowledge` | 知识库增删改 |
| GET | `/api/sessions` | 会话列表（含排队） |
| POST | `/api/sessions/{id}/take` | 客服接入会话 |
| POST | `/api/sessions/{id}/close` | 关闭会话 |
| POST | `/api/sessions/{id}/rate` | 访客评价 |
| GET | `/api/stats/overview` | 统计概览 |
| WS | `/ws/chat` | 实时聊天（参数 sessionId、role） |
