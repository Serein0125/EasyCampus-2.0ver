package com.campus.backend.service;

public interface LikeService {

    boolean toggleLike(Long userId, String targetType, Long targetId);

    boolean isLiked(Long userId, String targetType, Long targetId);

    int getLikeCount(String targetType, Long targetId);
}
