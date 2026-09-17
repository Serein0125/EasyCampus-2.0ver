package com.campus.backend.controller;

import com.campus.backend.common.Result;
import com.campus.backend.common.SecurityUtils;
import com.campus.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 卖家数据看板 - 基于 products 表的真实 SQL 聚合
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/dashboard")
@Tag(name = "数据看板", description = "卖家经营数据统计可视化")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "卖家统计看板数据", description = "对当前登录用户的商品做聚合统计")
    public Result<Map<String, Object>> getSellerStats() {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return Result.success(dashboardService.getSellerStats(sellerId));
    }
}