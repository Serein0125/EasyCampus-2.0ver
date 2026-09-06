-- 收藏功能修复迁移脚本
-- 为 favorites 表添加 target_type 字段，支持商品和帖子收藏

-- 1. 添加 target_type 字段（默认 PRODUCT，兼容已有数据）
ALTER TABLE favorites ADD COLUMN target_type VARCHAR(20) NOT NULL DEFAULT 'PRODUCT' COMMENT '目标类型: PRODUCT/POST' AFTER product_id;

-- 2. 删除旧的唯一索引（如果存在）
ALTER TABLE favorites DROP INDEX uk_user_product;

-- 3. 创建新的唯一索引（包含 target_type，支持同一用户收藏不同类型的目标）
ALTER TABLE favorites ADD UNIQUE INDEX uk_user_target (user_id, product_id, target_type);

-- 4. 删除旧的外键约束（如果存在，帖子收藏不需要关联 products 表）
-- 注意：如果外键名为其他名称，需要先查询实际外键名
-- SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_NAME = 'favorites' AND CONSTRAINT_TYPE = 'FOREIGN KEY';
ALTER TABLE favorites DROP FOREIGN KEY favorites_ibfk_2;
