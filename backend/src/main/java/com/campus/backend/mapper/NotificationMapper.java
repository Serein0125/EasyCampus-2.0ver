package com.campus.backend.mapper;

import com.campus.backend.entity.Notification;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface NotificationMapper {

    @Insert("INSERT INTO notifications (user_id, actor_id, type, target_id, target_type, content, is_read) " +
            "VALUES (#{userId}, #{actorId}, #{type}, #{targetId}, #{targetType}, #{content}, #{isRead})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Notification notification);

    /** 分页查询用户通知，支持按类型筛选，JOIN users 获取操作者头像和昵称 */
    @Select("<script>" +
            "SELECT n.*, u.avatar AS actor_avatar, u.nickname AS actor_nickname " +
            "FROM notifications n LEFT JOIN users u ON n.actor_id = u.id " +
            "WHERE n.user_id = #{userId} " +
            "<if test='type != null'> AND n.type = #{type} </if>" +
            "ORDER BY n.created_at DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Notification> selectByUserId(@Param("userId") Long userId,
                                       @Param("type") String type,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    /** 获取用户未读通知数 */
    @Select("SELECT COUNT(*) FROM notifications WHERE user_id = #{userId} AND is_read = false")
    int countUnread(@Param("userId") Long userId);

    /** 标记单条通知为已读 */
    @Update("UPDATE notifications SET is_read = true WHERE id = #{id} AND user_id = #{userId}")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    /** 标记用户所有通知为已读 */
    @Update("UPDATE notifications SET is_read = true WHERE user_id = #{userId} AND is_read = false")
    int markAllAsRead(@Param("userId") Long userId);

    /** 删除用户所有通知（一键清空） */
    @Delete("DELETE FROM notifications WHERE user_id = #{userId}")
    int deleteAllByUserId(@Param("userId") Long userId);
}
