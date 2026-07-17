#!/usr/bin/env bash
set -e

# ============================================
# 邮件系统部署脚本 — mail.lneqse.com
# 用法: bash deploy/scripts/deploy.sh
# ============================================

PROJECT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$PROJECT_DIR"

echo "========================================"
echo "  邮件系统部署 — mail.lneqse.com"
echo "========================================"

# 1. 检查 .env 是否存在
if [ ! -f .env ]; then
  echo "❌ .env 文件不存在，请先从 .env.example 复制并填入配置"
  echo "   cp .env.example .env && vim .env"
  exit 1
fi

# 2. 检查并生成安全密钥
source .env
if [ -z "$JWT_SECRET" ] || echo "$JWT_SECRET" | grep -q "change-me"; then
  echo "⚠️  JWT_SECRET 未设置，正在生成..."
  NEW_SECRET=$(openssl rand -base64 48)
  sed -i "s|^JWT_SECRET=.*|JWT_SECRET=${NEW_SECRET}|" .env
  echo "✅ JWT_SECRET 已自动生成"
fi

if [ -z "$APP_ENCRYPTION_KEY" ] || echo "$APP_ENCRYPTION_KEY" | grep -q "change-me"; then
  echo "⚠️  APP_ENCRYPTION_KEY 未设置，正在生成..."
  NEW_KEY=$(openssl rand -base64 32)
  sed -i "s|^APP_ENCRYPTION_KEY=.*|APP_ENCRYPTION_KEY=${NEW_KEY}|" .env
  echo "✅ APP_ENCRYPTION_KEY 已自动生成"
fi

# 3. 构建前端
echo ""
echo "📦 构建前端..."
cd frontend
if [ ! -d node_modules ]; then
  npm ci
fi
npm run build
cd ..
echo "✅ 前端构建完成"

# 4. 创建必要目录
mkdir -p deploy/nginx/certbot/www
mkdir -p deploy/nginx/certbot/conf

# 5. 启动 Docker 容器
echo ""
echo "🐳 启动 Docker 容器..."
docker compose down 2>/dev/null || true
docker compose up -d --build

# 6. 等待服务就绪
echo ""
echo "⏳ 等待服务启动（约 30 秒）..."
sleep 15

# 检查 MySQL 和 Redis 是否就绪
for i in $(seq 1 12); do
  if docker compose ps mysql | grep -q "healthy" && docker compose ps redis | grep -q "healthy"; then
    break
  fi
  echo "  等待数据库就绪... ($i/12)"
  sleep 5
done

docker compose ps

# 7. 输出结果
echo ""
NGINX_STATUS=$(docker compose ps nginx 2>/dev/null | grep -c "Up" || true)
APP_STATUS=$(docker compose ps app 2>/dev/null | grep -c "Up" || true)

if [ "$NGINX_STATUS" -gt 0 ] && [ "$APP_STATUS" -gt 0 ]; then
  echo "✅ 所有服务已成功启动！"
  echo ""
  echo "🌐 HTTP 访问: http://mail.lneqse.com"
  echo ""
  echo "========================================"
  echo "  📋 下一步：启用 HTTPS"
  echo "========================================"
  echo ""
  echo "  1. 确保 DNS 已将 mail.lneqse.com 解析到本机公网 IP"
  echo "  2. 申请 Let's Encrypt 证书："
  echo "     bash deploy/scripts/issue-cert.sh mail.lneqse.com your-email@example.com"
  echo "  3. 切换到 HTTPS 配置："
  echo "     cp deploy/nginx/conf.d/mail-https.conf deploy/nginx/conf.d/default.conf"
  echo "     docker compose restart nginx"
  echo "  4. 验证："
  echo "     curl -I https://mail.lneqse.com"
  echo ""
else
  echo "❌ 部分服务启动异常，请查看日志："
  echo "   docker compose logs -f"
fi
