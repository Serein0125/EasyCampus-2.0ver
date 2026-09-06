package com.campus.backend.controller;

import com.campus.backend.common.Result;
import com.campus.backend.common.SecurityUtils;
import com.campus.backend.dto.*;
import com.campus.backend.entity.UserBehavior;
import com.campus.backend.entity.UserFollow;
import com.campus.backend.mapper.UserBehaviorMapper;
import com.campus.backend.mapper.UserFollowMapper;
import com.campus.backend.service.PostService;
import com.campus.backend.service.ProductService;
import com.campus.backend.service.impl.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v2/feed")
@RequiredArgsConstructor
public class FeedController {

    private final PostService postService;
    private final ProductService productService;
    private final UserFollowMapper userFollowMapper;
    private final UserBehaviorMapper userBehaviorMapper;
    private final RecommendationService recommendationService;

    @GetMapping("/recommend")
    public Result<Map<String, Object>> getRecommendations(@RequestParam(defaultValue = "6") int limit) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        if (userId == null) {
            return Result.success(Map.of("list", List.of(), "total", 0));
        }
        List<FeedItemVO> items = recommendationService.recommend(userId, limit);
        return Result.success(Map.of("list", items, "total", items.size()));
    }

    @PostMapping("/behavior")
    public Result<Void> recordBehavior(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        if (userId == null) return Result.success(null);

        String targetType = (String) body.get("targetType");
        Long targetId = body.get("targetId") != null ? Long.valueOf(body.get("targetId").toString()) : null;
        if (targetType == null || targetId == null) return Result.success(null);

        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setBehaviorType("VIEW");
        behavior.setTargetType(targetType);
        behavior.setTargetId(targetId);
        userBehaviorMapper.insert(behavior);
        return Result.success(null);
    }

    @GetMapping
    public Result<Map<String, Object>> getFeed(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long circleId,
            @RequestParam(required = false) String tag) {

        Long userId = SecurityUtils.getCurrentUserIdOrNull();

        // 限制最大页码，防止深度分页导致性能问题
        int MAX_PAGE = 50;
        if (page > MAX_PAGE) {
            return Result.success(Map.of("list", List.of(), "page", page, "size", size, "total", 0));
        }

        PostQueryDTO postQuery = new PostQueryDTO();
        postQuery.setPage(page);
        postQuery.setSize(size);

        ProductQueryDTO productQuery = new ProductQueryDTO();
        productQuery.setPage(page);
        productQuery.setSize(size);
        productQuery.setStatus(1);

        if (circleId != null) {
            postQuery.setBoardId(circleId);
            productQuery.setCategoryId(circleId.intValue());
        }

        if (tag != null && !tag.isEmpty()) {
            postQuery.setTag(tag);
            productQuery.setTag(tag);
        }

        if ("following".equals(type) && userId != null) {
            List<Long> followeeIds = getFolloweeIds(userId);
            if (followeeIds.isEmpty()) {
                // 关注列表为空时，fallback到发现模式，显示所有内容
                type = "discover";
            } else {
                postQuery.setUserIds(followeeIds);
                productQuery.setSellerIds(followeeIds);
            }
        } else if ("campus".equals(type) && userId != null) {
            // 校区模式已废弃，fallback到发现模式
            type = "discover";
        }

        // 分别查询非广告内容和广告内容
        postQuery.setIsAd(false);  // 排除广告帖子，广告单独查询插入
        int fetchSize = page * size;
        postQuery.setSize(fetchSize);
        productQuery.setSize(fetchSize);
        // 始终从第1页开始获取，在内存中做统一分页
        postQuery.setPage(1);
        productQuery.setPage(1);

        List<PostVO> posts = postService.getPostList(postQuery, userId);
        List<ProductVO> products = productService.getProductList(productQuery);

        // 构建非广告信息流列表，按创建时间降序排列
        List<FeedItemVO> nonAdItems = new ArrayList<>();
        for (PostVO post : posts) {
            FeedItemVO item = feedItemFromPost(post);
            nonAdItems.add(item);
        }
        for (ProductVO product : products) {
            FeedItemVO item = feedItemFromProduct(product);
            nonAdItems.add(item);
        }
        nonAdItems.sort((a, b) -> {
            long aTime = a.getCreatedAt() != null ? a.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) : 0;
            long bTime = b.getCreatedAt() != null ? b.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) : 0;
            return Long.compare(bTime, aTime);
        });

        // 查询活跃的广告帖子
        PostQueryDTO adQuery = new PostQueryDTO();
        adQuery.setIsAd(true);
        adQuery.setPage(1);
        adQuery.setSize(50);
        if (circleId != null) adQuery.setBoardId(circleId);
        if (tag != null && !tag.isEmpty()) adQuery.setTag(tag);
        // 关注模式：仅展示关注的用户发布的广告
        if ("following".equals(type) && userId != null) {
            List<Long> followeeIds = getFolloweeIds(userId);
            if (!followeeIds.isEmpty()) adQuery.setUserIds(followeeIds);
        }
        List<PostVO> adPosts = postService.getPostList(adQuery, userId);

        // 按固定间隔插入广告到信息流
        List<FeedItemVO> items = insertAdsIntoFeed(nonAdItems, adPosts);

        // 按 itemType+id 去重
        List<FeedItemVO> deduped = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (FeedItemVO item : items) {
            String key = item.getItemType() + ":" + item.getId();
            if (seen.add(key)) {
                deduped.add(item);
            }
        }

        // 获取总数（非广告项 + 广告项）
        int postTotal = postService.getPostCount(postQuery);
        int productTotal = productService.getProductCount(productQuery);
        int total = postTotal + productTotal;

        // 分页截取
        int fromIndex = Math.min((page - 1) * size, deduped.size());
        int toIndex = Math.min(page * size, deduped.size());
        List<FeedItemVO> pageItems = deduped.subList(fromIndex, toIndex);

        Map<String, Object> result = Map.of(
                "list", pageItems,
                "page", page,
                "size", size,
                "total", total
        );
        return Result.success(result);
    }

    /** 将 PostVO 转为 FeedItemVO */
    private FeedItemVO feedItemFromPost(PostVO post) {
        FeedItemVO item = new FeedItemVO();
        item.setItemType("POST");
        item.setId(post.getId());
        item.setTitle(post.getTitle());
        item.setContent(post.getContent());
        item.setLikeCount(post.getLikeCount());
        item.setCommentCount(post.getCommentCount());
        item.setViewCount(post.getViewCount());
        item.setIsLiked(post.getIsLiked());
        item.setUserId(post.getUserId());
        item.setUserName(post.getUserName());
        item.setUserAvatar(post.getUserAvatar());
        item.setBoardId(post.getBoardId());
        item.setBoardName(post.getBoardName());
        item.setPostType(post.getPostType());
        item.setLocation(post.getLocation());
        item.setCreatedAt(post.getCreatedAt());
        item.setStartTime(post.getStartTime());
        item.setIsAd(post.getIsAd());
        item.setExposureBoost(post.getExposureBoost());
        item.setTags(post.getTags());
        item.setCoverImage(post.getCoverImage());
        return item;
    }

    /** 将 ProductVO 转为 FeedItemVO */
    private FeedItemVO feedItemFromProduct(ProductVO product) {
        FeedItemVO item = new FeedItemVO();
        item.setItemType("PRODUCT");
        item.setId(product.getId());
        item.setTitle(product.getName());
        item.setContent(product.getDescription());
        item.setCoverImage(product.getCoverImage());
        item.setPrice(product.getPrice() != null ? product.getPrice().toString() : null);
        item.setOriginalPrice(product.getOriginalPrice() != null ? product.getOriginalPrice().toString() : null);
        item.setLikeCount(product.getLikeCount());
        item.setViewCount(product.getViewCount());
        item.setIsLiked(product.getIsFavorited());
        item.setUserId(product.getSellerId());
        item.setUserName(product.getSellerName());
        item.setUserAvatar(product.getSellerAvatar());
        item.setCategoryId(product.getCategoryId());
        item.setCategoryName(product.getCategoryName());
        item.setHasStory(product.getHasStory());
        item.setConditionText(product.getConditionText());
        item.setLocation(product.getLocation());
        item.setCreatedAt(product.getCreatedAt());
        item.setTags(product.getTags());
        return item;
    }

    /** 根据 exposureBoost 推断广告套餐间隔（普通信息流使用） */
    private int getAdInterval(Integer exposureBoost) {
        if (exposureBoost == null) return 45;
        switch (exposureBoost) {
            case 10: return 12;  // Premium
            case 5:  return 18;  // Standard
            case 3:  return 30;  // Basic
            case 2:  return 45;  // Trial
            default: return 45;
        }
    }

    /** 按固定间隔将广告插入到非广告信息流中（方案A：各自独立计数） */
    private List<FeedItemVO> insertAdsIntoFeed(List<FeedItemVO> nonAdItems, List<PostVO> adPosts) {
        if (adPosts.isEmpty()) return new ArrayList<>(nonAdItems);

        // 广告先转为 FeedItemVO，并按 interval 从小到大排序（高优先级在前）
        List<FeedItemVO> ads = new ArrayList<>();
        for (PostVO vo : adPosts) {
            ads.add(feedItemFromPost(vo));
        }
        ads.sort((a, b) -> {
            int intervalA = getAdInterval(a.getExposureBoost());
            int intervalB = getAdInterval(b.getExposureBoost());
            return Integer.compare(intervalA, intervalB);
        });

        List<FeedItemVO> result = new ArrayList<>();
        // 每个广告独立的插入计数器
        Map<Long, Integer> adCounters = new HashMap<>();
        for (FeedItemVO ad : ads) {
            adCounters.put(ad.getId(), 0);
        }

        int nonAdIndex = 0;
        for (FeedItemVO item : nonAdItems) {
            // 在插入每个非广告内容前，检查是否有广告需要在此位置插入
            for (FeedItemVO ad : ads) {
                int interval = getAdInterval(ad.getExposureBoost());
                int counter = adCounters.get(ad.getId());
                // 独立计数：广告在第 interval, 2*interval, 3*interval... 条非广告内容后插入
                if (nonAdIndex > 0 && nonAdIndex % interval == 0 && counter < nonAdIndex / interval) {
                    result.add(ad);
                    adCounters.put(ad.getId(), nonAdIndex / interval);
                }
            }
            result.add(item);
            nonAdIndex++;
        }

        return result;
    }

    private List<Long> getFolloweeIds(Long userId) {
        List<UserFollow> follows = userFollowMapper.selectFollowing(userId, 0, 200);
        return follows.stream()
                .map(UserFollow::getFolloweeId)
                .collect(Collectors.toList());
    }
}
