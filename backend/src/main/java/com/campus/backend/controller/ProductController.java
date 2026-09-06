package com.campus.backend.controller;

import com.campus.backend.common.Result;
import com.campus.backend.common.SecurityUtils;
import com.campus.backend.dto.*;
import com.campus.backend.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品管理控制器 - 闲鱼风格
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/products")
@Tag(name = "商品管理", description = "发布、浏览、搜索、编辑商品")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "商品列表(分页)", description = "支持分类筛选、关键词搜索、价格区间、排序")
    public Result<Map<String, Object>> getProductList(ProductQueryDTO query) {
        List<ProductVO> list = productService.getProductList(query);
        int total = productService.getProductCount(query);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", query.getPage());
        result.put("size", query.getSize());
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "商品详情")
    public Result<ProductVO> getProductDetail(@PathVariable Long id) {
        return Result.success(productService.getProductDetail(id));
    }

    @PostMapping
    @Operation(summary = "发布商品")
    public Result<ProductVO> createProduct(@Valid @RequestBody ProductCreateDTO dto) {
        return Result.success(productService.createProduct(dto, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑商品")
    public Result<ProductVO> updateProduct(@PathVariable Long id,
                                           @Valid @RequestBody ProductUpdateDTO dto) {
        return Result.success(productService.updateProduct(id, dto, SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "下架/删除商品")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id, SecurityUtils.getCurrentUserId());
        return Result.success("删除成功");
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "上架/下架商品 (1上架 0下架)")
    public Result<Void> toggleStatus(@PathVariable Long id,
                                      @RequestParam Integer status) {
        productService.toggleProductStatus(id, SecurityUtils.getCurrentUserId(), status);
        return Result.success("状态更新成功");
    }

    @PutMapping("/{id}/sold")
    @Operation(summary = "标记商品已售出")
    public Result<Void> markAsSold(@PathVariable Long id) {
        productService.toggleProductStatus(id, SecurityUtils.getCurrentUserId(), 2);
        return Result.success("已标记为售出");
    }

    @GetMapping("/my")
    @Operation(summary = "我的商品")
    public Result<List<ProductVO>> getMyProducts() {
        return Result.success(productService.getMyProducts(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/deploy-version")
    @Operation(summary = "部署版本检测")
    public Result<String> getDeployVersion() {
        return Result.success("deployed", "v2.0-permitall-test");
    }

    @GetMapping("/ping")
    @Operation(summary = "Ping测试")
    public Result<String> ping() {
        return Result.success("pong", "server-is-alive");
    }
}
