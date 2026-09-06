package com.campus.backend.mapper;

import com.campus.backend.entity.PostComment;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface PostCommentMapper {

    @Select("SELECT * FROM post_comments WHERE id = #{id}")
    PostComment selectById(Long id);

    @Insert("INSERT INTO post_comments (post_id, user_id, parent_id, content) " +
            "VALUES (#{postId}, #{userId}, #{parentId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PostComment comment);

    @Update("UPDATE post_comments SET status = 'DELETED' WHERE id = #{id}")
    int deleteById(Long id);

    @Update("UPDATE post_comments SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(Long id);

    @Update("UPDATE post_comments SET like_count = GREATEST(0, like_count - 1) WHERE id = #{id}")
    int decrementLikeCount(Long id);

    @Select("SELECT * FROM post_comments WHERE post_id = #{postId} AND status = 'PUBLISHED' " +
            "ORDER BY created_at ASC")
    List<PostComment> selectByPostId(Long postId);

    @Select("SELECT * FROM post_comments WHERE post_id = #{postId} AND parent_id IS NULL " +
            "AND status = 'PUBLISHED' ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<PostComment> selectTopLevelByPostId(@Param("postId") Long postId,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);

    @Select("SELECT * FROM post_comments WHERE parent_id = #{parentId} AND status = 'PUBLISHED' " +
            "ORDER BY created_at ASC")
    List<PostComment> selectRepliesByParentId(Long parentId);

    @Select("SELECT COUNT(*) FROM post_comments WHERE post_id = #{postId} AND status = 'PUBLISHED'")
    int countByPostId(Long postId);
}
