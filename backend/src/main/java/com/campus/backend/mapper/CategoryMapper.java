package com.campus.backend.mapper;

import com.campus.backend.entity.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 分类数据访问层 (树形结构)
 */
@Mapper
public interface CategoryMapper {

    /**
     * 新增分类
     */
    @Insert("INSERT INTO categories (name, description, icon_url, parent_id, sort_order, status) " +
            "VALUES (#{name}, #{description}, #{iconUrl}, #{parentId}, #{sortOrder}, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Category category);

    /**
     * 更新分类
     */
    @Update("UPDATE categories SET name = #{name}, description = #{description}, icon_url = #{iconUrl}, " +
            "parent_id = #{parentId}, sort_order = #{sortOrder}, status = #{status}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(Category category);

    /**
     * 删除分类
     */
    @Delete("DELETE FROM categories WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 根据ID查询分类
     */
    @Select("SELECT * FROM categories WHERE id = #{id}")
    Category findById(Long id);

    /**
     * 根据名称查询分类
     */
    @Select("SELECT * FROM categories WHERE name = #{name}")
    Category findByName(String name);

    /**
     * 查询所有分类 (按排序)
     */
    @Select("SELECT * FROM categories ORDER BY sort_order ASC, created_at ASC")
    List<Category> findAll();

    /**
     * 查询根分类 (无父分类)
     */
    @Select("SELECT * FROM categories WHERE parent_id IS NULL ORDER BY sort_order ASC, created_at ASC")
    List<Category> findRootCategories();

    /**
     * 根据父分类ID查询子分类
     */
    @Select("SELECT * FROM categories WHERE parent_id = #{parentId} ORDER BY sort_order ASC, created_at ASC")
    List<Category> findByParentId(@Param("parentId") Long parentId);

    /**
     * 统计子分类数量
     */
    @Select("SELECT COUNT(*) FROM categories WHERE parent_id = #{parentId}")
    int countByParentId(@Param("parentId") Long parentId);

    /**
     * 统计某分类下的商品数量
     */
    @Select("SELECT COUNT(*) FROM products WHERE category_id = #{categoryId}")
    int countProductsByCategoryId(@Param("categoryId") Long categoryId);
}
