-- ============================================================
-- 数据库结构修复脚本 - v3（幂等版本）
-- 用途：为已有数据库添加缺失的字段，可安全重复执行
-- 策略：使用存储过程检查列是否存在，仅在缺失时添加
-- ============================================================

-- 创建辅助存储过程（如果不存在）
DROP PROCEDURE IF EXISTS add_column_if_not_exists;

DELIMITER //
CREATE PROCEDURE add_column_if_not_exists(
    IN tbl_name VARCHAR(128),
    IN col_name VARCHAR(128),
    IN col_def TEXT
)
BEGIN
    DECLARE col_count INT DEFAULT 0;
    SELECT COUNT(*) INTO col_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = tbl_name
      AND COLUMN_NAME = col_name;

    IF col_count = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE ', tbl_name, ' ADD COLUMN ', col_name, ' ', col_def);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- -----------------------------------------------------------
-- 1. 修复 users 表 - 添加缺失字段
-- -----------------------------------------------------------
CALL add_column_if_not_exists('users', 'nickname', 'VARCHAR(50) DEFAULT NULL COMMENT ''昵称''');
CALL add_column_if_not_exists('users', 'avatar', 'VARCHAR(500) DEFAULT NULL COMMENT ''头像URL''');
CALL add_column_if_not_exists('users', 'gender', 'TINYINT(1) DEFAULT NULL COMMENT ''性别:0未知,1男,2女''');
CALL add_column_if_not_exists('users', 'school', 'VARCHAR(100) DEFAULT NULL COMMENT ''学校名称''');
CALL add_column_if_not_exists('users', 'major', 'VARCHAR(100) DEFAULT NULL COMMENT ''专业''');
CALL add_column_if_not_exists('users', 'grade', 'VARCHAR(20) DEFAULT NULL COMMENT ''年级''');
CALL add_column_if_not_exists('users', 'wechat', 'VARCHAR(50) DEFAULT NULL COMMENT ''微信号''');
CALL add_column_if_not_exists('users', 'qq', 'VARCHAR(20) DEFAULT NULL COMMENT ''QQ号''');
CALL add_column_if_not_exists('users', 'bio', 'VARCHAR(255) DEFAULT NULL COMMENT ''个人简介''');
CALL add_column_if_not_exists('users', 'is_student', 'TINYINT(1) DEFAULT 1 COMMENT ''是否在校学生''');
CALL add_column_if_not_exists('users', 'status', 'TINYINT(1) DEFAULT 1 COMMENT ''状态:0禁用,1正常,2封禁''');
CALL add_column_if_not_exists('users', 'last_login_at', 'DATETIME DEFAULT NULL COMMENT ''最后登录时间''');
CALL add_column_if_not_exists('users', 'updated_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''');

-- 修复 phone/email 列定义（如果列类型需要调整）
CALL add_column_if_not_exists('users', 'phone', 'VARCHAR(20) DEFAULT NULL COMMENT ''手机号''');
CALL add_column_if_not_exists('users', 'email', 'VARCHAR(100) DEFAULT NULL COMMENT ''邮箱''');

-- 添加索引（幂等）
CREATE INDEX IF NOT EXISTS idx_username ON users (username);
CREATE INDEX IF NOT EXISTS idx_phone ON users (phone);
CREATE INDEX IF NOT EXISTS idx_status ON users (status);
CREATE INDEX IF NOT EXISTS idx_school ON users (school);

-- -----------------------------------------------------------
-- 2. 修复 categories 表
-- -----------------------------------------------------------
CALL add_column_if_not_exists('categories', 'description', 'VARCHAR(255) DEFAULT NULL COMMENT ''分类描述''');
CALL add_column_if_not_exists('categories', 'icon_url', 'VARCHAR(255) DEFAULT NULL COMMENT ''图标URL''');
CALL add_column_if_not_exists('categories', 'parent_id', 'BIGINT DEFAULT NULL COMMENT ''父分类ID''');
CALL add_column_if_not_exists('categories', 'sort_order', 'INT DEFAULT 0 COMMENT ''排序序号''');
CALL add_column_if_not_exists('categories', 'status', 'TINYINT(1) DEFAULT 1 COMMENT ''状态:0禁用,1正常''');
CALL add_column_if_not_exists('categories', 'updated_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''');

