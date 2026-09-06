package com.campus.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdPackageVO {

    private String id;

    private String name;

    private String description;

    private BigDecimal price;

    /** @deprecated 旧版曝光提升倍数，保留兼容；新版使用 interval 固定间隔插入 */
    @Deprecated
    private Integer exposureBoost;

    private Integer durationDays;

    private String badge;

    private String estimatedReach;

    private String scenario;

    private String terms;

    /** 普通信息流中插入间隔：用户每浏览 interval 条非广告内容后插入1条广告 */
    private Integer interval;

    /** 是否享有首页 Banner 轮播位 */
    private Boolean hasBanner;

    /** 是否进入推荐流（推荐流中同样按 interval 间隔插入） */
    private Boolean hasRecommendation;
}
