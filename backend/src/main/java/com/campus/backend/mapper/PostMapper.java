package com.campus.backend.mapper;

import com.campus.backend.entity.Post;
import com.campus.backend.dto.PostQueryDTO;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface PostMapper {

    @Select("SELECT * FROM posts WHERE id = #{id}")
    Post selectById(Long id);

    @Insert("INSERT INTO posts (user_id, title, content, post_type, board_id, group_buy_id, is_pinned, status, tags, is_ad, exposure_boost, start_time, end_time, location, contact, image_urls, cover_image) " +
            "VALUES (#{userId}, #{title}, #{content}, #{postType}, #{boardId}, #{groupBuyId}, COALESCE(#{isPinned}, 0), 'PUBLISHED', #{tags}, COALESCE(#{isAd}, 0), COALESCE(#{exposureBoost}, 1), #{startTime}, #{endTime}, #{location}, #{contact}, #{imageUrls}, #{coverImage})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Post post);

    @Update("UPDATE posts SET title = #{title}, content = #{content}, post_type = #{postType}, " +
            "board_id = #{boardId}, exposure_boost = #{exposureBoost}, start_time = #{startTime}, " +
            "end_time = #{endTime}, location = #{location}, contact = #{contact}, " +
            "tags = #{tags}, image_urls = #{imageUrls}, cover_image = #{coverImage}, " +
            "updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int update(Post post);

    @Update("UPDATE posts SET status = 'DELETED', updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int deleteById(Long id);

    @Update("UPDATE posts SET status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE posts SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(Long id);

    @Update("UPDATE posts SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(Long id);

    @Update("UPDATE posts SET like_count = GREATEST(0, like_count - 1) WHERE id = #{id}")
    int decrementLikeCount(Long id);

    @Update("UPDATE posts SET comment_count = comment_count + 1 WHERE id = #{id}")
    int incrementCommentCount(Long id);

    @Update("UPDATE posts SET comment_count = GREATEST(0, comment_count - 1) WHERE id = #{id}")
    int decrementCommentCount(Long id);

    @Update("UPDATE posts SET is_pinned = #{isPinned}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int togglePin(@Param("id") Long id, @Param("isPinned") Boolean isPinned);

    @Update("UPDATE posts SET is_essence = #{isEssence}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int toggleEssence(@Param("id") Long id, @Param("isEssence") Boolean isEssence);

    List<Post> selectPostList(@Param("query") PostQueryDTO query);

    int selectPostCount(@Param("query") PostQueryDTO query);

    @Select("SELECT * FROM posts WHERE user_id = #{userId} AND status != 'DELETED' ORDER BY created_at DESC")
    List<Post> selectByUserId(Long userId);

    @Select("SELECT * FROM posts WHERE board_id = #{boardId} AND status = 'PUBLISHED' " +
            "ORDER BY is_pinned DESC, created_at DESC LIMIT #{offset}, #{limit}")
    List<Post> selectByBoardId(@Param("boardId") Long boardId, @Param("offset") int offset, @Param("limit") int limit);

    List<Map<String, Object>> searchPosts(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);

    int searchPostCount(@Param("keyword") String keyword);

    @Select("<script>SELECT * FROM posts WHERE id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Post> selectByIds(@Param("ids") List<Long> ids);
}
