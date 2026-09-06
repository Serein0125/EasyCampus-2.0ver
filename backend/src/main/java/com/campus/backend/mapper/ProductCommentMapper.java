package com.campus.backend.mapper;

import com.campus.backend.entity.ProductComment;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ProductCommentMapper {

    @Select("SELECT * FROM product_comments WHERE id = #{id}")
    ProductComment selectById(Long id);

    @Insert("INSERT INTO product_comments (product_id, user_id, parent_id, content) " +
            "VALUES (#{productId}, #{userId}, #{parentId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProductComment comment);

    @Update("UPDATE product_comments SET status = 'DELETED' WHERE id = #{id}")
    int deleteById(Long id);

    @Update("UPDATE product_comments SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(Long id);

    @Update("UPDATE product_comments SET like_count = GREATEST(0, like_count - 1) WHERE id = #{id}")
    int decrementLikeCount(Long id);

    @Select("SELECT * FROM product_comments WHERE product_id = #{productId} AND status = 'PUBLISHED' " +
            "ORDER BY created_at ASC")
    List<ProductComment> selectByProductId(Long productId);

    @Select("SELECT * FROM product_comments WHERE product_id = #{productId} AND parent_id IS NULL " +
            "AND status = 'PUBLISHED' ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<ProductComment> selectTopLevelByProductId(@Param("productId") Long productId,
                                                     @Param("offset") int offset,
                                                     @Param("limit") int limit);

    @Select("SELECT * FROM product_comments WHERE parent_id = #{parentId} AND status = 'PUBLISHED' " +
            "ORDER BY created_at ASC")
    List<ProductComment> selectRepliesByParentId(Long parentId);

    @Select("SELECT COUNT(*) FROM product_comments WHERE product_id = #{productId} AND status = 'PUBLISHED'")
    int countByProductId(Long productId);
}
