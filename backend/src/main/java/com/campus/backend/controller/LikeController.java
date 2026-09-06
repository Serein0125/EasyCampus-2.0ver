package com.campus.backend.controller;

import com.campus.backend.common.Result;
import com.campus.backend.common.SecurityUtils;
import com.campus.backend.entity.Post;
import com.campus.backend.entity.Product;
import com.campus.backend.mapper.PostMapper;
import com.campus.backend.mapper.ProductMapper;
import com.campus.backend.service.LikeService;
import com.campus.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v2/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;
    private final NotificationService notificationService;
    private final PostMapper postMapper;
    private final ProductMapper productMapper;

    @PostMapping("/toggle")
    public Result<Map<String, Object>> toggleLike(@RequestParam String targetType, @RequestParam Long targetId) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isLiked = likeService.toggleLike(userId, targetType, targetId);
        int likeCount = likeService.getLikeCount(targetType, targetId);

        // 点赞成功时发送通知给内容作者
        if (isLiked) {
            sendLikeNotification(userId, targetType, targetId);
        }

        return Result.success(Map.of("isLiked", isLiked, "likeCount", likeCount));
    }

    /** 点赞时通知内容作者 */
    private void sendLikeNotification(Long actorId, String targetType, Long targetId) {
        try {
            Long authorId = null;
            String content = "";
            if ("POST".equals(targetType)) {
                Post post = postMapper.selectById(targetId);
                if (post != null) {
                    authorId = post.getUserId();
                    content = "赞了你的帖子「" + truncate(post.getTitle(), 20) + "」";
                }
            } else if ("PRODUCT".equals(targetType)) {
                Product product = productMapper.selectById(targetId);
                if (product != null) {
                    authorId = product.getSellerId();
                    content = "赞了你的商品「" + truncate(product.getName(), 20) + "」";
                }
            }
            if (authorId != null) {
                notificationService.createNotification(authorId, actorId, "LIKE", targetId, targetType, content);
            }
        } catch (Exception e) {
            log.warn("发送点赞通知失败: {}", e.getMessage());
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    @GetMapping("/check")
    public Result<Map<String, Object>> checkLiked(@RequestParam String targetType, @RequestParam Long targetId) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        boolean isLiked = userId != null && likeService.isLiked(userId, targetType, targetId);
        int likeCount = likeService.getLikeCount(targetType, targetId);
        return Result.success(Map.of("isLiked", isLiked, "likeCount", likeCount));
    }

    @GetMapping("/count")
    public Result<Map<String, Object>> getLikeCount(@RequestParam String targetType, @RequestParam Long targetId) {
        int likeCount = likeService.getLikeCount(targetType, targetId);
        return Result.success(Map.of("likeCount", likeCount));
    }
}
