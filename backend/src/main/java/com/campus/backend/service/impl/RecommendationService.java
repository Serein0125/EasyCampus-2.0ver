package com.campus.backend.service.impl;

import com.campus.backend.dto.FeedItemVO;
import com.campus.backend.dto.PostVO;
import com.campus.backend.dto.PostQueryDTO;
import com.campus.backend.dto.ProductVO;
import com.campus.backend.dto.ProductQueryDTO;
import com.campus.backend.entity.User;
import com.campus.backend.entity.Post;
import com.campus.backend.entity.Product;
import com.campus.backend.entity.UserBehavior;
import com.campus.backend.mapper.PostMapper;
import com.campus.backend.mapper.ProductMapper;
import com.campus.backend.mapper.UserBehaviorMapper;
import com.campus.backend.mapper.UserLikeMapper;
import com.campus.backend.mapper.UserMapper;
import com.campus.backend.service.PostService;
import com.campus.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐流服务。
 * 基于用户兴趣标签进行内容匹配打分，采用三维度加权（标签匹配 + 内容质量 + 时效性），
 * 对符合条件的广告按固定间隔插入推荐流。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserBehaviorMapper userBehaviorMapper;
    private final PostMapper postMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final UserLikeMapper userLikeMapper;
    private final PostService postService;
    private final ProductService productService;

    /** 推荐曝光的用户行为类型标识 */
    private static final String BEHAVIOR_IMPRESSION = "IMPRESSION";
    /** 推荐流去重窗口（小时） */
    private static final int DEDUP_HOURS = 24;
    /** 推荐流去重窗口内最多记录条数 */
    private static final int DEDUP_LIMIT = 50;
    /** 广告频次控制：24小时内同一广告对同一用户最多展示次数 */
    private static final int AD_MAX_IMPRESSIONS = 3;
    /** 候选池最大数量 */
    private static final int CANDIDATE_POOL_SIZE = 200;
    /** 时效性衰减系数：freshnessScore = 1/(1 + daysSinceCreation * DECAY) */
    private static final double FRESHNESS_DECAY = 0.15;

    /** 打分权重 */
    private static final double WEIGHT_TAG = 0.5;
    private static final double WEIGHT_QUALITY = 0.3;
    private static final double WEIGHT_FRESHNESS = 0.2;

    /** 质量分内部权重 */
    private static final double QW_LIKE = 0.5;
    private static final double QW_COMMENT = 0.3;
    private static final double QW_VIEW = 0.2;

    /**
     * 根据用户浏览历史生成个性化推荐。
     * 无浏览历史的用户回退到热门排序。
     */
    public List<FeedItemVO> recommend(Long userId, int limit) {
        // 1. 提取用户兴趣标签
        Set<String> interestTags = extractInterestTags(userId);

        // 2. 新用户（无浏览历史）回退到热门排序
        if (interestTags.isEmpty()) {
            return getHotItems(userId, limit);
        }

        // 3. 查询去重数据：最近24小时内已展示过的内容ID
        LocalDateTime dedupSince = LocalDateTime.now().minusHours(DEDUP_HOURS);
        Set<Long> impressedPostIds = new HashSet<>(userBehaviorMapper.selectRecentImpressions(
                userId, BEHAVIOR_IMPRESSION, "POST", dedupSince, DEDUP_LIMIT));
        List<Long> recentBrowsePostIds = userBehaviorMapper.selectRecentTargetIds(userId, "POST", 10);

        // 4. 拉取候选池：最近30天的帖子（非广告） + 在售商品
        List<Post> candidatePosts = fetchCandidatePosts();
        List<Product> candidateProducts = fetchCandidateProducts();

        // 5. 计算各候选品的质量分最大值（用于归一化）
        int maxLikeCount = 0, maxCommentCount = 0, maxViewCount = 0;
        for (Post p : candidatePosts) {
            maxLikeCount = Math.max(maxLikeCount, getInt(p.getLikeCount()));
            maxCommentCount = Math.max(maxCommentCount, getInt(p.getCommentCount()));
            maxViewCount = Math.max(maxViewCount, getInt(p.getViewCount()));
        }
        for (Product p : candidateProducts) {
            maxLikeCount = Math.max(maxLikeCount, getInt(p.getLikeCount()));
            // 商品没有 commentCount
            maxViewCount = Math.max(maxViewCount, getInt(p.getViewCount()));
        }
        // 确保分母不为0
        maxLikeCount = Math.max(maxLikeCount, 1);
        maxCommentCount = Math.max(maxCommentCount, 1);
        maxViewCount = Math.max(maxViewCount, 1);

        LocalDateTime now = LocalDateTime.now();

        // 6. 对帖子和商品分别打分，过滤去重
        List<ScoredItem> scoredItems = new ArrayList<>();

        for (Post post : candidatePosts) {
            // 过滤已浏览和已展示的内容
            if (recentBrowsePostIds.contains(post.getId())) continue;
            if (impressedPostIds.contains(post.getId())) continue;

            double score = calcScore(post.getTags(), interestTags,
                    post.getLikeCount(), post.getCommentCount(), post.getViewCount(),
                    post.getCreatedAt(), now,
                    maxLikeCount, maxCommentCount, maxViewCount);
            if (score > 0) {
                scoredItems.add(new ScoredItem("POST", post.getId(), score));
            }
        }

        // 收集最近浏览的商品ID用于过滤
        List<Long> recentProductIds = userBehaviorMapper.selectRecentTargetIds(userId, "PRODUCT", 10);

        for (Product product : candidateProducts) {
            if (recentProductIds.contains(product.getId())) continue;

            double score = calcScore(product.getTags(), interestTags,
                    product.getLikeCount(), 0, product.getViewCount(),
                    product.getCreatedAt(), now,
                    maxLikeCount, maxCommentCount, maxViewCount);
            if (score > 0) {
                scoredItems.add(new ScoredItem("PRODUCT", product.getId(), score));
            }
        }

        // 7. 按分数降序排列
        scoredItems.sort((a, b) -> Double.compare(b.score, a.score));

        // 8. 获取符合条件的广告帖子（hasRecommendation 的 Standard/Premium，且未超频控）
        List<Post> adPosts = fetchRecommendationAdPosts(userId, dedupSince);

        // 9. 构建结果：取 top N 条非广告内容，按固定间隔插入广告
        List<ScoredItem> topNonAdItems = new ArrayList<>();
        for (ScoredItem si : scoredItems) {
            if (topNonAdItems.size() >= limit) break;
            topNonAdItems.add(si);
        }

        // 按间隔插入广告
        List<ScoredItem> mergedItems = insertAdsByInterval(topNonAdItems, adPosts);

        // 10. 批量查询实体，组装 FeedItemVO
        return buildFeedItems(mergedItems, userId);
    }

    /** 提取用户兴趣标签：从最近浏览的帖子和商品中收集标签 */
    private Set<String> extractInterestTags(Long userId) {
        Set<String> tags = new LinkedHashSet<>();

        List<Long> recentPostIds = userBehaviorMapper.selectRecentTargetIds(userId, "POST", 10);
        if (!recentPostIds.isEmpty()) {
            for (Post post : postMapper.selectByIds(recentPostIds)) {
                if (post.getTags() != null) {
                    for (String tag : post.getTags().split(",")) {
                        String t = tag.trim();
                        if (!t.isEmpty()) tags.add(t);
                    }
                }
            }
        }

        List<Long> recentProductIds = userBehaviorMapper.selectRecentTargetIds(userId, "PRODUCT", 10);
        if (!recentProductIds.isEmpty()) {
            for (Product product : productMapper.selectByIds(recentProductIds)) {
                if (product.getTags() != null) {
                    for (String tag : product.getTags().split(",")) {
                        String t = tag.trim();
                        if (!t.isEmpty()) tags.add(t);
                    }
                }
            }
        }

        return tags;
    }

    /** 拉取非广告帖子候选池：最近30天，200条 */
    private List<Post> fetchCandidatePosts() {
        PostQueryDTO query = new PostQueryDTO();
        query.setPage(1);
        query.setSize(CANDIDATE_POOL_SIZE);
        // isAd 不设置 = 不过滤，后续在打分阶段过滤掉广告帖子
        List<PostVO> posts = postService.getPostList(query, null);
        return posts.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsAd()))  // 排除广告，广告单独处理
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .map(this::voToEntity)
                .collect(Collectors.toList());
    }

    /** 拉取商品候选池：在售状态，200条 */
    private List<Product> fetchCandidateProducts() {
        ProductQueryDTO query = new ProductQueryDTO();
        query.setPage(1);
        query.setSize(CANDIDATE_POOL_SIZE);
        query.setStatus(1);
        List<ProductVO> products = productService.getProductList(query);
        return products.stream()
                .map(this::voToEntity)
                .collect(Collectors.toList());
    }

    /** 三维度加权综合打分 */
    private double calcScore(String itemTags, Set<String> interestTags,
                             Integer likeCount, Integer commentCount, Integer viewCount,
                             LocalDateTime createdAt, LocalDateTime now,
                             int maxLikeCount, int maxCommentCount, int maxViewCount) {

        // 标签匹配分：匹配标签数 / 内容标签总数
        double tagScore = 0;
        if (itemTags != null && !itemTags.isEmpty()) {
            String[] tags = itemTags.split(",");
            int matchCount = 0;
            for (String tag : tags) {
                if (interestTags.contains(tag.trim())) matchCount++;
            }
            tagScore = (double) matchCount / tags.length;
        }

        // 内容质量分：对数归一化后的点赞/评论/浏览加权
        double likeScore = normalizeLog(getInt(likeCount), maxLikeCount);
        double commentScore = normalizeLog(getInt(commentCount), maxCommentCount);
        double viewScore = normalizeLog(getInt(viewCount), maxViewCount);
        double qualityScore = likeScore * QW_LIKE + commentScore * QW_COMMENT + viewScore * QW_VIEW;

        // 时效性分：随天数衰减
        double freshnessScore = 0;
        if (createdAt != null) {
            long daysSince = java.time.Duration.between(createdAt, now).toDays();
            freshnessScore = 1.0 / (1.0 + daysSince * FRESHNESS_DECAY);
        }

        return tagScore * WEIGHT_TAG + qualityScore * WEIGHT_QUALITY + freshnessScore * WEIGHT_FRESHNESS;
    }

    /** 对数归一化：log(1+value) / log(1+maxValue)，避免极端值主导 */
    private double normalizeLog(int value, int maxValue) {
        if (value <= 0) return 0;
        if (maxValue <= 1) return 1;
        return Math.log1p(value) / Math.log1p(maxValue);
    }

    /** 获取推荐流广告帖子（Standard/Premium 套餐、通过频控） */
    private List<Post> fetchRecommendationAdPosts(Long userId, LocalDateTime dedupSince) {
        PostQueryDTO query = new PostQueryDTO();
        query.setIsAd(true);
        query.setPage(1);
        query.setSize(50);
        List<PostVO> adVOs = postService.getPostList(query, userId);

        List<Post> eligibleAds = new ArrayList<>();
        for (PostVO vo : adVOs) {
            Post post = voToEntity(vo);
            // 仅 Standard(5) 和 Premium(10) 进入推荐流
            int boost = post.getExposureBoost() != null ? post.getExposureBoost() : 1;
            if (boost != 5 && boost != 10) continue;

            // 频控：24小时内同用户最多展示3次
            int impressionCount = userBehaviorMapper.countAdImpressions(
                    userId, BEHAVIOR_IMPRESSION, post.getId(), dedupSince);
            if (impressionCount >= AD_MAX_IMPRESSIONS) continue;

            eligibleAds.add(post);
        }

        // 按 interval 排序（高优先级套餐先插入）
        eligibleAds.sort((a, b) -> {
            int intervalA = getAdInterval(a.getExposureBoost());
            int intervalB = getAdInterval(b.getExposureBoost());
            return Integer.compare(intervalA, intervalB);
        });

        return eligibleAds;
    }

    /** 根据 exposureBoost 推断套餐间隔 */
    private int getAdInterval(Integer exposureBoost) {
        if (exposureBoost == null) return 45;
        switch (exposureBoost) {
            case 10: return 12;  // Premium
            case 5:  return 18;  // Standard
            case 3:  return 30;  // Basic（不上推荐流，仅兜底）
            case 2:  return 45;  // Trial（不上推荐流，仅兜底）
            default: return 45;
        }
    }

    /** 按固定间隔将广告插入到非广告内容序列中（方案A：各自独立计数） */
    private List<ScoredItem> insertAdsByInterval(List<ScoredItem> nonAdItems, List<Post> adPosts) {
        if (adPosts.isEmpty()) return new ArrayList<>(nonAdItems);

        List<ScoredItem> result = new ArrayList<>();
        // 每个广告独立的插入计数器
        Map<Long, Integer> adCounters = new HashMap<>();
        for (Post ad : adPosts) {
            adCounters.put(ad.getId(), 0);
        }

        int nonAdIndex = 0;
        for (ScoredItem item : nonAdItems) {
            // 在插入每个非广告内容前，检查是否有广告需要在此位置插入
            for (Post ad : adPosts) {
                int interval = getAdInterval(ad.getExposureBoost());
                int counter = adCounters.get(ad.getId());
                // 独立计数：广告在第 interval, 2*interval, 3*interval... 条非广告内容后插入
                if (nonAdIndex > 0 && nonAdIndex % interval == 0 && counter < nonAdIndex / interval) {
                    result.add(new ScoredItem("AD", ad.getId(), -interval)); // 负数score用于排序优先级
                    adCounters.put(ad.getId(), nonAdIndex / interval);
                }
            }
            result.add(item);
            nonAdIndex++;
        }

        return result;
    }

    /** 根据打分的 item 列表，批量查询实体并组装为 FeedItemVO */
    private List<FeedItemVO> buildFeedItems(List<ScoredItem> scoredItems, Long userId) {
        // 收集需要查询的ID
        List<Long> postIds = new ArrayList<>();
        List<Long> productIds = new ArrayList<>();
        for (ScoredItem si : scoredItems) {
            if ("POST".equals(si.itemType) || "AD".equals(si.itemType)) {
                postIds.add(si.id);
            } else if ("PRODUCT".equals(si.itemType)) {
                productIds.add(si.id);
            }
        }

        // 批量查询帖子实体
        Map<Long, Post> postMap = new HashMap<>();
        if (!postIds.isEmpty()) {
            for (Post p : postMapper.selectByIds(postIds)) {
                postMap.put(p.getId(), p);
            }
        }

        // 批量查询商品实体
        Map<Long, Product> productMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (Product p : productMapper.selectByIds(productIds)) {
                if (p.getStatus() != null && p.getStatus() == 1) {
                    productMap.put(p.getId(), p);
                }
            }
        }

        // 收集所有需要的用户ID，批量查询
        Set<Long> userIds = new HashSet<>();
        for (Post p : postMap.values()) userIds.add(p.getUserId());
        for (Product p : productMap.values()) userIds.add(p.getSellerId());
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User u : userMapper.selectBasicInfoByIds(new ArrayList<>(userIds))) {
                userMap.put(u.getId(), u);
            }
        }

        // 构建 FeedItemVO
        List<FeedItemVO> result = new ArrayList<>();
        for (ScoredItem si : scoredItems) {
            FeedItemVO vo = null;
            if ("POST".equals(si.itemType) || "AD".equals(si.itemType)) {
                Post postEntity = postMap.get(si.id);
                if (postEntity == null) continue;
                vo = new FeedItemVO();
                vo.setItemType("POST");
                vo.setId(postEntity.getId());
                vo.setTitle(postEntity.getTitle());
                vo.setContent(postEntity.getContent());
                vo.setLikeCount(postEntity.getLikeCount());
                vo.setCommentCount(postEntity.getCommentCount());
                vo.setViewCount(postEntity.getViewCount());
                vo.setUserId(postEntity.getUserId());
                vo.setPostType(postEntity.getPostType());
                vo.setCreatedAt(postEntity.getCreatedAt());
                vo.setLocation(postEntity.getLocation());
                vo.setIsAd(postEntity.getIsAd());
                vo.setExposureBoost(postEntity.getExposureBoost());
                vo.setTags(postEntity.getTags());
                vo.setCoverImage(postEntity.getCoverImage());
                User u = userMap.get(postEntity.getUserId());
                if (u != null) {
                    vo.setUserName(u.getNickname() != null ? u.getNickname() : u.getUsername());
                    vo.setUserAvatar(u.getAvatar());
                }
                vo.setIsLiked(userLikeMapper.exists(userId, "POST", postEntity.getId()));
            } else if ("PRODUCT".equals(si.itemType)) {
                Product productEntity = productMap.get(si.id);
                if (productEntity == null) continue;
                vo = new FeedItemVO();
                vo.setItemType("PRODUCT");
                vo.setId(productEntity.getId());
                vo.setTitle(productEntity.getName());
                vo.setContent(productEntity.getDescription());
                vo.setCoverImage(productEntity.getCoverImage());
                vo.setPrice(productEntity.getPrice() != null ? productEntity.getPrice().toString() : null);
                vo.setOriginalPrice(productEntity.getOriginalPrice() != null ? productEntity.getOriginalPrice().toString() : null);
                vo.setLikeCount(productEntity.getLikeCount());
                vo.setViewCount(productEntity.getViewCount());
                vo.setUserId(productEntity.getSellerId());
                vo.setLocation(productEntity.getLocation());
                vo.setCreatedAt(productEntity.getCreatedAt());
                vo.setTags(productEntity.getTags());
                User u = userMap.get(productEntity.getSellerId());
                if (u != null) {
                    vo.setUserName(u.getNickname() != null ? u.getNickname() : u.getUsername());
                    vo.setUserAvatar(u.getAvatar());
                }
                vo.setIsLiked(userLikeMapper.exists(userId, "PRODUCT", productEntity.getId()));
            }
            if (vo != null) result.add(vo);
        }

        log.info("推荐: userId={}, results={}", userId, result.size());
        return result;
    }

    /** 新用户无浏览历史时，返回按点赞数排序的热门内容 */
    private List<FeedItemVO> getHotItems(Long userId, int limit) {
        List<FeedItemVO> result = new ArrayList<>();

        PostQueryDTO postQuery = new PostQueryDTO();
        postQuery.setPage(1);
        postQuery.setSize(limit);
        postQuery.setSortBy("hot");
        List<PostVO> hotPosts = postService.getPostList(postQuery, userId);
        for (PostVO post : hotPosts) {
            FeedItemVO vo = new FeedItemVO();
            vo.setItemType("POST");
            vo.setId(post.getId());
            vo.setTitle(post.getTitle());
            vo.setContent(post.getContent());
            vo.setLikeCount(post.getLikeCount());
            vo.setCommentCount(post.getCommentCount());
            vo.setViewCount(post.getViewCount());
            vo.setIsLiked(post.getIsLiked());
            vo.setUserId(post.getUserId());
            vo.setUserName(post.getUserName());
            vo.setUserAvatar(post.getUserAvatar());
            vo.setPostType(post.getPostType());
            vo.setCreatedAt(post.getCreatedAt());
            vo.setLocation(post.getLocation());
            vo.setIsAd(post.getIsAd());
            vo.setExposureBoost(post.getExposureBoost());
            vo.setTags(post.getTags());
            result.add(vo);
        }

        if (result.size() < limit) {
            ProductQueryDTO productQuery = new ProductQueryDTO();
            productQuery.setPage(1);
            productQuery.setSize(limit - result.size());
            productQuery.setStatus(1);
            productQuery.setSortBy("view_count");
            List<ProductVO> hotProducts = productService.getProductList(productQuery);
            for (ProductVO product : hotProducts) {
                FeedItemVO vo = new FeedItemVO();
                vo.setItemType("PRODUCT");
                vo.setId(product.getId());
                vo.setTitle(product.getName());
                vo.setContent(product.getDescription());
                vo.setCoverImage(product.getCoverImage());
                vo.setPrice(product.getPrice() != null ? product.getPrice().toString() : null);
                vo.setOriginalPrice(product.getOriginalPrice() != null ? product.getOriginalPrice().toString() : null);
                vo.setLikeCount(product.getLikeCount());
                vo.setViewCount(product.getViewCount());
                vo.setUserId(product.getSellerId());
                vo.setUserName(product.getSellerName());
                vo.setUserAvatar(product.getSellerAvatar());
                vo.setCategoryId(product.getCategoryId());
                vo.setCategoryName(product.getCategoryName());
                vo.setLocation(product.getLocation());
                vo.setCreatedAt(product.getCreatedAt());
                vo.setTags(product.getTags());
                result.add(vo);
            }
        }

        return result.stream().limit(limit).collect(Collectors.toList());
    }

    /** 将 PostVO 转为 Post 实体（仅保留必要字段） */
    private Post voToEntity(PostVO vo) {
        Post p = new Post();
        p.setId(vo.getId());
        p.setUserId(vo.getUserId());
        p.setTitle(vo.getTitle());
        p.setContent(vo.getContent());
        p.setPostType(vo.getPostType());
        p.setLikeCount(vo.getLikeCount());
        p.setCommentCount(vo.getCommentCount());
        p.setViewCount(vo.getViewCount());
        p.setCreatedAt(vo.getCreatedAt());
        p.setLocation(vo.getLocation());
        p.setIsAd(vo.getIsAd());
        p.setExposureBoost(vo.getExposureBoost());
        p.setTags(vo.getTags());
        p.setCoverImage(vo.getCoverImage());
        return p;
    }

    /** 将 ProductVO 转为 Product 实体（仅保留必要字段） */
    private Product voToEntity(ProductVO vo) {
        Product p = new Product();
        p.setId(vo.getId());
        p.setSellerId(vo.getSellerId());
        p.setName(vo.getName());
        p.setDescription(vo.getDescription());
        p.setCoverImage(vo.getCoverImage());
        p.setPrice(vo.getPrice());
        p.setOriginalPrice(vo.getOriginalPrice());
        p.setLikeCount(vo.getLikeCount());
        p.setViewCount(vo.getViewCount());
        p.setStatus(1);
        p.setCreatedAt(vo.getCreatedAt());
        p.setTags(vo.getTags());
        p.setLocation(vo.getLocation());
        return p;
    }

    private int getInt(Integer value) {
        return value != null ? value : 0;
    }

    /** 打分后的条目 */
    private static class ScoredItem {
        String itemType;  // POST, PRODUCT, AD
        Long id;
        double score;

        ScoredItem(String itemType, Long id, double score) {
            this.itemType = itemType;
            this.id = id;
            this.score = score;
        }
    }
}
