package com.campus.backend.service;

import com.campus.backend.dto.ChatMessageVO;
import com.campus.backend.dto.ConversationVO;
import com.campus.backend.dto.MessageSendDTO;

import java.util.List;

/**
 * 聊天/消息服务接口 (核心功能 - 替代支付的协商机制)
 */
public interface ChatService {

    /**
     * 发送消息 (自动创建或复用会话)
     */
    ChatMessageVO sendMessage(MessageSendDTO sendDTO, Long senderId);

    /**
     * 获取与某用户的聊天记录
     */
    List<ChatMessageVO> getConversationMessages(Long conversationId, Long userId);

    /**
     * 获取当前用户的所有会话列表 (含最后一条消息摘要)
     */
    List<ConversationVO> getConversations(Long userId);

    /**
     * 获取未读消息总数
     */
    int getUnreadCount(Long userId);

    /**
     * 标记单条消息已读
     */
    void markMessageAsRead(Long messageId, Long userId);

    /**
     * 标记某会话的所有消息已读
     */
    void markConversationAsRead(Long conversationId, Long userId);
}
