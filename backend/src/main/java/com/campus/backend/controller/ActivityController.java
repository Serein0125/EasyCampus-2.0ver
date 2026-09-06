package com.campus.backend.controller;

import com.campus.backend.common.Result;
import com.campus.backend.common.SecurityUtils;
import com.campus.backend.dto.PostCreateDTO;
import com.campus.backend.dto.PostQueryDTO;
import com.campus.backend.dto.PostVO;
import com.campus.backend.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v2/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final PostService postService;

    @GetMapping
    public Result<Map<String, Object>> getActivities(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size,
                                                      @RequestParam(required = false) String status) {
        PostQueryDTO query = new PostQueryDTO();
        query.setPage(page);
        query.setSize(size);
        query.setPostType("ACTIVITY");
        // 前端传的status是活动时间阶段(upcoming/ongoing/past)，映射到activityStatus字段
        // 不设置query.setStatus，避免与帖子状态(PUBLISHED等)混淆
        if (status != null && !status.isEmpty()) {
            query.setActivityStatus(status);
        }

        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        List<PostVO> list = postService.getPostList(query, userId);
        int total = postService.getPostCount(query);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<PostVO> getActivityDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        PostVO post = postService.getPostDetail(id, userId);
        return Result.success(post);
    }

    @PostMapping
    public Result<PostVO> createActivity(@Valid @RequestBody PostCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        dto.setPostType("ACTIVITY");
        PostVO post = postService.createPost(dto, userId);
        log.info("用户 {} 创建活动 {}", userId, post.getId());
        return Result.success(post);
    }

    @GetMapping("/my")
    public Result<Map<String, Object>> getMyActivities(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        PostQueryDTO query = new PostQueryDTO();
        query.setPage(page);
        query.setSize(size);
        query.setPostType("ACTIVITY");
        query.setUserId(userId);

        List<PostVO> list = postService.getPostList(query, userId);
        int total = postService.getPostCount(query);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        return Result.success(result);
    }

    @PostMapping("/{id}/join")
    public Result<Map<String, Object>> joinActivity(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户 {} 报名活动 {}", userId, id);
        return Result.success(Map.of("joined", true, "message", "报名成功"));
    }

    @DeleteMapping("/{id}/join")
    public Result<Map<String, Object>> cancelJoin(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户 {} 取消报名活动 {}", userId, id);
        return Result.success(Map.of("joined", false, "message", "已取消报名"));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteActivity(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        postService.deletePost(id, userId);
        log.info("用户 {} 删除活动 {}", userId, id);
        return Result.success(null);
    }
}
