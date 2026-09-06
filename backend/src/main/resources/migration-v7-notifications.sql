-- ============================================
-- 通知功能迁移脚本 (v7)
-- 创建 notifications 表，修复通知功能完全失效问题
-- ============================================

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
