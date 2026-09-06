package com.campus.backend.common.cache;

public class CacheConstants {
    
    // 缓存名称常量
    public static final String CACHE_NAME_USER = "user";
    public static final String CACHE_NAME_PRODUCT = "product";
    public static final String CACHE_NAME_CATEGORY = "category";
    public static final String CACHE_NAME_MESSAGE = "message";
    
    // 缓存键前缀
    public static final String KEY_PREFIX_USER = "user:";
    public static final String KEY_PREFIX_PRODUCT = "product:";
    public static final String KEY_PREFIX_CATEGORY = "category:";
    public static final String KEY_PREFIX_MESSAGE = "message:";
    
    // 缓存过期时间（秒）
    public static final long TTL_USER = 1800; // 30分钟
    public static final long TTL_PRODUCT = 900; // 15分钟
    public static final long TTL_CATEGORY = 3600; // 1小时
    public static final long TTL_MESSAGE = 300; // 5分钟
    
    // 分布式锁前缀
    public static final String LOCK_PREFIX_USER = "lock:user:";
    public static final String LOCK_PREFIX_PRODUCT = "lock:product:";
    
    // 缓存统计前缀
    public static final String STATS_PREFIX = "stats:";
    
    // 热点数据缓存
    public static final String HOT_PRODUCTS = "hot:products";
    public static final String HOT_CATEGORIES = "hot:categories";
    public static final String HOT_USERS = "hot:users";
    
    // 缓存工具方法
    public static String getUserKey(Long userId) {
        return KEY_PREFIX_USER + userId;
    }
    
    public static String getUserKey(String username) {
        return KEY_PREFIX_USER + "username:" + username;
    }
    
    public static String getProductKey(Long productId) {
        return KEY_PREFIX_PRODUCT + productId;
    }
    
    public static String getCategoryKey(Long categoryId) {
        return KEY_PREFIX_CATEGORY + categoryId;
    }
    
    public static String getMessageKey(Long messageId) {
        return KEY_PREFIX_MESSAGE + messageId;
    }
    
    public static String getLockKey(String prefix, Long id) {
        return prefix + id;
    }
    
    public static String getStatsKey(String type) {
        return STATS_PREFIX + type;
    }
}