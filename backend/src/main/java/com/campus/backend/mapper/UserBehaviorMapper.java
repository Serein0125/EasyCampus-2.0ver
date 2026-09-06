package com.campus.backend.mapper;

import com.campus.backend.entity.UserBehavior;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface UserBehaviorMapper {

    @Insert("INSERT INTO user_behaviors (user_id, behavior_type, target_type, target_id) VALUES (#{userId}, #{behaviorType}, #{targetType}, #{targetId})")
    int insert(UserBehavior behavior);

    @Select("SELECT target_id FROM (SELECT target_id, MAX(created_at) as max_ts FROM user_behaviors WHERE user_id = #{userId} AND target_type = #{targetType} GROUP BY target_id ORDER BY max_ts DESC LIMIT #{limit}) t")
    List<Long> selectRecentTargetIds(@Param("userId") Long userId, @Param("targetType") String targetType, @Param("limit") int limit);

    /** 查询用户最近N条被推荐展示过的内容ID（用于推荐流跨会话去重） */
    @Select("SELECT target_id FROM user_behaviors WHERE user_id = #{userId} AND behavior_type = #{behaviorType} AND target_type = #{targetType} AND created_at > #{since} ORDER BY created_at DESC LIMIT #{limit}")
    List<Long> selectRecentImpressions(@Param("userId") Long userId, @Param("behaviorType") String behaviorType, @Param("targetType") String targetType, @Param("since") java.time.LocalDateTime since, @Param("limit") int limit);

    /** 统计24小时内某广告对某用户的曝光次数（频次控制） */
    @Select("SELECT COUNT(*) FROM user_behaviors WHERE user_id = #{userId} AND behavior_type = #{behaviorType} AND target_type = 'POST' AND target_id = #{postId} AND created_at > #{since}")
    int countAdImpressions(@Param("userId") Long userId, @Param("behaviorType") String behaviorType, @Param("postId") Long postId, @Param("since") java.time.LocalDateTime since);
}
