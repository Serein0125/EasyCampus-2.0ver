package com.campus.backend.controller;

import com.campus.backend.common.Result;
import com.campus.backend.dto.CategoryCreateDTO;
import com.campus.backend.dto.CategoryUpdateDTO;
import com.campus.backend.dto.CategoryVO;
import com.campus.backend.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/categories")
@Tag(name = "分类管理", description = "商品分类(树形结构)")
@RequiredArgsConstructor
public class CategoryApi {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "创建分类")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CategoryVO> createCategory(@Valid @RequestBody CategoryCreateDTO dto) {
        return Result.success(categoryService.createCategory(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新分类")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CategoryVO> updateCategory(@PathVariable Long id,
                                             @Valid @RequestBody CategoryUpdateDTO dto) {
        return Result.success(categoryService.updateCategory(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success("删除成功");
    }

    @GetMapping("/{id}")
    @Operation(summary = "分类详情")
    public Result<CategoryVO> getCategoryById(@PathVariable Long id) {
        return Result.success(categoryService.getCategoryById(id));
    }

    @GetMapping
    @Operation(summary = "所有分类(扁平列表)")
    public Result<List<CategoryVO>> getAllCategories() {
        return Result.success(categoryService.getAllCategories());
    }

    @GetMapping("/roots")
    @Operation(summary = "根分类列表")
    public Result<List<CategoryVO>> getRootCategories() {
        return Result.success(categoryService.getRootCategories());
    }

    @GetMapping("/parent/{parentId}")
    @Operation(summary = "子分类列表")
    public Result<List<CategoryVO>> getCategoriesByParentId(@PathVariable Long parentId) {
        return Result.success(categoryService.getCategoriesByParentId(parentId));
    }

    @GetMapping("/tree")
    @Operation(summary = "分类树形结构")
    public Result<List<CategoryVO>> getCategoryTree() {
        return Result.success(categoryService.getCategoryTree());
    }
}