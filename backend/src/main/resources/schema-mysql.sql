-- ============================================================
-- 校园二手交易平台 (Campus Flea Market) - MySQL数据库表结构
-- 版本: 2.0 (闲鱼风格全面重构)
-- ============================================================

-- -----------------------------------------------------------
-- 1. 用户表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username        VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password_hash   VARCHAR(255) NOT NULL COMMENT '密码哈希(BCrypt)',
    phone           VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    email           VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    nickname        VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    avatar          VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    gender          TINYINT(1)   DEFAULT NULL COMMENT '性别:0未知,1男,2女',
    school          VARCHAR(100) DEFAULT NULL COMMENT '学校名称',
    major           VARCHAR(100) DEFAULT NULL COMMENT '专业',
    grade           VARCHAR(20)  DEFAULT NULL COMMENT '年级',
    wechat          VARCHAR(50)  DEFAULT NULL COMMENT '微信号',
    qq              VARCHAR(20)  DEFAULT NULL COMMENT 'QQ号',
    bio             VARCHAR(255) DEFAULT NULL COMMENT '个人简介',
    is_student      TINYINT(1)   DEFAULT 1 COMMENT '是否在校学生',
    campus          VARCHAR(100) DEFAULT NULL COMMENT '校区',
    status          TINYINT(1)   DEFAULT 1 COMMENT '状态:0禁用,1正常,2封禁',
    role            VARCHAR(20)  DEFAULT 'USER' COMMENT '角色:USER,ADMIN',
    last_login_at   DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_school (school)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- -----------------------------------------------------------
-- 2. 商品分类表 (树形结构)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    name            VARCHAR(50)  NOT NULL COMMENT '分类名称',
    description     VARCHAR(200) DEFAULT NULL COMMENT '分类描述',
    icon_url        VARCHAR(255) DEFAULT NULL COMMENT '图标URL',
    parent_id       BIGINT       DEFAULT NULL COMMENT '父分类ID(NULL表示根分类)',
    sort_order      INT          DEFAULT 0 COMMENT '排序序号(越小越靠前)',
    status          TINYINT(1)   DEFAULT 1 COMMENT '状态:0禁用,1启用',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL,
    INDEX idx_parent_id (parent_id),
    INDEX idx_sort_order (sort_order),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- -----------------------------------------------------------
