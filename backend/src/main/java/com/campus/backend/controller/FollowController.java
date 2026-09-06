package com.campus.backend.controller;

import com.campus.backend.common.Result;
import com.campus.backend.common.SecurityUtils;
import com.campus.backend.dto.UserVO;
import com.campus.backend.service.FollowService;
import com.campus.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v2/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;
    private final NotificationService notificationService;

    @PostMapping("/toggle")
    public Result<Map<String, Object>> toggleFollow(@RequestParam Long followeeId) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isFollowing = followService.toggleFollow(userId, followeeId);

        // 关注成功时发送通知
        if (isFollowing) {
            try {
                notificationService.createNotification(followeeId, userId, "FOLLOW", userId, "USER", "关注了你");
            } catch (Exception e) {
                log.warn("发送关注通知失败: {}", e.getMessage());
            }
        }

        return Result.success(Map.of("isFollowing", isFollowing));
    }

    @GetMapping("/check")
    public Result<Map<String, Object>> checkFollowing(@RequestParam Long followeeId) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        boolean isFollowing = userId != null && followService.isFollowing(userId, followeeId);
        return Result.success(Map.of("isFollowing", isFollowing));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getFollowStats(@RequestParam Long userId) {
        int followingCount = followService.getFollowingCount(userId);
        int followerCount = followService.getFollowerCount(userId);
        return Result.success(Map.of("followingCount", followingCount, "followerCount", followerCount));
    }

    @GetMapping("/following")
    public Result<List<UserVO>> getFollowingList(@RequestParam(required = false) Long userId,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        if (userId == null) userId = SecurityUtils.getCurrentUserId();
        int offset = (page - 1) * size;
        List<UserVO> list = followService.getFollowingList(userId, offset, size);
        return Result.success(list);
    }

    @GetMapping("/followers")
    public Result<List<UserVO>> getFollowerList(@RequestParam(required = false) Long userId,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        if (userId == null) userId = SecurityUtils.getCurrentUserId();
        int offset = (page - 1) * size;
        List<UserVO> list = followService.getFollowerList(userId, offset, size);
        return Result.success(list);
    }
}
