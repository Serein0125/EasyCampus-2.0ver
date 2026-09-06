package com.campus.backend.service.impl;

import com.campus.backend.dto.AdCreateDTO;
import com.campus.backend.dto.AdPackageVO;
import com.campus.backend.dto.PostVO;
import com.campus.backend.entity.Post;
import com.campus.backend.exception.NotFoundException;
import com.campus.backend.mapper.PostMapper;
import com.campus.backend.service.AdService;
import com.campus.backend.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdServiceImpl implements AdService {

    private final PostMapper postMapper;
    private final PostService postService;

    private static final List<AdPackageVO> AD_PACKAGES = new ArrayList<>();

    static {
        // 体验推流：最低门槛，仅普通信息流展示，间隔最长
        AdPackageVO trial = new AdPackageVO();
        trial.setId("trial");
        trial.setName("体验推流");
        trial.setDescription("低成本试水推流，仅在首页信息流中展示，用户每浏览约45条内容出现1次");
        trial.setPrice(new BigDecimal("1.90"));
        trial.setExposureBoost(2);  // 保留兼容旧版
        trial.setDurationDays(1);
        trial.setBadge("体验");
        trial.setInterval(45);
        trial.setHasBanner(false);
        trial.setHasRecommendation(false);
        trial.setEstimatedReach("约 150-400 次曝光");
        trial.setScenario("适合首次体验推流的用户，推广闲置物品、二手教材等个人内容");
        trial.setTerms("每人限购1次；曝光量受平台活跃度影响；到期后自动下线；不支持退款");
        AD_PACKAGES.add(trial);

        // 基础推流：普通信息流间隔缩短，不上推荐流和Banner
        AdPackageVO basic = new AdPackageVO();
        basic.setId("basic");
        basic.setName("基础推流");
        basic.setDescription("普通信息流更高频展示，用户每浏览约30条内容出现1次，持续3天稳定曝光");
        basic.setPrice(new BigDecimal("4.90"));
        basic.setExposureBoost(3);  // 保留兼容旧版
        basic.setDurationDays(3);
        basic.setBadge("推荐");
        basic.setInterval(30);
        basic.setHasBanner(false);
        basic.setHasRecommendation(false);
        basic.setEstimatedReach("约 600-1500 次曝光");
        basic.setScenario("适合推广二手数码、社团活动、兼职信息等普通个人内容");
        basic.setTerms("同一内容不可重复购买同套餐；曝光量为预估值，实际受内容质量影响；到期后自动下线");
        AD_PACKAGES.add(basic);

        // 热门推流：信息流高频 + 推荐流
        AdPackageVO standard = new AdPackageVO();
        standard.setId("standard");
        standard.setName("热门推流");
        standard.setDescription("信息流高频展示 + 推荐流精准推送，双渠道覆盖，用户每浏览约18条内容出现1次");
        standard.setPrice(new BigDecimal("14.90"));
        standard.setExposureBoost(5);  // 保留兼容旧版
        standard.setDurationDays(7);
        standard.setBadge("热门");
        standard.setInterval(18);
        standard.setHasBanner(false);
        standard.setHasRecommendation(true);
        standard.setEstimatedReach("约 2500-6000 次曝光");
        standard.setScenario("适合推广高价值商品、考研资料、校园周边服务等需要精准触达的内容");
        standard.setTerms("可叠加购买（最多2次）；享受信息流+推荐流双渠道曝光；到期后自动下线；内容违规将终止推流且不退款");
        AD_PACKAGES.add(standard);

        // 校园爆款：全渠道覆盖（信息流 + 推荐流 + 首页Banner）
        AdPackageVO premium = new AdPackageVO();
        premium.setId("premium");
        premium.setName("校园爆款");
        premium.setDescription("全渠道推流：信息流最高频（约12条1次）+ 推荐流精准推送 + 首页Banner轮播，持续14天长效曝光");
        premium.setPrice(new BigDecimal("29.90"));
        premium.setExposureBoost(10);  // 保留兼容旧版
        premium.setDurationDays(14);
        premium.setBadge("爆款");
        premium.setInterval(12);
        premium.setHasBanner(true);
        premium.setHasRecommendation(true);
        premium.setEstimatedReach("约 8000-25000 次曝光");
        premium.setScenario("适合社团招新、商家入驻推广、大型活动宣传、毕业季清仓等高曝光需求内容");
        premium.setTerms("可叠加购买（最多2次）；享受信息流+推荐流+首页Banner三位一体曝光；到期后自动下线；内容违规将终止推流且不退款");
        AD_PACKAGES.add(premium);
    }

    @Override
    @Transactional
    public PostVO createAd(AdCreateDTO dto, Long userId) {
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setPostType(dto.getPostType() != null ? dto.getPostType() : "SHOWCASE");
        post.setTags(dto.getTags());
        post.setIsAd(true);

        // 处理图片：将 imageUrls 列表序列化为 JSON 字符串存入数据库
        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            try {
                post.setImageUrls(new ObjectMapper().writeValueAsString(dto.getImageUrls()));
            } catch (Exception e) {
                log.warn("广告图片URL序列化失败", e);
            }
        }
        post.setCoverImage(dto.getCoverImage());

        AdPackageVO pkg = getAdPackage(dto.getPackageId());
        if (pkg != null) {
            post.setExposureBoost(pkg.getExposureBoost());
        } else {
            post.setExposureBoost(1);
        }

        postMapper.insert(post);
        log.info("发布广告: id={}, userId={}, packageId={}, exposureBoost={}",
                post.getId(), userId, dto.getPackageId(), post.getExposureBoost());
        return postService.getPostDetail(post.getId(), userId);
    }

    @Override
    public List<AdPackageVO> getAdPackages() {
        return AD_PACKAGES;
    }

    @Override
    public AdPackageVO getAdPackage(String packageId) {
        if (packageId == null) return null;
        return AD_PACKAGES.stream()
                .filter(p -> p.getId().equals(packageId))
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional
    public boolean simulatePayment(Long userId, Long postId, String packageId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new NotFoundException("Post", postId);
        }
        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此广告");
        }

        AdPackageVO pkg = getAdPackage(packageId);
        if (pkg == null) {
            throw new RuntimeException("无效的推流套餐");
        }

        post.setExposureBoost(pkg.getExposureBoost());
        postMapper.update(post);

        log.info("模拟付费推流: postId={}, userId={}, packageId={}, exposureBoost={}",
                postId, userId, packageId, pkg.getExposureBoost());
        return true;
    }
}
