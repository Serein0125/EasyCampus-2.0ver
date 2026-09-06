package com.campus.backend.service;

/**
 * 收藏服务接口
 */
public interface FavoriteService {

    /** 收藏 */
    void addFavorite(Long userId, Long targetId, String targetType);

    /** 取消收藏 */
    void removeFavorite(Long userId, Long targetId, String targetType);

    /** 检查是否已收藏 */
    boolean isFavorited(Long userId, Long targetId, String targetType);

    /** 获取收藏数 */
    int countByProductId(Long productId);
}
