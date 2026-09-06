SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password_hash   VARCHAR(255) NOT NULL COMMENT '密码哈希(BCrypt)',
    phone           VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    email           VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    nickname        VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    avatar          VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    gender          TINYINT(1)   DEFAULT NULL COMMENT '性别:0未知,1男,2女',
    school          VARCHAR(100) DEFAULT NULL COMMENT '学校名称',
    campus          VARCHAR(100) DEFAULT NULL COMMENT '校区',
    major           VARCHAR(100) DEFAULT NULL COMMENT '专业',
    grade           VARCHAR(20)  DEFAULT NULL COMMENT '年级',
    wechat          VARCHAR(50)  DEFAULT NULL COMMENT '微信号',
    qq              VARCHAR(20)  DEFAULT NULL COMMENT 'QQ号',
    bio             VARCHAR(255) DEFAULT NULL COMMENT '个人简介',
    is_student      TINYINT(1)   DEFAULT 1 COMMENT '是否在校学生',
    status          TINYINT(1)   DEFAULT 1 COMMENT '状态:0禁用,1正常,2封禁',
    role            VARCHAR(20)  DEFAULT 'USER' COMMENT '角色:USER,ADMIN',
    last_login_at   DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS categories (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(50)  NOT NULL COMMENT '分类名称',
    description     VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
    icon_url        VARCHAR(255) DEFAULT NULL COMMENT '图标URL',
    parent_id       BIGINT       DEFAULT NULL COMMENT '父分类ID(NULL表示一级分类)',
    sort_order      INT          DEFAULT 0 COMMENT '排序序号',
    status          TINYINT(1)   DEFAULT 1 COMMENT '状态:0禁用,1正常',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent_id (parent_id),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

CREATE TABLE IF NOT EXISTS products (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200) NOT NULL COMMENT '商品标题',
    description     TEXT         DEFAULT NULL COMMENT '商品描述',
    price           DECIMAL(10,2) NOT NULL COMMENT '售价',
    original_price  DECIMAL(10,2) DEFAULT NULL COMMENT '原价/全新价格',
    category_id     BIGINT       NOT NULL COMMENT '分类ID',
    seller_id       BIGINT       NOT NULL COMMENT '卖家用户ID',
    condition_level TINYINT      DEFAULT NULL COMMENT '成色:1全新,2几乎新,3轻微使用,4明显使用,5一般',
    image_urls      JSON         DEFAULT NULL COMMENT '图片URL列表(JSON数组)',
    cover_image     VARCHAR(255) DEFAULT NULL COMMENT '封面图URL',
    status          TINYINT(1)   DEFAULT 1 COMMENT '状态:0下架/已售,1在售,2审核中',
    view_count      INT          DEFAULT 0 COMMENT '浏览次数',
    like_count      INT          DEFAULT 0 COMMENT '收藏次数',
    location        VARCHAR(100) DEFAULT NULL COMMENT '交易地点',
    delivery_method TINYINT(1)   DEFAULT NULL COMMENT '交付方式:1自提,2快递,3均可',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (seller_id) REFERENCES users(id),
    INDEX idx_category_id (category_id),
    INDEX idx_seller_id (seller_id),
    INDEX idx_status (status),
    INDEX idx_price (price),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

CREATE TABLE IF NOT EXISTS orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no        VARCHAR(32)  NOT NULL UNIQUE COMMENT '订单编号',
    buyer_id        BIGINT       NOT NULL COMMENT '买家ID',
    seller_id       BIGINT       NOT NULL COMMENT '卖家ID',
    total_amount    DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    status          VARCHAR(20)  DEFAULT 'pending' COMMENT '状态:pending/completed/cancelled/refunded',
    remark          VARCHAR(255) DEFAULT NULL COMMENT '买家备注',
    address         VARCHAR(255) DEFAULT NULL COMMENT '交易地点/收货地址',
    completed_at    DATETIME     DEFAULT NULL COMMENT '完成时间',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (buyer_id) REFERENCES users(id),
    FOREIGN KEY (seller_id) REFERENCES users(id),
    INDEX idx_buyer_id (buyer_id),
    INDEX idx_seller_id (seller_id),
    INDEX idx_status (status),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

CREATE TABLE IF NOT EXISTS order_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT       NOT NULL COMMENT '订单ID',
    product_id      BIGINT       NOT NULL COMMENT '商品ID',
    product_name    VARCHAR(200) NOT NULL COMMENT '商品名称(快照)',
    product_image   VARCHAR(255) DEFAULT NULL COMMENT '商品图片(快照)',
    price           DECIMAL(10,2) NOT NULL COMMENT '单价',
    quantity        INT          DEFAULT 1 COMMENT '数量',
    subtotal        DECIMAL(10,2) NOT NULL COMMENT '小计',
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单项表';

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
    FOREIGN KEY (user1_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (user2_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL,
    INDEX idx_user1 (user1_id),
    INDEX idx_user2 (user2_id),
    INDEX idx_product (product_id),
    INDEX idx_last_message_at (last_message_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

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
    FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

CREATE TABLE IF NOT EXISTS favorites (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    product_id      BIGINT       NOT NULL,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    UNIQUE INDEX uk_user_product (user_id, product_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

SET FOREIGN_KEY_CHECKS = 1;
