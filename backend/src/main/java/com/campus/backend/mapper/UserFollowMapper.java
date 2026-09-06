package com.campus.backend.mapper;

import com.campus.backend.entity.UserFollow;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface UserFollowMapper {

    @Insert("INSERT INTO user_follows (follower_id, followee_id) VALUES (#{followerId}, #{followeeId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserFollow follow);

    @Delete("DELETE FROM user_follows WHERE follower_id = #{followerId} AND followee_id = #{followeeId}")
    int delete(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    @Select("SELECT COUNT(*) > 0 FROM user_follows WHERE follower_id = #{followerId} AND followee_id = #{followeeId}")
    boolean exists(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    @Select("SELECT COUNT(*) FROM user_follows WHERE follower_id = #{userId}")
    int countFollowing(Long userId);

    @Select("SELECT COUNT(*) FROM user_follows WHERE followee_id = #{userId}")
    int countFollowers(Long userId);

    @Select("SELECT * FROM user_follows WHERE follower_id = #{userId} ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<UserFollow> selectFollowing(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT * FROM user_follows WHERE followee_id = #{userId} ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<UserFollow> selectFollowers(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
}
