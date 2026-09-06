package com.campus.backend.dto;

import lombok.Data;
import java.util.List;

/**
 * 商品查询条件DTO
 */
@Data
public class ProductQueryDTO {

    /** 分类ID筛选 */
    private Integer categoryId;

    /** 关键词搜索(搜索标题和描述) */
    private String keyword;

    /** 卖家ID筛选 */
    private Long sellerId;

    /** 卖家ID列表筛选（支持IN查询） */
    private List<Long> sellerIds;

    /** 状态筛选 (默认查在售的) */
    private Integer status = 1;

    /** 成色筛选 */
    private Integer conditionLevel;

    /** 价格区间-最低价 */
    private java.math.BigDecimal minPrice;

    /** 价格区间-最高价 */
    private java.math.BigDecimal maxPrice;

    /** 是否有故事内容 */
    private Boolean hasStory;

    /** 标签筛选 */
    private String tag;

    /** 排序方式: price_asc, price_desc, time_desc(默认), view_count */
    private String sortBy = "time_desc";

    /** 页码(从1开始) */
    private Integer page = 1;

    /** 每页大小 */
    private Integer size = 10;

    /** 计算MySQL OFFSET */
    public Integer getOffset() {
        int safePage = (page == null || page < 1) ? 1 : page;
        int safeSize = (size == null || size < 1) ? 10 : size;
        return safeSize * (safePage - 1);
    }
}
