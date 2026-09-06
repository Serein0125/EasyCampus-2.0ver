#!/bin/bash

# 校园二手交易平台 - 前端部署脚本
# 适用于学生期末作业项目部署

set -e  # 遇到错误立即退出

echo "========================================="
echo "校园二手交易平台 - 前端部署脚本"
echo "========================================="

# 检查参数
if [ $# -eq 0 ]; then
    echo "使用方法: $0 [dev|prod]"
    echo "  dev  - 开发环境部署（启动开发服务器）"
    echo "  prod - 生产环境部署（构建静态文件）"
    exit 1
fi

ENV=$1
echo "部署环境: $ENV"

# 检查Node.js是否安装
if ! command -v node &> /dev/null; then
    echo "错误: Node.js未安装"
    echo "请安装Node.js 18或更高版本"
    exit 1
fi

# 检查npm是否安装
if ! command -v npm &> /dev/null; then
    echo "错误: npm未安装"
    echo "请安装npm"
    exit 1
fi

echo "检查环境..."
node_version=$(node --version)
npm_version=$(npm --version)
echo "Node.js版本: $node_version"
echo "npm版本: $npm_version"

# 安装依赖
echo "安装依赖..."
npm install

if [ $? -ne 0 ]; then
    echo "错误: 依赖安装失败"
    exit 1
fi

echo "依赖安装成功!"

if [ "$ENV" = "prod" ]; then
    echo "生产环境部署..."
    echo "构建静态文件..."
    
    # 设置环境变量
    export VITE_API_BASE_URL=https://your-backend-domain.railway.app
    
    # 构建项目
    npm run build
    
    if [ $? -ne 0 ]; then
        echo "错误: 构建失败"
        exit 1
    fi
    
    echo "✅ 构建成功!"
    echo ""
    echo "构建文件位于: dist/"
    echo ""
    echo "部署方式:"
    echo "1. 使用Vercel部署（推荐）"
    echo "   - 注册Vercel账号"
    echo "   - 导入GitHub仓库"
    echo "   - 自动部署"
    echo ""
    echo "2. 使用Nginx部署"
    echo "   sudo cp -r dist/* /var/www/html/"
    echo "   sudo systemctl restart nginx"
    echo ""
    echo "3. 使用GitHub Pages部署"
    echo "   - 启用GitHub Pages"
    echo "   - 选择dist目录"
    echo ""
    echo "访问地址:"
    echo "  - Vercel: https://your-project.vercel.app"
    echo "  - GitHub Pages: https://your-username.github.io/your-repo"
    echo ""
    echo "测试账号:"
    echo "  - 学生1: student1 / password123"
    echo "  - 学生2: student2 / password123"
    echo "  - 教师: teacher1 / password123"
    
elif [ "$ENV" = "dev" ]; then
    echo "开发环境部署..."
    echo "启动开发服务器..."
    
    # 设置环境变量
    export VITE_API_BASE_URL=http://localhost:8080
    
    # 启动开发服务器
    echo "开发服务器启动中..."
    echo "前端地址: http://localhost:3000"
    echo "后端API: http://localhost:8080"
    echo ""
    echo "按 Ctrl+C 停止服务器"
    echo ""
    
    npm run dev
    
else
    echo "错误: 未知环境 '$ENV'"
    echo "可用环境: dev, prod"
    exit 1
fi