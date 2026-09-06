package com.campus.backend.service;

import com.campus.backend.dto.CategoryCreateDTO;
import com.campus.backend.dto.CategoryUpdateDTO;
import com.campus.backend.dto.CategoryVO;

import java.util.List;

/**
 * 分类服务接口 (树形结构管理)
 */
public interface CategoryService {

    /**
     * 创建分类
     */
    CategoryVO createCategory(CategoryCreateDTO createDTO);

    /**
     * 更新分类
     */
    CategoryVO updateCategory(Long id, CategoryUpdateDTO updateDTO);

    /**
     * 删除分类
     */
    void deleteCategory(Long id);

    /**
     * 根据ID获取分类详情
     */
    CategoryVO getCategoryById(Long id);

    /**
     * 获取所有分类 (扁平列表)
     */
    List<CategoryVO> getAllCategories();

    /**
     * 获取根分类
     */
    List<CategoryVO> getRootCategories();

    /**
     * 获取子分类
     */
    List<CategoryVO> getCategoriesByParentId(Long parentId);

    /**
     * 获取分类树形结构
     */
    List<CategoryVO> getCategoryTree();

    /**
     * 更新分类状态 (启用/禁用)
     */
    void updateCategoryStatus(Long id, Integer status);
}
