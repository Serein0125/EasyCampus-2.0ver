package com.campus.backend.service.impl;

import com.campus.backend.entity.Favorite;
import com.campus.backend.mapper.FavoriteMapper;
import com.campus.backend.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 收藏服务实现（支持商品和帖子）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;

    @Override
    @Transactional
    public void addFavorite(Long userId, Long targetId, String targetType) {
        if (!favoriteMapper.existsByTarget(userId, targetId, targetType)) {
            Favorite fav = new Favorite();
            fav.setUserId(userId);
            fav.setProductId(targetId);
            fav.setTargetType(targetType);
            favoriteMapper.insert(fav);
            log.info("收藏成功: userId={}, targetId={}, targetType={}", userId, targetId, targetType);
        }
    }

    @Override
    @Transactional
    public void removeFavorite(Long userId, Long targetId, String targetType) {
        int rows = favoriteMapper.deleteByTarget(userId, targetId, targetType);
        if (rows > 0) {
            log.info("取消收藏: userId={}, targetId={}, targetType={}", userId, targetId, targetType);
        }
    }

    @Override
    public boolean isFavorited(Long userId, Long targetId, String targetType) {
        return favoriteMapper.existsByTarget(userId, targetId, targetType);
    }

    @Override
    public int countByProductId(Long productId) {
        return favoriteMapper.countByProductId(productId);
    }
}
