-- 广告功能迁移脚本
-- 为 posts 表添加广告标识和曝光率提升字段

ALTER TABLE posts ADD COLUMN is_ad TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为广告';
ALTER TABLE posts ADD COLUMN exposure_boost INT NOT NULL DEFAULT 1 COMMENT '曝光率提升倍数';

-- 为已有广告数据创建索引（可选，提升查询性能）
CREATE INDEX idx_posts_is_ad ON posts(is_ad);
