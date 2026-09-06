package com.campus.backend.service.impl;

import com.campus.backend.entity.UserLike;
import com.campus.backend.mapper.PostCommentMapper;
import com.campus.backend.mapper.PostMapper;
import com.campus.backend.mapper.ProductMapper;
import com.campus.backend.mapper.UserLikeMapper;
import com.campus.backend.service.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final UserLikeMapper userLikeMapper;
    private final PostMapper postMapper;
    private final PostCommentMapper postCommentMapper;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public boolean toggleLike(Long userId, String targetType, Long targetId) {
        boolean alreadyLiked = userLikeMapper.exists(userId, targetType, targetId);
        if (alreadyLiked) {
            userLikeMapper.delete(userId, targetType, targetId);
            decrementTargetLikeCount(targetType, targetId);
            log.info("取消点赞: userId={}, targetType={}, targetId={}", userId, targetType, targetId);
            return false;
        } else {
            UserLike userLike = new UserLike();
            userLike.setUserId(userId);
            userLike.setTargetType(targetType);
            userLike.setTargetId(targetId);
            userLikeMapper.insert(userLike);
            incrementTargetLikeCount(targetType, targetId);
            log.info("点赞: userId={}, targetType={}, targetId={}", userId, targetType, targetId);
            return true;
        }
    }

    @Override
    public boolean isLiked(Long userId, String targetType, Long targetId) {
        return userLikeMapper.exists(userId, targetType, targetId);
    }

    @Override
    public int getLikeCount(String targetType, Long targetId) {
        try {
            return switch (targetType) {
                case "POST" -> {
                    var post = postMapper.selectById(targetId);
                    yield post != null && post.getLikeCount() != null ? post.getLikeCount() : 0;
                }
                case "PRODUCT" -> {
                    var product = productMapper.selectById(targetId);
                    yield product != null && product.getLikeCount() != null ? product.getLikeCount() : 0;
                }
                case "COMMENT" -> {
                    var comment = postCommentMapper.selectById(targetId);
                    yield comment != null && comment.getLikeCount() != null ? comment.getLikeCount() : 0;
                }
                default -> 0;
            };
        } catch (Exception e) {
            return userLikeMapper.countByTarget(targetType, targetId);
        }
    }

    private void incrementTargetLikeCount(String targetType, Long targetId) {
        switch (targetType) {
            case "POST" -> postMapper.incrementLikeCount(targetId);
            case "COMMENT" -> postCommentMapper.incrementLikeCount(targetId);
            case "PRODUCT" -> productMapper.incrementLikeCount(targetId);
        }
    }

    private void decrementTargetLikeCount(String targetType, Long targetId) {
        switch (targetType) {
            case "POST" -> postMapper.decrementLikeCount(targetId);
            case "COMMENT" -> postCommentMapper.decrementLikeCount(targetId);
            case "PRODUCT" -> productMapper.decrementLikeCount(targetId);
        }
    }
}
