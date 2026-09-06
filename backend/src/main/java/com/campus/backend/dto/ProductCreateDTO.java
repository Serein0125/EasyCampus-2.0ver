package com.campus.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 发布商品请求DTO
 */
@Data
public class ProductCreateDTO {

    @NotBlank(message = "商品标题不能为空")
    @Size(max = 100, message = "标题最长100个字符")
    private String name;

    /** 商品描述 */
    private String description;

    @NotNull(message = "价格不能为空")
    @Positive(message = "价格必须大于0")
    private BigDecimal price;

    /** 原价/全新价格 */
    private BigDecimal originalPrice;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    /**
     * 成色等级:
     * 1-全新, 2-几乎全新, 3-轻微使用痕迹,
     * 4-明显使用痕迹, 5-一般
     */
    private Integer conditionLevel;

    /** 商品图片URL列表 */
    private List<String> imageUrls;

    /** 封面图片URL */
    private String coverImage;

    /** 交易地点 */
    private String location;

    /** 交付方式: 1自提, 2快递, 3均可 */
    private Integer deliveryMethod;

    /** 故事标题（商品故事功能） */
    private String storyTitle;

    /** 故事正文（商品故事功能） */
    private String storyContent;

    /** 故事配图URL列表 */
    private List<String> storyImages;

    /** 逗号分隔标签 */
    private String tags;

    /** 销售模式：FIXED_PRICE（一口价）/AUCTION（拍卖） */
    private String saleMode;
}
