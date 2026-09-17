-- ============================================================
-- 看板演示种子数据 - 分配给账号 user02 (seller_id=2)
-- 目的：让「卖家数据看板」页面有真实可展示的聚合数据
-- 特点：覆盖近 30 天 / 多分类 / 含已售出(status=2)形成成交
-- 幂等：使用 INSERT IGNORE，重复执行不会重复插入
-- 执行：在本地 MySQL (campus_market_dev) 手动执行本文件
--   mysql -uroot campus_market_dev < seed-dashboard-data.sql
-- 字段说明：delivery_method 1自提/2快递/3均可；sale_mode FIXED_PRICE/AUCTION
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

INSERT IGNORE INTO products
  (name, description, price, category_id, seller_id, condition_level,
   image_urls, cover_image, location, delivery_method, status, view_count, like_count,
   sale_mode, tags, created_at, updated_at)
VALUES
-- ---------- 近30天发布 38 件，含已售出商品 ----------
('iPhone 13 国行 128G', '成色好，无拆修，电池健康 88%', 1899.00, 11, 2, 2,
 '["https://picsum.photos/400/400?random=11"]', 'https://picsum.photos/400/400?random=11', '学生宿舍', 1, 2, 620, 88, 'FIXED_PRICE', '手机,iPhone', NOW() - INTERVAL 29 DAY, NOW()),
('高数同济第七版上', '笔记少，几乎全新，含课后答案', 18.00, 21, 2, 1,
 '["https://picsum.photos/400/400?random=12"]', 'https://picsum.photos/400/400?random=12', '图书馆', 1, 1, 300, 45, 'FIXED_PRICE', '教材,高数', NOW() - INTERVAL 29 DAY, NOW()),
('机械键盘 87键红轴', '手感好，键帽近全新，送拔键器', 210.00, 13, 2, 2,
 '["https://picsum.photos/400/400?random=13"]', 'https://picsum.photos/400/400?random=13', '学生宿舍', 1, 1, 356, 41, 'FIXED_PRICE', '外设,键盘', NOW() - INTERVAL 28 DAY, NOW()),
('线性代数学习指导', '考研用，刷题标记多', 25.00, 21, 2, 3,
 '["https://picsum.photos/400/400?random=14"]', 'https://picsum.photos/400/400?random=14', '图书馆', 1, 1, 150, 12, 'FIXED_PRICE', '教材,线代', NOW() - INTERVAL 28 DAY, NOW()),
('小米手环7 NFC', '心率睡眠检测，表带换新', 95.00, 13, 2, 2,
 '["https://picsum.photos/400/400?random=15"]', 'https://picsum.photos/400/400?random=15', '学生宿舍', 1, 2, 298, 33, 'FIXED_PRICE', '智能穿戴,小米', NOW() - INTERVAL 27 DAY, NOW()),
('羽毛球拍全碳素', '打感扎实，穿线68P', 260.00, 4, 2, 3,
 '["https://picsum.photos/400/400?random=16"]', 'https://picsum.photos/400/400?random=16', '体育馆', 1, 1, 257, 29, 'FIXED_PRICE', '运动,羽毛球', NOW() - INTERVAL 27 DAY, NOW()),
('大学英语四级真题', '附听力材料，做了一半', 15.00, 23, 2, 3,
 '["https://picsum.photos/400/400?random=17"]', 'https://picsum.photos/400/400?random=17', '图书馆', 1, 1, 210, 20, 'FIXED_PRICE', '考试,四六级', NOW() - INTERVAL 26 DAY, NOW()),
('无线蓝牙耳机', '降噪款，续航6h', 120.00, 14, 2, 2,
 '["https://picsum.photos/400/400?random=18"]', 'https://picsum.photos/400/400?random=18', '学生宿舍', 1, 2, 410, 51, 'FIXED_PRICE', '数码,耳机', NOW() - INTERVAL 26 DAY, NOW()),
('宿舍小台灯护眼', '三档调光，USB充电', 35.00, 33, 2, 1,
 '["https://picsum.photos/400/400?random=19"]', 'https://picsum.photos/400/400?random=19', '学生宿舍', 1, 1, 88, 9, 'FIXED_PRICE', '宿舍,台灯', NOW() - INTERVAL 25 DAY, NOW()),
('《三体》三册套装', '读完仅一次，书页微黄', 45.00, 22, 2, 2,
 '["https://picsum.photos/400/400?random=20"]', 'https://picsum.photos/400/400?random=20', '图书馆', 1, 1, 176, 22, 'FIXED_PRICE', '小说,科幻', NOW() - INTERVAL 24 DAY, NOW()),
('PS5手柄双人套装', '共两个，均无漂移', 380.00, 13, 2, 2,
 '["https://picsum.photos/400/400?random=21"]', 'https://picsum.photos/400/400?random=21', '学生宿舍', 1, 1, 330, 40, 'FIXED_PRICE', '游戏,主机', NOW() - INTERVAL 24 DAY, NOW()),