-- 3. 商品表 (优化版 - 增加成色/原价等闲鱼风格字段)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '商品ID',
    name            VARCHAR(255) NOT NULL COMMENT '商品标题',
    description     TEXT         DEFAULT NULL COMMENT '商品详细描述',
    price           DECIMAL(10,2) NOT NULL COMMENT '现售价格',
    original_price  DECIMAL(10,2) DEFAULT NULL COMMENT '原价/全新价格',
    category_id     BIGINT       DEFAULT NULL COMMENT '所属分类ID',
    seller_id       BIGINT       NOT NULL COMMENT '卖家用户ID',
    condition_level TINYINT      DEFAULT NULL COMMENT '成色等级:1全新,2几乎全新,3轻微使用痕迹,4明显使用痕迹,5一般',
    image_urls      JSON         DEFAULT NULL COMMENT '商品图片URL列表(JSON数组)',
    cover_image     VARCHAR(255) DEFAULT NULL COMMENT '封面图片URL',
    status          TINYINT(1)   DEFAULT 1 COMMENT '状态:0已下架/删除,1在售,2已售出,3预约中',
    view_count      INT          DEFAULT 0 COMMENT '浏览次数',
    like_count      INT          DEFAULT 0 COMMENT '收藏/喜欢次数',
    location        VARCHAR(100) DEFAULT NULL COMMENT '交易地点(如:清华图书馆)',
    delivery_method TINYINT(1)   DEFAULT NULL COMMENT '交付方式:1自提,2快递,3均可',
    story_title     VARCHAR(200) DEFAULT NULL COMMENT '故事标题',
    story_content   TEXT         DEFAULT NULL COMMENT '故事正文(富文本)',
    story_images    TEXT         DEFAULT NULL COMMENT '故事配图URL(JSON数组)',
    has_story       TINYINT(1)   DEFAULT 0 COMMENT '是否有故事内容',
    sale_mode       VARCHAR(20)  DEFAULT 'FIXED_PRICE' COMMENT '销售模式:FIXED_PRICE/AUCTION',
    tags            VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)',
    campus_tag      VARCHAR(50)  DEFAULT NULL COMMENT '校区标签',
    org_id          BIGINT       DEFAULT NULL COMMENT '关联交易组织ID',
    visibility      VARCHAR(20)  DEFAULT 'PUBLIC' COMMENT '可见性:PUBLIC/ORG_ONLY',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_category_id (category_id),
    INDEX idx_seller_id (seller_id),
    INDEX idx_status (status),
    INDEX idx_price (price),
    INDEX idx_created_at (created_at),
    FULLTEXT INDEX ft_name_desc (name, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- -----------------------------------------------------------
-- 4. 聊天会话表 (核心新功能 - 用于私信系统)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_conversations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
    user1_id        BIGINT       NOT NULL COMMENT '参与者1的用户ID',
    user2_id        BIGINT       NOT NULL COMMENT '参与者2的用户ID',
    product_id      BIGINT       DEFAULT NULL COMMENT '关联的商品ID(可选)',
    last_message    TEXT         DEFAULT NULL COMMENT '最后一条消息内容',
    last_message_at DATETIME     DEFAULT NULL COMMENT '最后消息时间',
    user1_unread    INT          DEFAULT 0 COMMENT 'user1的未读消息数',
    user2_unread    INT          DEFAULT 0 COMMENT 'user2的未读消息数',
    status          TINYINT(1)   DEFAULT 1 COMMENT '状态:0已关闭,1正常',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user1_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (user2_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL,
    UNIQUE INDEX uk_user_pair (user1_id, user2_id),
    INDEX idx_user1 (user1_id),
    INDEX idx_user2 (user2_id),
    INDEX idx_product (product_id),
    INDEX idx_last_message_at (last_message_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

-- -----------------------------------------------------------
-- 5. 聊天消息表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_messages (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    conversation_id BIGINT       NOT NULL COMMENT '所属会话ID',
    sender_id       BIGINT       NOT NULL COMMENT '发送者用户ID',
    receiver_id     BIGINT       NOT NULL COMMENT '接收者用户ID',
    content         TEXT         NOT NULL COMMENT '消息内容',
    message_type    VARCHAR(20)  DEFAULT 'text' COMMENT '消息类型:text,image,system',
    is_read         TINYINT(1)   DEFAULT 0 COMMENT '是否已读:0未读,1已读',
    read_at         DATETIME     DEFAULT NULL COMMENT '阅读时间',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_created_at (created_at),
    INDEX idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- -----------------------------------------------------------
-- 6. 商品收藏表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS favorites (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    product_id      BIGINT       NOT NULL COMMENT '目标ID（商品ID或帖子ID）',
    target_type     VARCHAR(20)  NOT NULL DEFAULT 'PRODUCT' COMMENT '目标类型: PRODUCT/POST',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE INDEX uk_user_target (user_id, product_id, target_type),
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- -----------------------------------------------------------
-- 9. 社区帖子表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS posts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '帖子ID',
    user_id         BIGINT       NOT NULL COMMENT '发布者用户ID',
    title           VARCHAR(200) NOT NULL COMMENT '帖子标题',
    content         TEXT         NOT NULL COMMENT '帖子正文',
    post_type       VARCHAR(30)  NOT NULL DEFAULT 'DISCUSSION' COMMENT '类型:DISCUSSION/SHOWCASE/HELP/ACTIVITY',
    board_id        BIGINT       DEFAULT NULL COMMENT '所属板块ID',
    group_buy_id    BIGINT       DEFAULT NULL COMMENT '关联团购活动ID',
    view_count      INT          DEFAULT 0 COMMENT '浏览次数',
    like_count      INT          DEFAULT 0 COMMENT '点赞次数',
    comment_count   INT          DEFAULT 0 COMMENT '评论次数',
    is_pinned       TINYINT(1)   DEFAULT 0 COMMENT '是否置顶',
    is_essence      TINYINT(1)   DEFAULT 0 COMMENT '是否精华',
    status          VARCHAR(20)  DEFAULT 'PUBLISHED' COMMENT '状态:PUBLISHED/DRAFT/HIDDEN/DELETED',
    start_time      DATETIME     DEFAULT NULL COMMENT '活动开始时间',
    end_time        DATETIME     DEFAULT NULL COMMENT '活动结束时间',
    location        VARCHAR(200) DEFAULT NULL COMMENT '活动地点',
    contact         VARCHAR(200) DEFAULT NULL COMMENT '负责人联系方式',
    tags            VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)',
    campus_tag      VARCHAR(50)  DEFAULT NULL COMMENT '校区标签',
    is_ad           TINYINT(1)   DEFAULT 0 COMMENT '是否为广告',
    exposure_boost  INT          DEFAULT 1 COMMENT '曝光率提升倍数',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_board_id (board_id),
    INDEX idx_post_type (post_type),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_group_buy_id (group_buy_id),
    INDEX idx_posts_is_ad (is_ad)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区帖子表';

-- -----------------------------------------------------------
-- 10. 帖子评论表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS post_comments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
    post_id         BIGINT       NOT NULL COMMENT '所属帖子ID',
    user_id         BIGINT       NOT NULL COMMENT '评论者用户ID',
    parent_id       BIGINT       DEFAULT NULL COMMENT '父评论ID(NULL为一级评论)',
    content         TEXT         NOT NULL COMMENT '评论内容',
    like_count      INT          DEFAULT 0 COMMENT '点赞次数',
    status          VARCHAR(20)  DEFAULT 'PUBLISHED' COMMENT '状态:PUBLISHED/HIDDEN/DELETED',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子评论表';

-- -----------------------------------------------------------
-- 11. 商品评论表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_comments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
    product_id      BIGINT       NOT NULL COMMENT '所属商品ID',
    user_id         BIGINT       NOT NULL COMMENT '评论者用户ID',
    parent_id       BIGINT       DEFAULT NULL COMMENT '父评论ID(NULL为一级评论)',
    content         TEXT         NOT NULL COMMENT '评论内容',
    like_count      INT          DEFAULT 0 COMMENT '点赞次数',
    status          VARCHAR(20)  DEFAULT 'PUBLISHED' COMMENT '状态:PUBLISHED/HIDDEN/DELETED',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    INDEX idx_product_id (product_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品评论表';

-- -----------------------------------------------------------
-- 12. 通用点赞记录表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_likes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    user_id         BIGINT       NOT NULL COMMENT '点赞用户ID',
    target_type     VARCHAR(30)  NOT NULL COMMENT '目标类型:POST/COMMENT/PRODUCT',
    target_id       BIGINT       NOT NULL COMMENT '目标ID',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通用点赞记录表';

-- -----------------------------------------------------------
-- 13. 用户关注表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_follows (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    follower_id     BIGINT       NOT NULL COMMENT '关注者用户ID',
    followee_id     BIGINT       NOT NULL COMMENT '被关注者用户ID',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    UNIQUE KEY uk_follow (follower_id, followee_id),
    INDEX idx_follower (follower_id),
    INDEX idx_followee (followee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注表';

-- -----------------------------------------------------------
-- 14. 用户行为记录表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_behaviors (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    behavior_type   VARCHAR(30)  DEFAULT NULL COMMENT '行为类型',
    target_type     VARCHAR(30)  DEFAULT NULL COMMENT '目标类型',
    target_id       BIGINT       DEFAULT NULL COMMENT '目标ID',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为记录表';

-- -----------------------------------------------------------
-- 15. 交易组织表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS trade_organizations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '组织ID',
    name            VARCHAR(100) DEFAULT NULL COMMENT '组织名称',
    org_type        VARCHAR(50)  DEFAULT NULL COMMENT '组织类型',
    description     VARCHAR(500) DEFAULT NULL COMMENT '组织描述',
    logo_url        VARCHAR(500) DEFAULT NULL COMMENT 'Logo URL',
    banner_url      VARCHAR(500) DEFAULT NULL COMMENT 'Banner URL',
    contact_email   VARCHAR(100) DEFAULT NULL COMMENT '联系邮箱',
    website_url     VARCHAR(500) DEFAULT NULL COMMENT '网站URL',
    location        VARCHAR(200) DEFAULT NULL COMMENT '位置',
    founder_id      BIGINT       DEFAULT NULL COMMENT '创始人用户ID',
    join_type       VARCHAR(20)  DEFAULT NULL COMMENT '加入方式',
    status          VARCHAR(20)  DEFAULT 'PENDING' COMMENT '状态:PENDING/APPROVED/REJECTED',
    member_count    INT          DEFAULT 0 COMMENT '成员数量',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_founder_id (founder_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易组织表';

-- -----------------------------------------------------------
-- 16. 组织成员表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS org_members (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    org_id          BIGINT       NOT NULL COMMENT '组织ID',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    role            VARCHAR(20)  DEFAULT NULL COMMENT '角色',
    joined_at       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    INDEX idx_org_id (org_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织成员表';

-- -----------------------------------------------------------
-- 17. 组织加入申请表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS org_join_requests (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '申请ID',
    org_id          BIGINT       NOT NULL COMMENT '组织ID',
    user_id         BIGINT       NOT NULL COMMENT '申请用户ID',
    message         VARCHAR(500) DEFAULT NULL COMMENT '申请留言',
    status          VARCHAR(20)  DEFAULT 'PENDING' COMMENT '状态:PENDING/APPROVED/REJECTED',
    reviewer_id     BIGINT       DEFAULT NULL COMMENT '审核人ID',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    reviewed_at     DATETIME     DEFAULT NULL COMMENT '审核时间',
    INDEX idx_org_id (org_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织加入申请表';

-- -----------------------------------------------------------
-- 18. 组织邀请表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS org_invitations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '邀请ID',
    org_id          BIGINT       NOT NULL COMMENT '组织ID',
    inviter_id      BIGINT       NOT NULL COMMENT '邀请人ID',
    invitee_id      BIGINT       NOT NULL COMMENT '被邀请人ID',
    invite_code     VARCHAR(50)  DEFAULT NULL COMMENT '邀请码',
    status          VARCHAR(20)  DEFAULT 'PENDING' COMMENT '状态:PENDING/ACCEPTED/REJECTED',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    responded_at    DATETIME     DEFAULT NULL COMMENT '响应时间',
    INDEX idx_org_id (org_id),
    INDEX idx_invitee_id (invitee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织邀请表';

-- -----------------------------------------------------------
-- 19. 组织审计日志表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS org_audit_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    org_id          BIGINT       NOT NULL COMMENT '组织ID',
    actor_id        BIGINT       NOT NULL COMMENT '操作者ID',
    action          VARCHAR(50)  DEFAULT NULL COMMENT '操作类型',
    target_id       BIGINT       DEFAULT NULL COMMENT '目标ID',
    detail          VARCHAR(500) DEFAULT NULL COMMENT '详情',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_org_id (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织审计日志表';

-- -----------------------------------------------------------
-- 20. 验证码表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS verification_codes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '验证码ID',
    account         VARCHAR(100) NOT NULL COMMENT '关联的邮箱或手机号',
    code            VARCHAR(6)   NOT NULL COMMENT '验证码',
    type            VARCHAR(30)  NOT NULL DEFAULT 'RESET_PASSWORD' COMMENT '类型:RESET_PASSWORD',
    used            TINYINT(1)   DEFAULT 0 COMMENT '是否已使用:0未使用,1已使用',
    expire_at       DATETIME     NOT NULL COMMENT '过期时间',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_account_type (account, type),
    INDEX idx_expire_at (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='验证码表';

CREATE TABLE IF NOT EXISTS notifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
    user_id         BIGINT       NOT NULL COMMENT '接收通知的用户ID',
    actor_id        BIGINT       NOT NULL COMMENT '触发通知的用户ID',
    type            VARCHAR(20)  NOT NULL COMMENT '通知类型: LIKE/COMMENT/FOLLOW',
    target_id       BIGINT       NOT NULL COMMENT '关联目标ID',
    target_type     VARCHAR(20)  NOT NULL COMMENT '目标类型: POST/PRODUCT/USER',
    content         VARCHAR(500) NOT NULL COMMENT '通知内容',
    is_read         TINYINT(1)   DEFAULT 0 COMMENT '是否已读: 0未读, 1已读',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_user_type (user_id, type),
    INDEX idx_user_unread (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';
