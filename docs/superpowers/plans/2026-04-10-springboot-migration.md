# Spring Boot 迁移 + Redis 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有 Servlet/JSP 邮件系统迁移为 Spring Boot + MyBatis + Vue 3 前后端分离架构，并引入 Redis 实现分布式 Session、缓存和限流。

**Architecture:** 后端 Spring Boot 3.x 提供 REST API，MyBatis 替换手写 JDBC，HikariCP 连接池；前端 Vue 3 + Element Plus；Redis 负责 JWT 黑名单、收件箱缓存、登录限流、未读数计数、远程邮件异步队列。

**Tech Stack:** Java 17, Spring Boot 3.x, MyBatis, HikariCP, jjwt, Redis (Spring Data Redis), Vue 3, Vite, TypeScript, Element Plus, Pinia, Axios, MySQL 8.0, Nginx, Docker Compose

---

## 阶段一：Spring Boot 骨架 + MyBatis

### Task 1: 初始化 Spring Boot 项目

**Files:**
- Create: `mail-backend/pom.xml`
- Create: `mail-backend/src/main/java/com/mail/MailApplication.java`
- Create: `mail-backend/src/main/resources/application.yml`

- [ ] 创建 Maven 项目，pom.xml 引入依赖：spring-boot-starter-web, spring-boot-starter-security, mybatis-spring-boot-starter:3.0.3, mysql-connector-j, jjwt-api:0.12.x + jjwt-impl + jjwt-jackson, spring-boot-starter-data-redis, spring-boot-starter-validation, lombok

- [ ] 写 application.yml：
```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:3306/${DB_NAME:mail_system}?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: ${DB_USER:root}
    password: ${DB_PASS:}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASS:}
  servlet:
    multipart:
      max-file-size: 10MB
    max-request-size: 20MB
mybatis:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
jwt:
  secret: ${JWT_SECRET:change-me-in-production}
  expire-ms: 86400000
app:
  upload-dir: ${UPLOAD_DIR:/data/uploads}
  mail-domain: ${APP_MAIL_DOMAIN:lneqse.com}
  remote-api-secret: ${REMOTE_API_SECRET:CHANGE_ME}
```

- [ ] 验证项目能启动：`mvn spring-boot:run`
- [ ] Commit: `feat: init spring boot project skeleton`

---

### Task 2: Entity + Mapper + XML

**Files:**
- Create: `entity/User.java`, `Mail.java`, `Attachment.java`, `Contact.java`, `MailFlag.java`
- Create: `mapper/UserMapper.java` 等5个 Mapper 接口
- Create: `resources/mapper/*.xml` 5个 XML

- [ ] 写5个 Entity（使用 Lombok @Data，LocalDateTime 替换 Date）

- [ ] 写 MailMapper.xml 收件箱核心 SQL：
```xml
<select id="findInbox" resultType="com.mail.dto.response.MailListItem">
  SELECT m.id, m.subject, m.sent_at, m.has_attachments,
      m.is_remote, m.remote_sender_email,
      s.email AS sender_email, s.username AS sender_username,
         COALESCE(f.is_read,0) AS is_read,
         COALESCE(f.is_starred,0) AS is_starred,
         COALESCE(f.is_important,0) AS is_important
  FROM mails m
  LEFT JOIN users s ON m.sender_id = s.id
  LEFT JOIN mail_flags f ON f.mail_id = m.id AND f.user_id = #{userId}
  WHERE m.receiver_id = #{userId}
    AND m.receiver_deleted = 0 AND m.status = 1
  ORDER BY m.sent_at DESC
  LIMIT #{size} OFFSET #{offset}
</select>
```

- [ ] 写 MailFlagMapper.xml UPSERT：
```xml
<insert id="upsert">
  INSERT INTO mail_flags(mail_id, user_id, is_read, is_starred, is_important)
  VALUES(#{mailId}, #{userId}, #{isRead,jdbcType=BOOLEAN},
         #{isStarred,jdbcType=BOOLEAN}, #{isImportant,jdbcType=BOOLEAN})
  ON DUPLICATE KEY UPDATE
    is_read=VALUES(is_read), is_starred=VALUES(is_starred),
    is_important=VALUES(is_important), updated_at=NOW()
</insert>
```

- [ ] Commit: `feat: add entities, mappers and xml`

---

### Task 3: 通用响应 + 工具类

