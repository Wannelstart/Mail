# 📧 Mail System — 全栈邮件系统

<div align="center">

**基于 Spring Boot 3 + Vue 3 的企业级邮件系统**

支持站内邮件 · QQ 邮箱绑定 · 外部邮件收发 · AI 智能润色 · Docker 一键部署

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen?logo=spring)](https://spring.io/)
[![Vue](https://img.shields.io/badge/Vue-3-green?logo=vue.js)](https://vuejs.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue?logo=docker)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

</div>

---

## 📋 目录

- [功能特性](#-功能特性)
- [系统架构](#-系统架构)
- [技术栈](#-技术栈)
- [项目结构](#-项目结构)
- [快速开始](#-快速开始)
- [Docker 部署](#-docker-部署)
- [配置说明](#-配置说明)
- [API 接口](#-api-接口)
- [数据库设计](#-数据库设计)
- [安全特性](#-安全特性)
- [部署脚本](#-部署脚本)
- [常见问题](#-常见问题)

---

## ✨ 功能特性

### 用户系统
- 🔐 注册 / 登录（BCrypt 密码哈希）
- 🔑 JWT 令牌认证（24h 过期）
- 👤 头像上传、用户名修改
- 🔒 密码修改

### 邮件核心
- 📬 收件箱 / 发件箱 / 草稿箱
- ⭐ 星标、重要标记、已读/未读
- 📎 附件上传 / 下载（单文件 ≤10MB，总计 ≤20MB）
- ↩️ 邮件撤回（2 分钟内）
- 🗑️ 软删除（发件人/收件人独立删除）

### QQ 邮箱集成
- 📤 通过 QQ SMTP 发送外部邮件
- 📥 定时 IMAP 拉取绑定 QQ 邮箱的新收件
- 🔐 QQ 授权码 AES-GCM 加密存储

### 联系人管理
- 👥 添加联系人、分组管理
- 🔍 联系人搜索
- 📧 联系人快捷选择

### AI 功能
- 🤖 智谱 GLM 邮件文本润色
- ✏️ 一键优化邮件内容

### 搜索
- 🔎 按主题 / 内容 / 发件人全文搜索
- 📂 跨收件箱、发件箱、草稿箱搜索

### 性能优化
- ⚡ Vue keep-alive 页面缓存，标签页秒切
- 📡 数据预加载，首次进入即有数据
- 🔄 智能刷新（数据不变不重渲染）

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────┐
│                     用户浏览器                        │
│                  https://mail.lneqse.com             │
└─────────────────────┬───────────────────────────────┘
                      │ HTTPS
┌─────────────────────▼───────────────────────────────┐
│                   Nginx (反向代理)                    │
│          静态文件服务 + SSL 终止 + 安全头              │
└──────────┬──────────────────────────┬───────────────┘
           │ /api/*                   │ 静态文件
┌──────────▼──────────┐    ┌──────────▼──────────┐
│   Spring Boot App   │    │    Vue 3 Frontend   │
│     (Port 8080)     │    │   (Nginx 静态服务)    │
│                     │    │                     │
│  • Security (JWT)   │    │  • Pinia Store      │
│  • REST API         │    │  • Vue Router       │
│  • Mail Service     │    │  • Axios            │
│  • AI Service       │    │  • keep-alive 缓存   │
│  • IMAP Receiver    │    │                     │
└──┬─────────────┬────┘    └─────────────────────┘
   │             │
┌──▼────┐   ┌───▼────┐
│ MySQL │   │ Redis  │
│  8.0  │   │  7     │
└───────┘   └────────┘
```

---

## 🛠️ 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot | 3.2.5 |
| **安全** | Spring Security + JWT (jjwt) | 6.2.4 / 0.12.5 |
| **ORM** | MyBatis | 3.0.3 |
| **数据库** | MySQL | 8.0 |
| **缓存** | Redis | 7 (Alpine) |
| **前端框架** | Vue 3 (Composition API) | 3.x |
| **状态管理** | Pinia | latest |
| **路由** | Vue Router | 4.x |
| **HTTP 客户端** | Axios | latest |
| **UI 图标** | Lucide Vue | latest |
| **构建工具** | Vite | latest |
| **Web 服务器** | Nginx | 1.27 (Alpine) |
| **容器化** | Docker + Docker Compose | latest |
| **HTTPS** | Let's Encrypt (Certbot) | latest |
| **AI** | 智谱 GLM API | glm-5 |
| **JDK** | Eclipse Temurin | 21 |

---

## 📁 项目结构

```
mail-system/
├── src/main/java/com/mail/          # Spring Boot 后端
│   ├── config/                      # 配置类
│   │   ├── GlobalExceptionHandler   #   全局异常处理
│   │   ├── JwtConfig                #   JWT 配置与校验
│   │   ├── SecurityConfig           #   Spring Security 配置
│   │   └── WebMvcConfig             #   CORS 配置
│   ├── controller/                  # REST API 控制器
│   │   ├── AttachmentController     #   附件上传/下载
│   │   ├── AuthController           #   注册/登录/密码
│   │   ├── ContactController        #   联系人 CRUD
│   │   ├── MailController           #   邮件收发/搜索
│   │   └── RemoteMailController     #   远程邮件接收
│   ├── dto/                         # 数据传输对象
│   │   ├── request/                 #   请求 DTO
│   │   └── response/                #   响应 DTO
│   ├── entity/                      # 数据库实体
│   ├── mapper/                      # MyBatis Mapper 接口
│   ├── security/                    # 安全组件
│   │   ├── JwtAuthFilter            #   JWT 认证过滤器
│   │   ├── JwtUtil                  #   JWT 工具类
│   │   └── RateLimitFilter          #   API 频率限制
│   ├── service/                     # 业务逻辑
│   │   ├── AuthService              #   认证服务
│   │   ├── MailService              #   邮件服务
│   │   ├── IncomingMailService      #   IMAP 收件拉取
│   │   ├── ExternalMailService      #   SMTP 外部发送
│   │   ├── AiService                #   AI 润色
│   │   └── ContactService           #   联系人服务
│   └── util/                        # 工具类
│       ├── AesEncryptor             #   AES-GCM 加密
│       ├── HtmlSanitizer            #   OWASP XSS 防护
│       ├── FileValidator            #   文件上传校验
│       └── SignatureUtil            #   HMAC 签名
├── src/main/resources/
│   ├── application.yml              # 应用配置
│   └── mapper/                      # MyBatis XML 映射
├── frontend/                        # Vue 3 前端
│   └── src/
│       ├── api/index.js             # Axios 封装（自动带 Token）
│       ├── stores/
│       │   ├── auth.js              # 认证状态
│       │   └── mail.js              # 邮件数据缓存
│       ├── router/index.js          # 路由配置
│       └── views/
│           ├── Layout.vue           # 主布局（keep-alive）
│           ├── Inbox.vue            # 收件箱
│           ├── Sent.vue             # 发件箱
│           ├── Drafts.vue           # 草稿箱
│           ├── Compose.vue          # 写邮件
│           ├── ViewMail.vue         # 查看邮件
│           ├── Search.vue           # 搜索
│           ├── Contacts.vue         # 联系人
│           ├── Login.vue            # 登录
│           └── Register.vue         # 注册
├── deploy/                          # 部署配置
│   ├── db/init/                     # 数据库初始化 SQL
│   │   ├── 01_schema.sql            #   建表脚本
│   │   ├── 02_migrate_if_old.sql    #   旧版迁移
│   │   └── 03_add_user_fields.sql   #   字段扩展
│   ├── nginx/conf.d/                # Nginx 配置
│   │   ├── default.conf             #   HTTP 配置
│   │   └── mail-https.conf          #   HTTPS 配置
│   └── scripts/
│       ├── deploy.sh                #   部署脚本
│       ├── issue-cert.sh            #   SSL 证书申请
│       ├── backup-db.sh             #   数据库备份
│       └── backup-uploads.sh        #   附件备份
├── .env.example                     # 环境变量模板
├── .gitignore                       # Git 忽略规则
├── docker-compose.yml               # Docker 编排
├── Dockerfile                       # 后端多阶段构建
├── pom.xml                          # Maven 配置
├── setup-server.sh                  # 服务器一键部署
└── README.md                        # 本文件
```

---

## 🚀 快速开始

### 环境要求

| 工具 | 最低版本 | 说明 |
|------|---------|------|
| JDK | 21 | 推荐 Eclipse Temurin |
| Maven | 3.9+ | 构建后端 |
| Node.js | 20+ | 构建前端 |
| MySQL | 8.0 | 数据存储 |
| Redis | 7+ | 缓存/限流 |

### 1. 克隆项目

```bash
git clone https://github.com/你的用户名/mail-system.git
cd mail-system
```

### 2. 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env`，填入实际配置：

```bash
# 数据库密码
DB_PASS=你的MySQL密码

# Redis 密码
REDIS_PASS=你的Redis密码

# JWT 密钥（生成方式: openssl rand -base64 48）
JWT_SECRET=你的随机密钥

# QQ 邮箱 SMTP（用于发外部邮件）
SMTP_USER=你的QQ邮箱@qq.com
SMTP_PASS=QQ邮箱授权码

# 智谱 AI（可选）
GLM_API_KEY=你的API密钥
```

### 3. 启动后端

```bash
mvn spring-boot:run
```

后端启动在 `http://localhost:8080`

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器启动在 `http://localhost:5173`

---

## 🐳 Docker 部署

### 方式一：一键部署（推荐）

适用于全新 Ubuntu 服务器：

```bash
# 将项目上传到服务器
scp -r . root@服务器IP:/opt/mail-system

# SSH 登录服务器
ssh root@服务器IP

# 执行一键部署
cd /opt/mail-system
bash setup-server.sh
```

脚本自动完成：系统更新 → Docker 安装 → Node.js 安装 → 前端构建 → 容器启动

### 方式二：手动 Docker Compose

```bash
# 1. 配置环境变量
cp .env.example .env
nano .env

# 2. 构建并启动
docker compose up -d --build

# 3. 查看状态
docker compose ps

# 4. 查看日志
docker compose logs -f app
```

### 方式三：HTTPS 部署

```bash
# 1. 申请 Let's Encrypt 证书
bash deploy/scripts/issue-cert.sh mail.yourdomain.com your@email.com

# 2. 切换 HTTPS 配置
cp deploy/nginx/conf.d/mail-https.conf deploy/nginx/conf.d/default.conf

# 3. 重启
docker compose restart nginx
```

---

## ⚙️ 配置说明

所有敏感配置通过 `.env` 文件管理，**绝不提交到 Git**。

| 变量 | 必填 | 说明 | 示例 |
|------|:----:|------|------|
| `TZ` | ✅ | 时区 | `Asia/Shanghai` |
| `DB_HOST` | ✅ | MySQL 地址 | `localhost` / `mysql`（Docker） |
| `DB_NAME` | ✅ | 数据库名 | `mail_system` |
| `DB_USER` | ✅ | 数据库用户 | `root` |
| `DB_PASS` | ✅ | 数据库密码 | — |
| `REDIS_PASS` | ✅ | Redis 密码 | — |
| `JWT_SECRET` | ✅ | JWT 签名密钥（≥32字符） | `openssl rand -base64 48` |
| `APP_ENCRYPTION_KEY` | ✅ | AES 加密密钥（32字符） | `openssl rand -base64 32` |
| `APP_MAIL_DOMAIN` | ❌ | 邮件域名 | `lneqse.com` |
| `REMOTE_API_SECRET` | ❌ | 远程 API 密钥 | — |
| `SMTP_USER` | ❌ | QQ 邮箱地址 | `xxx@qq.com` |
| `SMTP_PASS` | ❌ | QQ 邮箱授权码 | — |
| `GLM_API_KEY` | ❌ | 智谱 AI Key | `sk-xxx` |

---

## 🔌 API 接口

### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/auth/register` | 用户注册 |
| `POST` | `/api/auth/login` | 用户登录 |
| `PUT` | `/api/auth/password` | 修改密码 |
| `PUT` | `/api/auth/username` | 修改用户名 |
| `POST` | `/api/auth/avatar` | 上传头像 |
| `POST` | `/api/auth/bind-qq` | 绑定 QQ 邮箱 |

### 邮件

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/mail/inbox` | 收件箱（分页） |
| `GET` | `/api/mail/sent` | 发件箱（分页） |
| `GET` | `/api/mail/drafts` | 草稿箱（分页） |
| `GET` | `/api/mail/{id}` | 邮件详情 |
| `POST` | `/api/mail/send` | 发送邮件/保存草稿 |
| `PUT` | `/api/mail/{id}` | 更新草稿 |
| `DELETE` | `/api/mail/{id}` | 删除邮件 |
| `POST` | `/api/mail/recall/{id}` | 撤回邮件 |

### 联系人

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/contact` | 联系人列表 |
| `POST` | `/api/contact` | 添加联系人 |
| `PUT` | `/api/contact/{id}` | 修改联系人 |
| `DELETE` | `/api/contact/{id}` | 删除联系人 |

### 附件

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/attachment/upload/{mailId}` | 上传附件 |
| `GET` | `/api/attachment/download/{id}` | 下载附件 |

### 搜索

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/search` | 全文搜索 |

### AI

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/ai/optimize` | AI 文本润色 |

---

## 🗄️ 数据库设计

```sql
-- 用户表
users (id, username, email, password_hash, salt, qq_email, qq_auth_code, avatar, created_at)

-- 邮件表
mails (id, sender_id, receiver_id, subject, content, status, 
       sender_deleted, receiver_deleted, sent_at, has_attachments,
       is_remote, sender_server, remote_sender_email, created_at)

-- 附件表
attachments (id, mail_id, file_name, file_path, file_size, content_type, created_at)

-- 联系人表
contacts (id, owner_id, contact_id, email, nickname, group_name, created_at)

-- 邮件标记表（已读/星标/重要）
mail_flags (id, mail_id, user_id, is_read, is_starred, is_important, created_at, updated_at)
```

---

## 🔒 安全特性

| 防护层 | 措施 |
|--------|------|
| **认证** | JWT 令牌 + 24h 过期 |
| **密码** | BCrypt 哈希（不存储明文） |
| **XSS** | OWASP HTML Sanitizer 清理邮件内容 |
| **加密** | AES-GCM 加密 QQ 授权码等敏感字段 |
| **限流** | 登录/注册 5 次/分钟（Redis 计数器） |
| **CORS** | 限定来源域名，禁止通配符 |
| **HTTPS** | Let's Encrypt 自动证书 + HSTS |
| **HTTP 头** | X-Frame-Options、X-Content-Type-Options、CSP |
| **容器** | Docker 非 root 运行 |
| **文件** | 扩展名白名单 + 大小限制 |
| **Nginx** | 隐藏 .env/.git 等敏感文件 |

---

## 📜 部署脚本

| 脚本 | 用途 |
|------|------|
| `setup-server.sh` | 全新服务器初始化（安装 Docker、Node.js、部署） |
| `deploy/scripts/deploy.sh` | 项目部署（构建前端、启动容器） |
| `deploy/scripts/issue-cert.sh` | 申请 Let's Encrypt SSL 证书 |
| `deploy/scripts/backup-db.sh` | 数据库备份 |
| `deploy/scripts/backup-uploads.sh` | 附件备份 |

---

## ❓ 常见问题

### Q: 发送外部邮件失败？
确保已绑定 QQ 邮箱（设置 → 绑定 QQ 邮箱），QQ 邮箱需在 QQ 邮箱设置中开启 SMTP 并获取授权码。

### Q: 收不到外部邮件？
系统每 60 秒通过 IMAP 拉取绑定 QQ 邮箱的新邮件。确认 QQ 邮箱已开启 IMAP 服务。

### Q: Docker 构建失败？
确保服务器有足够的内存（建议 ≥2GB）和磁盘空间（构建需约 2GB）。Maven 首次构建需要下载依赖，可能需要 5-10 分钟。

### Q: 如何修改端口？
编辑 `docker-compose.yml` 中 nginx 的 ports 映射，如 `"8880:80"`。

---

## 📄 License

MIT
