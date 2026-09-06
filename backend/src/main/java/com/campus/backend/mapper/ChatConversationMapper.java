package com.campus.backend.mapper;

import com.campus.backend.entity.ChatConversation;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天会话数据访问层
 */
@Mapper
public interface ChatConversationMapper {

    /**
     * 新建会话
     */
    @Insert("INSERT INTO chat_conversations (user1_id, user2_id, product_id, status) " +
            "VALUES (#{user1Id}, #{user2Id}, #{productId}, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatConversation conversation);

    /**
     * 根据ID查询会话
     */
    @Select("SELECT * FROM chat_conversations WHERE id = #{id}")
    ChatConversation findById(Long id);

    /**
     * 根据两个用户ID查找已有会话 (唯一索引)
     */
    @Select("SELECT * FROM chat_conversations " +
            "WHERE ((user1_id = #{userId1} AND user2_id = #{userId2}) " +
            "OR (user1_id = #{userId2} AND user2_id = #{userId1})) " +
            "AND status = 1 LIMIT 1")
    ChatConversation findByUserPair(
        @Param("userId1") Long userId1,
        @Param("userId2") Long userId2
    );

    /**
     * 查询用户的所有会话 (按最后消息时间倒序)
     */
    @Select("SELECT * FROM chat_conversations " +
            "WHERE (user1_id = #{userId} OR user2_id = #{userId}) AND status = 1 " +
            "ORDER BY COALESCE(last_message_at, created_at) DESC")
    List<ChatConversation> findByUserId(@Param("userId") Long userId);

    /**
     * 更新会话的最后消息信息
     */
    @Update("UPDATE chat_conversations SET " +
            "last_message = #{lastMessage}, " +
            "last_message_at = #{messageAt}, " +
            "user1_unread = CASE WHEN user2_id = #{senderId} THEN user1_unread + 1 ELSE user1_unread END, " +
            "user2_unread = CASE WHEN user1_id = #{senderId} THEN user2_unread + 1 ELSE user2_unread END, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{conversationId}")
    int updateLastMessage(
        @Param("conversationId") Long conversationId,
        @Param("lastMessage") String lastMessage,
        @Param("messageAt") LocalDateTime messageAt,
        @Param("senderId") Long senderId
    );

    /**
     * 清除某用户的未读计数 (标记已读)
     */
    @Update("UPDATE chat_conversations SET " +
            "user1_unread = CASE WHEN user1_id = #{userId} THEN 0 ELSE user1_unread END, " +
            "user2_unread = CASE WHEN user2_id = #{userId} THEN 0 ELSE user2_unread END, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{conversationId}")
    int clearUnreadCount(
        @Param("conversationId") Long conversationId,
        @Param("userId") Long userId
    );

    /**
     * 关闭会话
     */
    @Update("UPDATE chat_conversations SET status = 0, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int closeConversation(@Param("id") Long id);
}