**Files:**
- Create: `dto/response/ApiResponse.java`
- Create: `dto/response/PageResult.java`
- Create: `util/PasswordUtil.java`
- Create: `util/FileUtil.java`
- Create: `util/SignatureUtil.java`

- [ ] 写 ApiResponse<T>（ok/fail 静态工厂方法）
- [ ] 迁移 PasswordUtil（SHA-256 加盐，10000次迭代）
- [ ] 迁移 FileUtil（存盘路径生成、Base64 解码，改用 app.upload-dir 配置）
- [ ] 写 SignatureUtil（从旧 ReceiveMailApiServlet.generateSignature 提取）
- [ ] Commit: `feat: add common response and utils`

---

### Task 4: JWT + Spring Security

**Files:**
- Create: `config/JwtConfig.java`
- Create: `security/JwtUtil.java`
- Create: `security/JwtAuthFilter.java`
- Create: `security/UserPrincipal.java`
- Create: `config/SecurityConfig.java`
- Create: `config/WebMvcConfig.java`

- [ ] JwtUtil：generateToken(userId), parseUserId(token), isValid(token)
- [ ] JwtAuthFilter：从 Authorization: Bearer 提取 token，写入 SecurityContextHolder
- [ ] SecurityConfig 放行：/api/auth/**, /api/remote/receive
- [ ] WebMvcConfig 配置 CORS（允许前端 localhost:5173 和生产域名）
- [ ] Commit: `feat: add JWT auth and security config`

---

### Task 5: 认证模块

**Files:**
- Create: `service/AuthService.java`
- Create: `controller/AuthController.java`
- Create: `dto/request/LoginRequest.java`
- Create: `dto/request/RegisterRequest.java`
- Create: `dto/response/AuthResponse.java`

- [ ] AuthService.register：校验唯一性，生成 salt，SHA-256 hash，insert
- [ ] AuthService.login：查用户，验证 hash，返回 JWT token
- [ ] 接口：POST /api/auth/register, POST /api/auth/login, GET /api/auth/me
- [ ] 用 curl 测试注册和登录
- [ ] Commit: `feat: auth register and login`

---

### Task 6: 邮件核心模块

**Files:**
- Create: `service/MailService.java`
- Create: `controller/MailController.java`
- Create: `dto/response/MailListItem.java`
- Create: `dto/response/MailDetail.java`

- [ ] MailService：getInbox, getSent, getDrafts, getMailDetail（自动标已读）, sendMail, saveDraft, deleteMail（软删除）
- [ ] MailController：所有接口注入 @AuthenticationPrincipal UserPrincipal
- [ ] 测试收件箱、发送邮件接口
- [ ] Commit: `feat: mail core CRUD`

---

### Task 7: 附件、标记、联系人、搜索

**Files:**
- Create: `service/AttachmentService.java`, `controller/AttachmentController.java`
- Create: `service/MailFlagService.java`, `controller/MailFlagController.java`
- Create: `service/ContactService.java`, `controller/ContactController.java`
- Create: `service/SearchService.java`, `controller/SearchController.java`

- [ ] GET /api/attachments/{id}/download（流式响应，校验归属权）
- [ ] PUT /api/mails/{id}/flags
- [ ] 联系人 CRUD + GET /api/contacts/search?keyword=
- [ ] GET /api/search?keyword=&box=&searchIn=&page=&size=（动态 SQL）
- [ ] Commit: `feat: attachment, flag, contact, search`

---

### Task 8: 远程邮件接收

**Files:**
- Create: `service/RemoteMailService.java`
- Create: `controller/RemoteMailController.java`
- Create: `dto/request/RemoteReceiveRequest.java`

- [ ] 迁移 ReceiveMailApiServlet 逻辑（验签、存邮件、存附件 Base64 解码）
- [ ] POST /api/remote/receive
- [ ] 用 curl 模拟测试
- [ ] Commit: `feat: remote mail receive API`

---

## 阶段二：Redis 集成
### Task 9: Redis 基础配置

- [ ] 配置 RedisTemplate<String, String>，StringRedisSerializer
- [ ] Commit: `feat: redis config`

---

### Task 10: JWT 黑名单（登出）

- [ ] AuthService.logout：token 存入 Redis，key=blacklist:{token}，TTL=剩余有效期
- [ ] JwtAuthFilter 验证时检查黑名单
- [ ] POST /api/auth/logout
- [ ] Commit: `feat: jwt blacklist logout via redis`

---

### Task 11: 登录限流

- [ ] RateLimiter：Redis INCR + EXPIRE，key=rate:login:{ip}，1分钟内超5次返回429
- [ ] 在 AuthService.login 中调用
- [ ] Commit: `feat: login rate limiting via redis`

---

### Task 12: 收件箱列表缓存

- [ ] getInbox 先查 Redis（key=inbox:{userId}:p{page}，TTL=60s），miss 则查 DB 并写缓存
- [ ] sendMail / deleteMail 时删除对应用户的 inbox 缓存
- [ ] Commit: `feat: inbox list cache via redis`

---

### Task 13: 未读数缓存

- [ ] key=unread:{userId}，收到新邮件 INCR，标记已读 DECR
- [ ] GET /api/mails/unread-count
- [ ] Commit: `feat: unread count cache via redis`
---

### Task 14: 远程邮件异步发送队列
- [ ] sendMail 检测到远程地址时 LPUSH 到 remote_mail_queue
- [ ] @Scheduled BRPOP 消费，HTTP POST 到目标服务器
- [ ] 失败重试（key=remote:retry:{mailId}），超3次标记失败
- [ ] Commit: `feat: async remote mail queue via redis`

---

## 阶段三：Vue 3 前端

### Task 15: 初始化前端项目

- [ ] npm create vue@latest mail-frontend（TypeScript + Vue Router + Pinia）
- [ ] 安装 element-plus axios
- [ ] vite.config.ts 代理 /api -> http://localhost:8080
- [ ] Commit: `feat: init vue3 frontend`

---

### Task 16: Axios 封装 + 路由 + Auth Store

- [ ] http.ts：请求拦截器加 Authorization header，响应拦截器处理 401 跳转登录
- [ ] auth store：token + userInfo，login/logout/register action
- [ ] router：路由守卫，未登录跳 /login
- [ ] Commit: `feat: axios, router, auth store`

---

### Task 17: 登录/注册页

- [ ] Login.vue + Register.vue（Element Plus 表单）
- [ ] Commit: `feat: login and register pages`

---

### Task 18: 邮件列表页

- [ ] Inbox.vue / Sent.vue / Drafts.vue（Element Plus Table，分页，标记按钮）
- [ ] MailSidebar.vue 组件（含未读数角标）
- [ ] Commit: `feat: mail list pages`

---

### Task 19: 写信/查看邮件页

- [ ] Compose.vue（收件人自动补全、附件上传、草稿保存、发送）
- [ ] ViewMail.vue（邮件详情、附件下载、回复/转发）
- [ ] Commit: `feat: compose and view mail pages`

---

### Task 20: 联系人 + 搜索页

- [ ] Contacts.vue（列表、添加、编辑、删除）
- [ ] Search.vue（关键词搜索、范围过滤）
- [ ] Commit: `feat: contacts and search pages`

---

## 阶段四：Docker Compose 整合

### Task 21: 更新 Docker Compose

- [ ] docker-compose.yml 新增 redis:7-alpine 服务
- [ ] 后端 Dockerfile：多阶段构建（maven build + jre 运行）
- [ ] 前端 Dockerfile：node build + nginx 静态服务
- [ ] Nginx：/api/ 代理后端，/ 服务前端静态文件
- [ ] docker compose up 全量验证
- [ ] Commit: `feat: docker compose with redis and frontend`

---

## API 速查表

| 模块 | 方法 | 路径 |
|---|---|---|
| 注册 | POST | /api/auth/register |
| 登录 | POST | /api/auth/login |
| 登出 | POST | /api/auth/logout |
| 当前用户 | GET | /api/auth/me |
| 收件箱 | GET | /api/mails/inbox |
| 发件箱 | GET | /api/mails/sent |
| 草稿箱 | GET | /api/mails/drafts |
| 邮件详情 | GET | /api/mails/{id} |
| 发送邮件 | POST | /api/mails/send |
| 保存草稿 | POST | /api/mails/draft |
| 删除邮件 | DELETE | /api/mails/{id} |
| 未读数 | GET | /api/mails/unread-count |
| 邮件标记 | PUT | /api/mails/{id}/flags |
| 下载附件 | GET | /api/attachments/{id}/download |
| 联系人列表 | GET | /api/contacts |
| 添加联系人 | POST | /api/contacts |
| 修改联系人 | PUT | /api/contacts/{id} |
| 删除联系人 | DELETE | /api/contacts/{id} |
| 联系人搜索 | GET | /api/contacts/search |
| 邮件搜索 | GET | /api/search |
| 远程接收 | POST | /api/remote/receive |