('考研数学全书习题', '配套视频讲解', 40.00, 23, 2, 3,
 '["https://picsum.photos/400/400?random=22"]', 'https://picsum.photos/400/400?random=22', '图书馆', 1, 1, 140, 15, 'FIXED_PRICE', '考研,数学', NOW() - INTERVAL 23 DAY, NOW()),
('宿舍多功能置物架', '可折叠，节省空间', 28.00, 31, 2, 1,
 '["https://picsum.photos/400/400?random=23"]', 'https://picsum.photos/400/400?random=23', '学生宿舍', 1, 1, 66, 7, 'FIXED_PRICE', '宿舍,置物', NOW() - INTERVAL 22 DAY, NOW()),
('口袋打印机喵喵机', '热敏打印，蓝牙连接', 89.00, 13, 2, 2,
 '["https://picsum.photos/400/400?random=24"]', 'https://picsum.photos/400/400?random=24', '学生宿舍', 1, 2, 255, 28, 'FIXED_PRICE', '数码,打印', NOW() - INTERVAL 22 DAY, NOW()),
('Java编程思想第二版', '经典入门，有划线笔记', 30.00, 21, 2, 3,
 '["https://picsum.photos/400/400?random=25"]', 'https://picsum.photos/400/400?random=25', '图书馆', 1, 1, 190, 18, 'FIXED_PRICE', '编程,教材', NOW() - INTERVAL 21 DAY, NOW()),
('便携榨汁杯', 'USB充电，出行方便', 45.00, 32, 2, 1,
 '["https://picsum.photos/400/400?random=26"]', 'https://picsum.photos/400/400?random=26', '学生宿舍', 1, 1, 92, 11, 'FIXED_PRICE', '生活,榨汁', NOW() - INTERVAL 20 DAY, NOW()),
('熨烫挂烫机', '宿舍可用，小巧', 70.00, 32, 2, 2,
 '["https://picsum.photos/400/400?random=27"]', 'https://picsum.photos/400/400?random=27', '学生宿舍', 1, 1, 120, 10, 'FIXED_PRICE', '生活,家电', NOW() - INTERVAL 19 DAY, NOW()),
('骑行头盔', '带灯，安全合格', 55.00, 4, 2, 2,
 '["https://picsum.photos/400/400?random=28"]', 'https://picsum.photos/400/400?random=28', '体育馆', 1, 1, 105, 8, 'FIXED_PRICE', '运动,骑行', NOW() - INTERVAL 19 DAY, NOW()),
('考研英语真题一刷', '词汇笔记丰富', 22.00, 23, 2, 3,
 '["https://picsum.photos/400/400?random=29"]', 'https://picsum.photos/400/400?random=29', '图书馆', 1, 1, 164, 14, 'FIXED_PRICE', '考研,英语', NOW() - INTERVAL 18 DAY, NOW()),
('iPad 平板 9代', '128G 成色好，送保护套', 1450.00, 13, 2, 2,
 '["https://picsum.photos/400/400?random=30"]', 'https://picsum.photos/400/400?random=30', '学生宿舍', 1, 2, 480, 62, 'FIXED_PRICE', '苹果,iPad', NOW() - INTERVAL 18 DAY, NOW()),
('口红三支套装', '全新未拆', 60.00, 5, 2, 1,
 '["https://picsum.photos/400/400?random=31"]', 'https://picsum.photos/400/400?random=31', '学生宿舍', 1, 1, 78, 13, 'FIXED_PRICE', '美妆,口红', NOW() - INTERVAL 17 DAY, NOW()),
('折叠小书桌', '床上用，可调角度', 38.00, 33, 2, 1,
 '["https://picsum.photos/400/400?random=32"]', 'https://picsum.photos/400/400?random=32', '学生宿舍', 1, 1, 70, 6, 'FIXED_PRICE', '宿舍,书桌', NOW() - INTERVAL 16 DAY, NOW()),
('篮球 7号', '手感好，室内外通用', 45.00, 4, 2, 2,
 '["https://picsum.photos/400/400?random=33"]', 'https://picsum.photos/400/400?random=33', '体育馆', 1, 1, 130, 12, 'FIXED_PRICE', '运动,篮球', NOW() - INTERVAL 15 DAY, NOW()),
('C语言程序设计', '含上机实验册', 18.00, 21, 2, 3,
 '["https://picsum.photos/400/400?random=34"]', 'https://picsum.photos/400/400?random=34', '图书馆', 1, 1, 96, 9, 'FIXED_PRICE', '编程,教材', NOW() - INTERVAL 14 DAY, NOW()),
