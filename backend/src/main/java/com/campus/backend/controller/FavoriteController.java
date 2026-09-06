package com.campus.backend.controller;

import com.campus.backend.common.Result;
import com.campus.backend.common.SecurityUtils;
import com.campus.backend.entity.Favorite;
import com.campus.backend.entity.Post;
import com.campus.backend.entity.Product;
import com.campus.backend.mapper.FavoriteMapper;
import com.campus.backend.mapper.PostMapper;
import com.campus.backend.mapper.ProductMapper;
import com.campus.backend.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/favorites")
@Tag(name = "收藏管理", description = "商品和帖子收藏功能")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final FavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;
    private final PostMapper postMapper;

    /** 收藏 */
    @PostMapping("/{targetId}")
    @Operation(summary = "收藏（商品或帖子）")
    public Result<Void> addFavorite(@PathVariable Long targetId,
                                     @RequestParam(defaultValue = "PRODUCT") String targetType) {
        favoriteService.addFavorite(SecurityUtils.getCurrentUserId(), targetId, targetType);
        return Result.success("收藏成功");
    }

    /** 取消收藏 */
    @DeleteMapping("/{targetId}")
    @Operation(summary = "取消收藏（商品或帖子）")
    public Result<Void> removeFavorite(@PathVariable Long targetId,
                                        @RequestParam(defaultValue = "PRODUCT") String targetType) {
        favoriteService.removeFavorite(SecurityUtils.getCurrentUserId(), targetId, targetType);
        return Result.success("已取消收藏");
    }

    /** 检查是否已收藏 */
    @GetMapping("/{targetId}/check")
    @Operation(summary = "检查是否已收藏")
    public Result<Boolean> checkFavorited(@PathVariable Long targetId,
                                           @RequestParam(defaultValue = "PRODUCT") String targetType) {
        return Result.success(favoriteService.isFavorited(SecurityUtils.getCurrentUserId(), targetId, targetType));
    }

    /** 我的收藏列表（按 targetType 筛选） */
    @GetMapping
    @Operation(summary = "我的收藏列表")
    public Result<Map<String, Object>> getMyFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "PRODUCT") String targetType) {
        Long userId = SecurityUtils.getCurrentUserId();
        int offset = (page - 1) * size;

        List<Favorite> favorites = favoriteMapper.selectByUserIdAndType(userId, targetType, offset, size);
        int total = favoriteMapper.countByUserIdAndType(userId, targetType);

        List<Map<String, Object>> items = new ArrayList<>();
        if ("POST".equals(targetType)) {
            for (Favorite fav : favorites) {
                try {
                    Post post = postMapper.selectById(fav.getProductId());
                    if (post != null) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", post.getId());
                        item.put("title", post.getTitle());
                        item.put("content", post.getContent());
                        item.put("postType", post.getPostType());
                        item.put("viewCount", post.getViewCount());
                        item.put("likeCount", post.getLikeCount());
                        item.put("commentCount", post.getCommentCount());
                        item.put("tags", post.getTags());
                        item.put("createdAt", post.getCreatedAt());
                        item.put("favoritedAt", fav.getCreatedAt());
                        item.put("targetType", "POST");
                        items.add(item);
                    }
                } catch (Exception ignored) {}
            }
        } else {
            for (Favorite fav : favorites) {
                try {
                    Product product = productMapper.selectById(fav.getProductId());
                    if (product != null) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", product.getId());
                        item.put("name", product.getName());
                        item.put("price", product.getPrice());
                        item.put("originalPrice", product.getOriginalPrice());
                        item.put("coverImage", product.getCoverImage());
                        item.put("conditionLevel", product.getConditionLevel());
                        item.put("status", product.getStatus());
                        item.put("categoryId", product.getCategoryId());
                        item.put("location", product.getLocation());
                        item.put("createdAt", product.getCreatedAt());
                        item.put("favoritedAt", fav.getCreatedAt());
                        item.put("targetType", "PRODUCT");
                        items.add(item);
                    }
                } catch (Exception ignored) {}
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", items);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("targetType", targetType);
        return Result.success(result);
    }

    /** 我的收藏数量 */
    @GetMapping("/count")
    @Operation(summary = "我的收藏数量")
    public Result<Integer> getMyFavoriteCount() {
        return Result.success(favoriteMapper.countByUserId(SecurityUtils.getCurrentUserId()));
    }

    /** 我的收藏分类统计（商品 + 帖子） */
    @GetMapping("/counts")
    @Operation(summary = "我的收藏分类统计")
    public Result<Map<String, Integer>> getFavoriteCounts() {
        Long userId = SecurityUtils.getCurrentUserId();
        Map<String, Integer> counts = new HashMap<>();
        counts.put("productCount", favoriteMapper.countByUserIdAndType(userId, "PRODUCT"));
        counts.put("postCount", favoriteMapper.countByUserIdAndType(userId, "POST"));
        return Result.success(counts);
    }
}
