-- ============================================================
-- 数据库迁移脚本 - v4 (社区功能)
-- 用途：平台架构转型 + 商品故事功能
-- 原则：仅 CREATE TABLE 和 ALTER TABLE ADD COLUMN（非破坏性）
-- ============================================================

-- -----------------------------------------------------------
-- 1. 帖子表（社区内容核心）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS posts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL COMMENT '发布者用户ID',
    title           VARCHAR(200) NOT NULL COMMENT '帖子标题',
    content         TEXT         NOT NULL COMMENT '帖子正文（富文本）',
    post_type       VARCHAR(30)  NOT NULL DEFAULT 'DISCUSSION' COMMENT '类型：DISCUSSION/SHOWCASE/HELP/ACTIVITY',
    board_id        BIGINT       DEFAULT NULL COMMENT '所属板块ID（兴趣社区）',
    group_buy_id    BIGINT       DEFAULT NULL COMMENT '关联团购活动ID（可为空）',
    view_count      INT          DEFAULT 0 COMMENT '浏览次数',
    like_count      INT          DEFAULT 0 COMMENT '点赞次数',
    comment_count   INT          DEFAULT 0 COMMENT '评论次数',
    is_pinned       TINYINT(1)   DEFAULT 0 COMMENT '是否置顶',
    is_essence      TINYINT(1)   DEFAULT 0 COMMENT '是否精华',
    status          VARCHAR(20)  DEFAULT 'PUBLISHED' COMMENT '状态：PUBLISHED/DRAFT/HIDDEN/DELETED',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_board_id (board_id),
    INDEX idx_post_type (post_type),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_group_buy_id (group_buy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区帖子表';

-- -----------------------------------------------------------
-- 2. 帖子评论表（支持嵌套回复）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS post_comments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id         BIGINT       NOT NULL COMMENT '所属帖子ID',
    user_id         BIGINT       NOT NULL COMMENT '评论者用户ID',
    parent_id       BIGINT       DEFAULT NULL COMMENT '父评论ID（NULL为一级评论）',
    content         TEXT         NOT NULL COMMENT '评论内容',
    like_count      INT          DEFAULT 0 COMMENT '点赞次数',
    status          VARCHAR(20)  DEFAULT 'PUBLISHED' COMMENT '状态：PUBLISHED/HIDDEN/DELETED',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子评论表';

-- -----------------------------------------------------------
-- 3. 点赞记录表（通用，支持帖子/评论/商品等多目标类型）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_likes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL COMMENT '点赞用户ID',
    target_type     VARCHAR(30)  NOT NULL COMMENT '点赞目标类型：POST/COMMENT/PRODUCT',
    target_id       BIGINT       NOT NULL COMMENT '点赞目标ID',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通用点赞记录表';

-- -----------------------------------------------------------
-- 4. 用户关注表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_follows (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id     BIGINT       NOT NULL COMMENT '关注者用户ID',
    followee_id     BIGINT       NOT NULL COMMENT '被关注者用户ID',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    UNIQUE KEY uk_follow (follower_id, followee_id),
    INDEX idx_follower (follower_id),
    INDEX idx_followee (followee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注表';

-- -----------------------------------------------------------
-- 5. 商品表扩展（商品故事功能 - 幂等安全版本）
-- -----------------------------------------------------------
-- 重新创建辅助存储过程（以防 v3 已将其删除）
DROP PROCEDURE IF EXISTS add_column_if_not_exists_v4;

-- -----------------------------------------------------------
-- 6. 商品评论表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_comments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id      BIGINT       NOT NULL COMMENT '所属商品ID',
    user_id         BIGINT       NOT NULL COMMENT '评论者用户ID',
    parent_id       BIGINT       DEFAULT NULL COMMENT '父评论ID（NULL为一级评论）',
    content         TEXT         NOT NULL COMMENT '评论内容',
    like_count      INT          DEFAULT 0 COMMENT '点赞次数',
    status          VARCHAR(20)  DEFAULT 'PUBLISHED' COMMENT '状态：PUBLISHED/HIDDEN/DELETED',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    INDEX idx_product_id (product_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品评论表';

DELIMITER //
CREATE PROCEDURE add_column_if_not_exists_v4(
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

CALL add_column_if_not_exists_v4('products', 'story_title', 'VARCHAR(200) DEFAULT NULL COMMENT ''故事标题''');
CALL add_column_if_not_exists_v4('products', 'story_content', 'TEXT DEFAULT NULL COMMENT ''故事正文（富文本/长文本）''');
CALL add_column_if_not_exists_v4('products', 'story_images', 'TEXT DEFAULT NULL COMMENT ''故事配图URL（JSON数组，与商品主图独立）''');
CALL add_column_if_not_exists_v4('products', 'has_story', 'TINYINT(1) DEFAULT 0 COMMENT ''是否有故事内容（便于筛选）''');
CALL add_column_if_not_exists_v4('products', 'sale_mode', 'VARCHAR(20) DEFAULT ''FIXED_PRICE'' COMMENT ''销售模式：FIXED_PRICE（一口价）/AUCTION（拍卖）''');

DROP PROCEDURE IF EXISTS add_column_if_not_exists_v4;
