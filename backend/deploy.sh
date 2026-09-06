#!/bin/bash

# 校园二手交易平台部署脚本
# 适用于学生期末作业项目部署

set -e  # 遇到错误立即退出

echo "========================================="
echo "校园二手交易平台 - 后端服务部署脚本"
echo "========================================="

# 检查参数
if [ $# -eq 0 ]; then
    echo "使用方法: $0 [dev|prod]"
    echo "  dev  - 开发环境部署"
    echo "  prod - 生产环境部署"
    exit 1
fi

ENV=$1
echo "部署环境: $ENV"

# 检查Java是否安装
if ! command -v java &> /dev/null; then
    echo "错误: Java未安装"
    echo "请安装Java 21或更高版本"
    exit 1
fi

# 检查Maven是否安装
if ! command -v mvn &> /dev/null; then
    echo "错误: Maven未安装"
    echo "请安装Maven 3.6或更高版本"
    exit 1
fi

echo "检查环境..."
java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
mvn_version=$(mvn -version 2>&1 | head -n 1 | cut -d' ' -f3)
echo "Java版本: $java_version"
echo "Maven版本: $mvn_version"

# 清理并构建项目
echo "清理并构建项目..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "错误: 项目构建失败"
    exit 1
fi

echo "项目构建成功!"

# 根据环境选择配置文件
if [ "$ENV" = "prod" ]; then
    echo "生产环境部署..."
    echo "请确保MySQL数据库已启动并配置正确"
    echo "默认配置:"
    echo "  - 数据库: localhost:3306/campus_market"
    echo "  - 用户名: root"
    echo "  - 密码: 123456"
    echo ""
    echo "如果需要修改配置，请编辑:"
    echo "  src/main/resources/application-prod.yml"
    echo ""
    read -p "是否继续? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "部署取消"
        exit 0
    fi
    
    # 设置环境变量
    export SPRING_PROFILES_ACTIVE=prod
    
    # 检查MySQL是否可用
    echo "检查MySQL连接..."
    if command -v mysql &> /dev/null; then
        if mysql -u root -p123456 -e "USE campus_market;" 2>/dev/null; then
            echo "MySQL连接成功!"
        else
            echo "警告: MySQL连接失败，请确保数据库已创建"
            echo "可以运行以下命令创建数据库:"
            echo "  mysql -u root -p123456 < src/main/resources/schema-mysql.sql"
            echo "  mysql -u root -p123456 < src/main/resources/data-mysql.sql"
        fi
    fi
    
    # 启动应用
    echo "启动后端服务..."
    JAR_FILE="target/backend-0.0.1-SNAPSHOT.jar"
    nohup java -jar $JAR_FILE > logs/app.log 2>&1 &
    PID=$!
    echo "应用已启动，进程ID: $PID"
    echo "日志文件: logs/app.log"
    echo "API地址: http://localhost:8080"
    echo "Swagger文档: http://localhost:8080/swagger-ui.html"
    
elif [ "$ENV" = "dev" ]; then
    echo "开发环境部署..."
    echo "使用H2内存数据库"
    
    # 设置环境变量
    export SPRING_PROFILES_ACTIVE=dev
    
    # 启动应用
    echo "启动后端服务..."
    JAR_FILE="target/backend-0.0.1-SNAPSHOT.jar"
    nohup java -jar $JAR_FILE > logs/app.log 2>&1 &
    PID=$!
    echo "应用已启动，进程ID: $PID"
    echo "日志文件: logs/app.log"
    echo "API地址: http://localhost:8080"
    echo "H2控制台: http://localhost:8080/h2-console"
    echo "Swagger文档: http://localhost:8080/swagger-ui.html"
else
    echo "错误: 未知环境 '$ENV'"
    echo "可用环境: dev, prod"
    exit 1
fi

# 等待应用启动
echo "等待应用启动..."
sleep 5

# 检查应用是否启动成功
if curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "✅ 应用启动成功!"
    echo ""
    echo "部署完成!"
    echo "========================================="
    echo "测试账号:"
    echo "  - 学生1: student1 / password123"
    echo "  - 学生2: student2 / password123"
    echo "  - 教师: teacher1 / password123"
    echo "========================================="
else
    echo "❌ 应用启动失败，请检查日志: logs/app.log"
    exit 1
fi