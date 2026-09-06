package com.campus.backend.mapper;

import com.campus.backend.entity.Product;
import com.campus.backend.dto.ProductQueryDTO;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface ProductMapper {

    List<Product> selectProductList(@Param("query") ProductQueryDTO query);

    int selectProductCount(@Param("query") ProductQueryDTO query);

    @Select("SELECT * FROM products WHERE id = #{id}")
    Product selectById(Long id);

    @Insert("INSERT INTO products (name, description, price, original_price, category_id, seller_id, " +
            "condition_level, image_urls, cover_image, location, delivery_method, status, " +
            "story_title, story_content, story_images, has_story, sale_mode, tags) " +
            "VALUES (#{name}, #{description}, #{price}, #{originalPrice}, #{categoryId}, #{sellerId}, " +
            "#{conditionLevel}, #{imageUrls}, #{coverImage}, #{location}, #{deliveryMethod}, 1, " +
            "#{storyTitle}, #{storyContent}, #{storyImages}, #{hasStory}, #{saleMode}, #{tags})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);

    int update(Product product);

    @Update("UPDATE products SET status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE products SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    @Update("UPDATE products SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Long id);

    @Update("UPDATE products SET like_count = GREATEST(0, like_count - 1) WHERE id = #{id}")
    int decrementLikeCount(@Param("id") Long id);

    @Select("SELECT * FROM products WHERE seller_id = #{sellerId} ORDER BY created_at DESC")
    List<Product> selectBySellerId(@Param("sellerId") Long sellerId);

    @Select("SELECT COUNT(*) FROM products WHERE category_id = #{categoryId}")
    int countByCategoryId(@Param("categoryId") Long categoryId);

    @Delete("DELETE FROM products WHERE id = #{id}")
    int deleteById(Long id);

    List<Map<String, Object>> searchProducts(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);

    int searchProductCount(@Param("keyword") String keyword);

    @Select("<script>SELECT * FROM products WHERE id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Product> selectByIds(@Param("ids") List<Long> ids);
}
