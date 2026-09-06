-- 帖子图片功能迁移脚本
-- 为 posts 表添加图片URL和封面图字段

ALTER TABLE posts ADD COLUMN image_urls JSON DEFAULT NULL COMMENT '图片URL列表(JSON数组)';
ALTER TABLE posts ADD COLUMN cover_image VARCHAR(500) DEFAULT NULL COMMENT '封面图URL';