('宿舍灭蚊灯', '物理灭蚊，安静', 25.00, 33, 2, 1,
 '["https://picsum.photos/400/400?random=35"]', 'https://picsum.photos/400/400?random=35', '学生宿舍', 1, 1, 58, 4, 'FIXED_PRICE', '宿舍,日用品', NOW() - INTERVAL 13 DAY, NOW()),
('便携蓝牙音箱', '低音不错，防水防尘', 110.00, 14, 2, 2,
 '["https://picsum.photos/400/400?random=36"]', 'https://picsum.photos/400/400?random=36', '学生宿舍', 1, 1, 285, 30, 'FIXED_PRICE', '数码,音箱', NOW() - INTERVAL 12 DAY, NOW()),
('高数下册', '同专业教材，内页干净', 16.00, 21, 2, 2,
 '["https://picsum.photos/400/400?random=37"]', 'https://picsum.photos/400/400?random=37', '图书馆', 1, 1, 88, 7, 'FIXED_PRICE', '教材,高数', NOW() - INTERVAL 11 DAY, NOW()),
('瑜伽垫加厚', '含网袋，防滑', 32.00, 4, 2, 2,
 '["https://picsum.photos/400/400?random=38"]', 'https://picsum.photos/400/400?random=38', '体育馆', 1, 1, 76, 6, 'FIXED_PRICE', '运动,瑜伽', NOW() - INTERVAL 10 DAY, NOW()),
('考研政治精讲精练', '全新未使用', 35.00, 23, 2, 1,
 '["https://picsum.photos/400/400?random=39"]', 'https://picsum.photos/400/400?random=39', '图书馆', 1, 1, 118, 16, 'FIXED_PRICE', '考研,政治', NOW() - INTERVAL 9 DAY, NOW()),
('降噪头戴耳机', '音质优秀，可折叠', 320.00, 14, 2, 2,
 '["https://picsum.photos/400/400?random=40"]', 'https://picsum.photos/400/400?random=40', '学生宿舍', 1, 2, 388, 46, 'FIXED_PRICE', '数码,耳机', NOW() - INTERVAL 8 DAY, NOW()),
('移动电源 20000mAh', '双向快充，两入两出', 80.00, 13, 2, 2,
 '["https://picsum.photos/400/400?random=41"]', 'https://picsum.photos/400/400?random=41', '学生宿舍', 1, 1, 205, 19, 'FIXED_PRICE', '数码,充电', NOW() - INTERVAL 7 DAY, NOW()),
('《平凡的世界》', '经典三部曲', 28.00, 22, 2, 2,
 '["https://picsum.photos/400/400?random=42"]', 'https://picsum.photos/400/400?random=42', '图书馆', 1, 1, 142, 12, 'FIXED_PRICE', '小说,文学', NOW() - INTERVAL 6 DAY, NOW()),
('跳绳负重款', '计数精确，可调长度', 20.00, 4, 2, 1,
 '["https://picsum.photos/400/400?random=43"]', 'https://picsum.photos/400/400?random=43', '体育馆', 1, 1, 49, 5, 'FIXED_PRICE', '运动,跳绳', NOW() - INTERVAL 5 DAY, NOW()),
('日语学习指南', '新标日配套练习', 24.00, 21, 2, 3,
 '["https://picsum.photos/400/400?random=44"]', 'https://picsum.photos/400/400?random=44', '图书馆', 1, 2, 130, 11, 'FIXED_PRICE', '教材,日语', NOW() - INTERVAL 4 DAY, NOW()),
('智能手环表带', '全新多条换色', 12.00, 13, 2, 1,
 '["https://picsum.photos/400/400?random=45"]', 'https://picsum.photos/400/400?random=45', '学生宿舍', 1, 1, 33, 3, 'FIXED_PRICE', '数码,配件', NOW() - INTERVAL 3 DAY, NOW()),
('跑步鞋 42码', '穿着舒适，鞋底有轻微磨损', 65.00, 4, 2, 3,
 '["https://picsum.photos/400/400?random=46"]', 'https://picsum.photos/400/400?random=46', '体育馆', 1, 1, 88, 9, 'FIXED_PRICE', '运动,跑鞋', NOW() - INTERVAL 2 DAY, NOW()),
('宿舍收纳箱套装', '两个，可叠放', 30.00, 31, 2, 1,
 '["https://picsum.photos/400/400?random=47"]', 'https://picsum.photos/400/400?random=47', '学生宿舍', 1, 1, 42, 5, 'FIXED_PRICE', '宿舍,收纳', NOW() - INTERVAL 1 DAY, NOW()),
('毛笔字帖一套', '楷书入门', 15.00, 22, 2, 1,
 '["https://picsum.photos/400/400?random=48"]', 'https://picsum.photos/400/400?random=48', '图书馆', 1, 1, 55, 4, 'FIXED_PRICE', '文娱,字帖', NOW(), NOW());

SET FOREIGN_KEY_CHECKS = 1;