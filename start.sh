#!/bin/sh

echo "========================================="
echo "Starting Campus Market Backend..."
echo "========================================="

echo "Environment variables:"
echo "SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-not set}"

export DB_HOST="${DB_HOST:-${MYSQLHOST:-${MYSQL_HOST:-localhost}}}"
export DB_PORT="${DB_PORT:-${MYSQLPORT:-${MYSQL_PORT:-3306}}}"
export DB_NAME="${DB_NAME:-${MYSQLDATABASE:-${MYSQL_DATABASE:-campus_market}}}"
export DB_USER="${DB_USER:-${MYSQLUSER:-${MYSQL_USER:-root}}}"
export DB_PASSWORD="${DB_PASSWORD:-${MYSQLPASSWORD:-${MYSQL_ROOT_PASSWORD:-${MYSQL_PASSWORD:-123456}}}}"

echo "Final database configuration:"
echo "DB_HOST: ${DB_HOST}"
echo "DB_PORT: ${DB_PORT}"
echo "DB_NAME: ${DB_NAME}"
echo "DB_USER: ${DB_USER}"
echo "DB_PASSWORD: *****"
echo "========================================="

if [ ! -f "/app/backend.jar" ]; then
    echo "Error: JAR file not found at /app/backend.jar"
    exit 1
fi

# 确保上传目录存在（兼容卷挂载后权限问题）
UPLOAD_DIR="${APP_UPLOAD_BASE_PATH:-/app/uploads}"
mkdir -p "$UPLOAD_DIR" "$UPLOAD_DIR/temp" 2>/dev/null || true
echo "Upload directory: $UPLOAD_DIR"

echo "JAR file found. Starting application..."
java -version
echo "Starting Spring Boot application..."

exec java -Xmx512m -Xms256m \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} \
    -Dlogging.level.com.campus.backend=INFO \
    -Dlogging.level.org.springframework=WARN \
    -Dlogging.level.org.springframework.jdbc=WARN \
    -Dlogging.level.org.hibernate=WARN \
    -jar /app/backend.jar