-- -----------------------------------------------------------
-- 3. 修复 products 表
-- -----------------------------------------------------------
CALL add_column_if_not_exists('products', 'original_price', 'DECIMAL(10,2) DEFAULT NULL COMMENT ''原价/全新价格''');
CALL add_column_if_not_exists('products', 'condition_level', 'TINYINT DEFAULT NULL COMMENT ''成色:1全新,2几乎新,3轻微使用,4明显使用,5一般''');
CALL add_column_if_not_exists('products', 'image_urls', 'JSON DEFAULT NULL COMMENT ''图片URL列表(JSON数组)''');
CALL add_column_if_not_exists('products', 'cover_image', 'VARCHAR(255) DEFAULT NULL COMMENT ''封面图URL''');
CALL add_column_if_not_exists('products', 'status', 'TINYINT(1) DEFAULT 1 COMMENT ''状态:0下架/已售,1在售,2审核中''');
CALL add_column_if_not_exists('products', 'view_count', 'INT DEFAULT 0 COMMENT ''浏览次数''');
CALL add_column_if_not_exists('products', 'like_count', 'INT DEFAULT 0 COMMENT ''收藏次数''');
CALL add_column_if_not_exists('products', 'location', 'VARCHAR(100) DEFAULT NULL COMMENT ''交易地点''');
CALL add_column_if_not_exists('products', 'delivery_method', 'TINYINT(1) DEFAULT NULL COMMENT ''交付方式:1自提,2快递,3均可''');
CALL add_column_if_not_exists('products', 'updated_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''');

-- -----------------------------------------------------------
-- 4. 修复 orders 表
-- -----------------------------------------------------------
CALL add_column_if_not_exists('orders', 'address', 'VARCHAR(255) DEFAULT NULL COMMENT ''交易地点/收货地址''');
CALL add_column_if_not_exists('orders', 'completed_at', 'DATETIME DEFAULT NULL COMMENT ''完成时间''');
CALL add_column_if_not_exists('orders', 'updated_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''');

-- -----------------------------------------------------------
-- 5. 创建 chat_conversations 表（如果不存在）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_conversations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user1_id        BIGINT       NOT NULL,
    user2_id        BIGINT       NOT NULL,
    product_id      BIGINT       DEFAULT NULL,
    last_message    TEXT         DEFAULT NULL,
    last_message_at DATETIME     DEFAULT NULL,
    user1_unread    INT          DEFAULT 0,
    user2_unread    INT          DEFAULT 0,
    status          TINYINT(1)   DEFAULT 1,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user1 (user1_id),
    INDEX idx_user2 (user2_id),
    INDEX idx_product (product_id),
    INDEX idx_last_message_at (last_message_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

-- -----------------------------------------------------------
-- 6. 创建 chat_messages 表（如果不存在）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_messages (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT       NOT NULL,
    sender_id       BIGINT       NOT NULL,
    receiver_id     BIGINT       NOT NULL,
    content         TEXT         NOT NULL,
    message_type    VARCHAR(20)  DEFAULT 'text',
    is_read         TINYINT(1)   DEFAULT 0,
    read_at         DATETIME     DEFAULT NULL,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- -----------------------------------------------------------
-- 7. 创建 favorites 表（如果不存在）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS favorites (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    product_id      BIGINT       NOT NULL,
    target_type     VARCHAR(20)  NOT NULL DEFAULT 'PRODUCT',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_user_target (user_id, product_id, target_type),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- -----------------------------------------------------------
-- 8. 创建 order_items 表（如果不存在）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT       NOT NULL COMMENT '订单ID',
    product_id      BIGINT       NOT NULL COMMENT '商品ID',
    product_name    VARCHAR(200) NOT NULL COMMENT '商品名称(快照)',
    product_image   VARCHAR(255) DEFAULT NULL COMMENT '商品图片(快照)',
    price           DECIMAL(10,2) NOT NULL COMMENT '单价',
    quantity        INT          DEFAULT 1 COMMENT '数量',
    subtotal        DECIMAL(10,2) NOT NULL COMMENT '小计',
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单项表';

-- -----------------------------------------------------------
-- 清理辅助存储过程
-- -----------------------------------------------------------
DROP PROCEDURE IF EXISTS add_column_if_not_exists;
