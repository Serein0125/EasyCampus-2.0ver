package com.campus.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

/**
 * 更新商品请求DTO
 */
@Data
public class ProductUpdateDTO {

    /** 商品标题 */
    private String name;

    /** 商品描述 */
    private String description;

    /** 现售价格 */
    @Positive(message = "价格必须大于0")
    private BigDecimal price;

    /** 原价/全新价格 */
    private BigDecimal originalPrice;

    /** 分类ID */
    private Long categoryId;

    /** 成色等级 */
    private Integer conditionLevel;

    /** 商品图片URL列表 */
    private List<String> imageUrls;

    /** 封面图片URL */
    private String coverImage;

    /** 交易地点 */
    private String location;

    /** 交付方式 */
    private Integer deliveryMethod;

    /** 故事标题 */
    private String storyTitle;

    /** 故事正文 */
    private String storyContent;

    /** 故事配图URL列表 */
    private List<String> storyImages;

    /** 销售模式 */
    private String saleMode;

    /** 状态: 0-已下架, 1-在售, 2-已售出, 3-预约中 */
    private Integer status;
}
