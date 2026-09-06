#!/bin/bash
# ============================================
# Campus Market 一键部署脚本
# 功能: 推送代码到 GitHub → SSH 到云服务器拉取并重新构建 Docker
# ============================================

set -e

# ========== 配置 ==========
SERVER_HOST="${DEPLOY_SERVER_HOST:-your-server-ip}"
SERVER_USER="root"
SERVER_PROJECT_PATH="/opt/campus_platform"

# ========== 颜色输出 ==========
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # 无颜色

print_step() { echo -e "${BLUE}[步骤 $1/$TOTAL_STEPS]${NC} $2"; }
print_ok()   { echo -e "${GREEN}[完成]${NC} $1"; }
print_err()  { echo -e "${RED}[错误]${NC} $1"; }
print_warn() { echo -e "${YELLOW}[警告]${NC} $1"; }

TOTAL_STEPS=5

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  Campus Market 一键部署${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""

# ========== 步骤1: 获取 Commit 信息 ==========
print_step 1 "准备提交信息"

if [ -n "$1" ]; then
    COMMIT_MSG="$1"
    echo "  使用提供的提交信息: $COMMIT_MSG"
else
    echo -n "  请输入提交信息 (留空使用默认): "
    read -r COMMIT_MSG
    if [ -z "$COMMIT_MSG" ]; then
        COMMIT_MSG="deploy: $(date '+%Y-%m-%d %H:%M')"
        echo "  使用默认提交信息: $COMMIT_MSG"
    fi
fi

# ========== 步骤2: 本地编译验证 ==========
print_step 2 "本地编译验证"

echo "  后端编译中..."
if ! ./backend/mvnw -f backend/pom.xml compile -q 2>/dev/null; then
    print_err "后端编译失败，请修复后重试"
    exit 1
fi
echo "  后端编译: OK"

echo "  前端构建中..."
if ! (cd frontend && npx vite build 2>/dev/null); then
    print_err "前端构建失败，请修复后重试"
    exit 1
fi
echo "  前端构建: OK"

print_ok "本地验证通过"

# ========== 步骤3: 推送代码到 GitHub ==========
print_step 3 "推送代码到 GitHub"

echo "  git add ."
git add .
echo "  git commit -m \"$COMMIT_MSG\""
if git diff --cached --quiet; then
    echo "  没有变更需要提交，跳过 commit"
else
    git commit -m "$COMMIT_MSG"
fi
echo "  git push"
git push

print_ok "代码已推送到 GitHub"

# ========== 步骤4: 服务器拉取代码 ==========
print_step 4 "云服务器拉取最新代码"

ssh -o ConnectTimeout=10 "${SERVER_USER}@${SERVER_HOST}" << 'REMOTE_SCRIPT'
set -e
cd /opt/campus_platform
echo "  当前分支: $(git branch --show-current)"
echo "  git pull"
git pull
echo ""
echo "  git log -1 --oneline"
git log -1 --oneline
REMOTE_SCRIPT

if [ $? -ne 0 ]; then
    print_err "SSH 连接失败或 git pull 失败"
    print_warn "请检查: 1) 服务器是否在线 2) SSH 免密是否配置 3) 项目路径是否正确"
    exit 1
fi

print_ok "服务器代码已更新"

# ========== 步骤5: Docker 重新构建部署 ==========
print_step 5 "Docker 重新构建并部署"

ssh -o ConnectTimeout=10 "${SERVER_USER}@${SERVER_HOST}" << 'REMOTE_SCRIPT'
set -e
cd /opt/campus_platform

echo "  docker compose -f docker-compose.prod.yml build backend"
docker compose -f docker-compose.prod.yml build backend

echo ""
echo "  docker compose -f docker-compose.prod.yml up -d"
docker compose -f docker-compose.prod.yml up -d

echo ""
echo "  容器状态:"
docker compose -f docker-compose.prod.yml ps

echo ""
echo "  重新加载 Nginx 配置..."
docker compose -f docker-compose.prod.yml restart nginx

echo ""
echo "  清理旧镜像..."
docker image prune -f 2>/dev/null || true
REMOTE_SCRIPT

if [ $? -ne 0 ]; then
    print_err "Docker 构建或部署失败"
    exit 1
fi

print_ok "Docker 部署完成"

# ========== 完成 ==========
echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  部署成功!${NC}"
echo -e "${GREEN}  网站: https://c2cmarket.store${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
