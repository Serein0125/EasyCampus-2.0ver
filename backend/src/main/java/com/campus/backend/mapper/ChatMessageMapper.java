package com.campus.backend.mapper;

import com.campus.backend.entity.ChatMessage;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息数据访问层
 */
@Mapper
public interface ChatMessageMapper {

    /**
     * 发送消息
     */
    @Insert("INSERT INTO chat_messages (conversation_id, sender_id, receiver_id, content, message_type) " +
            "VALUES (#{conversationId}, #{senderId}, #{receiverId}, #{content}, #{messageType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessage message);

    /**
     * 根据ID查询消息
     */
    @Select("SELECT * FROM chat_messages WHERE id = #{id}")
    ChatMessage findById(Long id);

    /**
     * 查询某个会话的消息记录 (分页，时间倒序)
     */
    @Select("SELECT * FROM chat_messages WHERE conversation_id = #{conversationId} " +
            "ORDER BY created_at ASC LIMIT #{limit} OFFSET #{offset}")
    List<ChatMessage> selectByConversationId(
        @Param("conversationId") Long conversationId,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );

    /**
     * 查询某个会话的所有消息
     */
    @Select("SELECT * FROM chat_messages WHERE conversation_id = #{conversationId} ORDER BY created_at ASC")
    List<ChatMessage> selectAllByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 查询用户在某会话中的未读消息数
     */
    @Select("SELECT COUNT(*) FROM chat_messages WHERE conversation_id = #{conversationId} " +
            "AND receiver_id = #{userId} AND is_read = 0")
    int countUnreadInConversation(
        @Param("conversationId") Long conversationId,
        @Param("userId") Long userId
    );

    /**
     * 标记单条消息已读
     */
    @Update("UPDATE chat_messages SET is_read = 1, read_at = CURRENT_TIMESTAMP WHERE id = #{id} AND receiver_id = #{userId}")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 标记某会话中接收者的所有消息为已读
     */
    @Update("UPDATE chat_messages SET is_read = 1, read_at = CURRENT_TIMESTAMP " +
            "WHERE conversation_id = #{conversationId} AND receiver_id = #{userId} AND is_read = 0")
    int markConversationAsRead(
        @Param("conversationId") Long conversationId,
        @Param("userId") Long userId
    );
}
