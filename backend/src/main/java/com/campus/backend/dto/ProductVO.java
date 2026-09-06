package com.campus.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品信息VO (返回前端)
 */
@Data
public class ProductVO {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Long categoryId;
    private String categoryName;
    private Long sellerId;
    private String sellerName;
    private String sellerAvatar;
    private String sellerSchool;
    private Integer conditionLevel;
    private String conditionText;
    private List<String> imageUrls;
    private String coverImage;
    private Integer status;
    private String statusText;
    private Integer viewCount;
    private Integer likeCount;
    private Boolean isFavorited;
    private String location;
    private Integer deliveryMethod;
    private String deliveryMethodText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 商品故事相关 */
    private String storyTitle;
    private String storyContent;
    private List<String> storyImages;
    private Boolean hasStory;

    /** 销售模式 */
    private String saleMode;

    private String tags;

    /** 获取成色文本 */
    public String getConditionText() {
        if (conditionLevel == null) return "未设置";
        return switch (conditionLevel) {
            case 1 -> "全新";
            case 2 -> "几乎全新";
            case 3 -> "轻微使用痕迹";
            case 4 -> "明显使用痕迹";
            case 5 -> "一般";
            default -> "未设置";
        };
    }

    /** 获取状态文本 */
    public String getStatusText() {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "已下架";
            case 1 -> "在售";
            case 2 -> "已售出";
            case 3 -> "预约中";
            default -> "未知";
        };
    }

    /** 获取交付方式文本 */
    public String getDeliveryMethodText() {
        if (deliveryMethod == null) return "未设置";
        return switch (deliveryMethod) {
            case 1 -> "自提";
            case 2 -> "快递";
            case 3 -> "均可";
            default -> "未设置";
        };
    }
}
