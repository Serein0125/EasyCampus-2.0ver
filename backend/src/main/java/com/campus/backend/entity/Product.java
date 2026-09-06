package com.campus.backend.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体类
 * 对应数据库 products 表
 * 闲鱼风格：支持成色、原价、多图、浏览量等字段
 */
@Data
public class Product {

    /** 商品ID */
    private Long id;

    /** 商品标题 */
    private String name;

    /** 商品详细描述 */
    private String description;

    /** 现售价格 */
    private BigDecimal price;

    /** 原价/全新价格 */
    private BigDecimal originalPrice;

    /** 所属分类ID */
    private Long categoryId;

    /** 卖家用户ID */
    private Long sellerId;

    /**
     * 成色等级:
     * 1-全新, 2-几乎全新, 3-轻微使用痕迹,
     * 4-明显使用痕迹, 5-一般
     */
    private Integer conditionLevel;

    /** 商品图片URL列表(JSON数组) */
    private String imageUrls;

    /** 封面图片URL */
    private String coverImage;

    /**
     * 状态:
     * 0-已下架/删除, 1-在售, 2-已售出, 3-预约中
     */
    private Integer status;

    /** 浏览次数 */
    private Integer viewCount;

    /** 收藏/喜欢次数 */
    private Integer likeCount;

    /** 交易地点(如: 清华图书馆) */
    private String location;

    /** 交付方式: 1自提, 2快递, 3均可 */
    private Integer deliveryMethod;

    /** 故事标题（商品故事功能） */
    private String storyTitle;

    /** 故事正文（富文本/长文本，商品故事功能） */
    private String storyContent;

    /** 故事配图URL列表（JSON数组，与商品主图独立） */
    private String storyImages;

    /** 是否有故事内容（便于筛选） */
    private Boolean hasStory;

    /** 销售模式：FIXED_PRICE（一口价）/ AUCTION（拍卖） */
    private String saleMode;

    /** 关联交易组织ID（组织化交易功能） */
    private Long orgId;

    /** 可见性：PUBLIC / ORG_ONLY */
    private String visibility;

    private String tags;

    private String campusTag;

    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
