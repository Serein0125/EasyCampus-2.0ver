package com.campus.backend.service.impl;

import com.campus.backend.mapper.ProductMapper;
import com.campus.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 卖家数据看板实现：对 products 表按 seller_id 做 SQL 聚合，
 * 组装为前端看板所需结构（summary / trend / categories / topProducts）。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProductMapper productMapper;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MM-dd");

    @Override
    public Map<String, Object> getSellerStats(Long sellerId) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1) 汇总卡片
        List<Map<String, Object>> summaryRows = productMapper.selectSellerSummary(sellerId);
        Map<String, Object> summary = summaryRows.isEmpty()
                ? emptySummary()
                : summaryRows.get(0);
        result.put("summary", summary);

        // 2) 近30天趋势（缺失日期补零）
        result.put("trend", fillTrend(productMapper.selectSellerTrend(sellerId)));

        // 3) 类目分布
        List<Map<String, Object>> categories = new ArrayList<>();
        for (Map<String, Object> row : productMapper.selectSellerCategoryStats(sellerId)) {
            Map<String, Object> cat = new LinkedHashMap<>();
            cat.put("name", row.get("categoryName"));
            cat.put("count", row.get("count"));
            cat.put("gmv", row.get("gmv"));
            categories.add(cat);
        }
        result.put("categories", categories);

        // 4) 浏览量 Top 商品
        result.put("topProducts", productMapper.selectSellerTopProducts(sellerId));

        return result;
    }

    /** 未登录/无数据时的空汇总 */
    private Map<String, Object> emptySummary() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("published", 0);
        s.put("onSale", 0);
        s.put("sold", 0);
        s.put("totalViews", 0);
        s.put("totalLikes", 0);
        s.put("gmv", 0);
        return s;
    }

    /** 把聚合到的天数据铺到「最近30天」的完整日期序列上，无数据的日期补 0 */
    private List<Map<String, Object>> fillTrend(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> byDay = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object day = row.get("day");
            if (day != null) {
                byDay.put(day.toString(), row);
            }
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String mmdd = date.format(DAY_FMT);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", mmdd);
            Map<String, Object> hit = byDay.get(date.toString());
            point.put("views", num(hit, "views"));
            point.put("likes", num(hit, "likes"));
            point.put("sales", num(hit, "sales"));
            trend.add(point);
        }
        return trend;
    }

    /** 把 MySQL 返回的数值对象安全转为 number，避免 null/字符串杂音 */
    private Number num(Map<String, Object> row, String key) {
        if (row == null || row.get(key) == null) {
            return 0;
        }
        Object v = row.get(key);
        if (v instanceof Number) {
            return (Number) v;
        }
        try {
            return Double.valueOf(v.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}