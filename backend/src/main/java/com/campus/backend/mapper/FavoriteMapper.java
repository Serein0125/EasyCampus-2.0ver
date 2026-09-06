package com.campus.backend.mapper;

import com.campus.backend.entity.Favorite;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 商品收藏数据访问层
 */
@Mapper
public interface FavoriteMapper {

    /**
     * 收藏商品
     */
    @Insert("INSERT INTO favorites (user_id, product_id, target_type) VALUES (#{userId}, #{productId}, #{targetType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Favorite favorite);

    /** 取消收藏(商品) */
    @Delete("DELETE FROM favorites WHERE user_id = #{userId} AND product_id = #{productId} AND target_type = 'PRODUCT'")
    int delete(@Param("userId") Long userId, @Param("productId") Long productId);

    /** 取消收藏(通用) */
    @Delete("DELETE FROM favorites WHERE user_id = #{userId} AND product_id = #{targetId} AND target_type = #{targetType}")
    int deleteByTarget(@Param("userId") Long userId, @Param("targetId") Long targetId, @Param("targetType") String targetType);

    /** 检查是否已收藏(商品) */
    @Select("SELECT COUNT(*) > 0 FROM favorites WHERE user_id = #{userId} AND product_id = #{productId}")
    boolean exists(@Param("userId") Long userId, @Param("productId") Long productId);

    /** 检查是否已收藏(通用) */
    @Select("SELECT COUNT(*) > 0 FROM favorites WHERE user_id = #{userId} AND product_id = #{targetId} AND target_type = #{targetType}")
    boolean existsByTarget(@Param("userId") Long userId, @Param("targetId") Long targetId, @Param("targetType") String targetType);

    /** 查询用户的收藏列表（按类型筛选） */
    @Select("SELECT * FROM favorites WHERE user_id = #{userId} AND target_type = #{targetType} ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Favorite> selectByUserIdAndType(
        @Param("userId") Long userId,
        @Param("targetType") String targetType,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );

    /** 统计用户某类型收藏数 */
    @Select("SELECT COUNT(*) FROM favorites WHERE user_id = #{userId} AND target_type = #{targetType}")
    int countByUserIdAndType(@Param("userId") Long userId, @Param("targetType") String targetType);

    /**
     * 查询用户的收藏列表
     */
    @Select("SELECT * FROM favorites WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Favorite> selectByUserId(
        @Param("userId") Long userId,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );

    /**
     * 统计用户收藏数
     */
    @Select("SELECT COUNT(*) FROM favorites WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);

    /**
     * 统计商品的收藏数
     */
    @Select("SELECT COUNT(*) FROM favorites WHERE product_id = #{productId}")
    int countByProductId(@Param("productId") Long productId);
}
