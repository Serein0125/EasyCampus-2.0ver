package com.campus.backend.mapper;

import com.campus.backend.entity.UserLike;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface UserLikeMapper {

    @Insert("INSERT INTO user_likes (user_id, target_type, target_id) VALUES (#{userId}, #{targetType}, #{targetId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserLike userLike);

    @Delete("DELETE FROM user_likes WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    int delete(@Param("userId") Long userId, @Param("targetType") String targetType, @Param("targetId") Long targetId);

    @Select("SELECT COUNT(*) > 0 FROM user_likes WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    boolean exists(@Param("userId") Long userId, @Param("targetType") String targetType, @Param("targetId") Long targetId);

    @Select("SELECT COUNT(*) FROM user_likes WHERE target_type = #{targetType} AND target_id = #{targetId}")
    int countByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    @Select("SELECT * FROM user_likes WHERE user_id = #{userId} AND target_type = #{targetType} " +
            "ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<UserLike> selectByUserIdAndType(@Param("userId") Long userId, @Param("targetType") String targetType,
                                          @Param("offset") int offset, @Param("limit") int limit);
}
