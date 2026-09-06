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
@RequestMapping("/api/v2/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public Result<PostVO> createPost(@Valid @RequestBody PostCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        PostVO post = postService.createPost(dto, userId);
        return Result.success(post);
    }

    @PutMapping("/{id}")
    public Result<PostVO> updatePost(@PathVariable Long id, @Valid @RequestBody PostCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        PostVO post = postService.updatePost(id, dto, userId);
        return Result.success(post);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        postService.deletePost(id, userId);
        return Result.success(null);
    }

    @GetMapping("/{id}")
    public Result<PostVO> getPostDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        PostVO post = postService.getPostDetail(id, userId);
        return Result.success(post);
    }

    @GetMapping
    public Result<Map<String, Object>> getPostList(PostQueryDTO query) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        List<PostVO> list = postService.getPostList(query, userId);
        int total = postService.getPostCount(query);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", query.getPage());
        result.put("size", query.getSize());
        return Result.success(result);
    }

    @GetMapping("/user/{userId}")
    public Result<List<PostVO>> getUserPosts(@PathVariable Long userId) {
        List<PostVO> posts = postService.getUserPosts(userId);
        return Result.success(posts);
    }

    @PutMapping("/{id}/pin")
    public Result<Void> togglePin(@PathVariable Long id, @RequestParam Boolean isPinned) {
        Long userId = SecurityUtils.getCurrentUserId();
        postService.togglePin(id, userId, isPinned);
        return Result.success(null);
    }

    @PutMapping("/{id}/essence")
    public Result<Void> toggleEssence(@PathVariable Long id, @RequestParam Boolean isEssence) {
        postService.toggleEssence(id, isEssence);
        return Result.success(null);
    }
}
