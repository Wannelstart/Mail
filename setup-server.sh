#!/usr/bin/env bash
set -e

# ============================================
# Ubuntu 服务器初始化 + 邮件系统部署
# 适用于全新 Ubuntu 20.04/22.04/24.04 服务器
# 用法: bash setup-server.sh
# ============================================

echo "========================================"
echo "  🖥️  Ubuntu 服务器初始化"
echo "========================================"

# ---------- 1. 系统更新 ----------
echo ""
echo "📦 [1/6] 更新系统软件包..."
apt update && apt upgrade -y

# ---------- 2. 安装基础工具 ----------
echo ""
echo "🔧 [2/6] 安装基础工具..."
apt install -y curl wget git vim ca-certificates gnupg lsb-release

# ---------- 3. 安装 Docker ----------
echo ""
echo "🐳 [3/6] 安装 Docker..."
if command -v docker &>/dev/null; then
  echo "  Docker 已安装: $(docker --version)"
else
  # 卸载旧版本
  apt remove -y docker docker-engine docker.io containerd runc 2>/dev/null || true

  # 添加 Docker 官方 GPG key 和仓库
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg

  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
    $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
    tee /etc/apt/sources.list.d/docker.list > /dev/null

  apt update
  apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

  # 启动 Docker
  systemctl start docker
  systemctl enable docker

  echo "✅ Docker 安装完成: $(docker --version)"
  echo "✅ Docker Compose: $(docker compose version)"
fi

# ---------- 4. 安装 Node.js（用于构建前端） ----------
echo ""
echo "📦 [4/6] 安装 Node.js..."
if command -v node &>/dev/null; then
  echo "  Node.js 已安装: $(node --version)"
else
  curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
  apt install -y nodejs
  echo "✅ Node.js 安装完成: $(node --version)"
fi

# ---------- 5. 配置防火墙 ----------
echo ""
echo "🔥 [5/6] 配置防火墙..."
if command -v ufw &>/dev/null; then
  ufw allow 22/tcp    # SSH
  ufw allow 80/tcp    # HTTP
  ufw allow 443/tcp   # HTTPS
  echo "✅ 防火墙规则已添加 (22, 80, 443)"
else
  echo "  ufw 未安装，跳过防火墙配置"
fi

# ---------- 6. 部署项目 ----------
echo ""
echo "🚀 [6/6] 部署邮件系统..."
PROJECT_DIR="/opt/mail-system"

if [ -d "$PROJECT_DIR" ]; then
  echo "  项目目录已存在，执行更新..."
  cd "$PROJECT_DIR"
  git pull || echo "⚠️ git pull 失败，请手动更新代码"
else
  echo "❌ 请先将项目代码放到 $PROJECT_DIR"
  echo "   方式1: git clone <你的仓库> $PROJECT_DIR"
  echo "   方式2: scp -r ./代码 root@服务器IP:$PROJECT_DIR"
  echo ""
  echo "   放好后重新运行: bash setup-server.sh"
  exit 1
fi

cd "$PROJECT_DIR"

# 运行部署脚本
bash deploy/scripts/deploy.sh

echo ""
echo "========================================"
echo "  ✅ 部署完成！"
echo "========================================"
