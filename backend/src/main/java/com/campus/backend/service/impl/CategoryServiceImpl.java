package com.campus.backend.service.impl;

import com.campus.backend.dto.CategoryCreateDTO;
import com.campus.backend.dto.CategoryUpdateDTO;
import com.campus.backend.dto.CategoryVO;
import com.campus.backend.entity.Category;
import com.campus.backend.common.ErrorCode;
import com.campus.backend.exception.BusinessException;
import com.campus.backend.exception.NotFoundException;
import com.campus.backend.mapper.CategoryMapper;
import com.campus.backend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分类服务实现 (树形结构)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryVO createCategory(CategoryCreateDTO dto) {
        if (categoryMapper.findByName(dto.getName()) != null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "分类名称已存在");
        }
        if (dto.getParentId() != null && categoryMapper.findById(dto.getParentId()) == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "父分类不存在");
        }

        Category category = new Category();
        BeanUtils.copyProperties(dto, category);
        if (category.getSortOrder() == null) category.setSortOrder(0);
        categoryMapper.insert(category);
        log.info("创建分类成功: {}", category.getName());
        return convertToVO(category);
    }

    @Override
    @Transactional
    public CategoryVO updateCategory(Long id, CategoryUpdateDTO dto) {
        Category category = getCategoryOrThrow(id);

        if (dto.getName() != null && !dto.getName().equals(category.getName())) {
            if (categoryMapper.findByName(dto.getName()) != null) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "分类名称已存在");
            }
            category.setName(dto.getName());
        }
        if (dto.getParentId() != null) {
            if (dto.getParentId().equals(id)) {
                throw new BusinessException(ErrorCode.CATEGORY_HAS_CHILDREN, "不能设置自己为父分类");
            }
            if (categoryMapper.findById(dto.getParentId()) == null) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "父分类不存在");
            }
            category.setParentId(dto.getParentId());
        }
        if (dto.getDescription() != null) category.setDescription(dto.getDescription());
        if (dto.getSortOrder() != null) category.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) category.setStatus(dto.getStatus());
        if (dto.getIconUrl() != null) category.setIconUrl(dto.getIconUrl());

        categoryMapper.update(category);
        return convertToVO(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = getCategoryOrThrow(id);
        if (categoryMapper.countByParentId(id) > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_CHILDREN, "该分类下有子分类，无法删除");
        }
        if (categoryMapper.countProductsByCategoryId(id) > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_CHILDREN, "该分类下有商品，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    @Override
    public CategoryVO getCategoryById(Long id) {
        return convertToVO(getCategoryOrThrow(id));
    }

    @Override
    public List<CategoryVO> getAllCategories() {
        return categoryMapper.findAll().stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<CategoryVO> getRootCategories() {
        return categoryMapper.findRootCategories().stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<CategoryVO> getCategoriesByParentId(Long parentId) {
        return categoryMapper.findByParentId(parentId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<CategoryVO> getCategoryTree() {
        List<Category> all = categoryMapper.findAll();
        List<CategoryVO> roots = all.stream()
                .filter(c -> c.getParentId() == null)
                .map(this::convertToVO)
                .collect(Collectors.toList());

        for (CategoryVO root : roots) {
            buildTree(root, all);
        }
        return roots;
    }

    @Override
    @Transactional
    public void updateCategoryStatus(Long id, Integer status) {
        getCategoryOrThrow(id);
        if (status != 0 && status != 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "状态值无效");
        }
        Category cat = new Category();
        cat.setId(id);
        cat.setStatus(status);
        categoryMapper.update(cat);
    }

    private Category getCategoryOrThrow(Long id) {
        Category cat = categoryMapper.findById(id);
        if (cat == null) {
            throw new NotFoundException("分类", id);
        }
        return cat;
    }

    private CategoryVO convertToVO(Category c) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(c, vo);
        vo.setChildren(new ArrayList<>());
        return vo;
    }

    private void buildTree(CategoryVO parent, List<Category> all) {
        List<CategoryVO> children = all.stream()
                .filter(c -> parent.getId().equals(c.getParentId()))
                .map(this::convertToVO)
                .collect(Collectors.toList());
        parent.setChildren(children);
        for (CategoryVO child : children) {
            buildTree(child, all);
        }
    }
}
