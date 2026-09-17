package com.campus.backend.service;

import java.util.Map;

/**
 * 卖家数据看板服务
 */
public interface DashboardService {

    /**
     * 获取指定卖家的经营统计数据（汇总/30天趋势/类目分布/Top商品）
     */
    Map<String, Object> getSellerStats(Long sellerId);
}