package com.campus.backend.service.impl;

import com.campus.backend.dto.*;
import com.campus.backend.entity.ChatConversation;
import com.campus.backend.entity.ChatMessage;
import com.campus.backend.common.ErrorCode;
import com.campus.backend.exception.BusinessException;
import com.campus.backend.exception.NotFoundException;
import com.campus.backend.mapper.ChatConversationMapper;
import com.campus.backend.mapper.ChatMessageMapper;
import com.campus.backend.mapper.ProductMapper;
import com.campus.backend.mapper.UserMapper;
import com.campus.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天/消息服务实现 - 核心功能
 * 用于用户间私信协商交易细节（价格、地点、时间），替代支付功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ChatMessageVO sendMessage(MessageSendDTO dto, Long senderId) {
        // 验证接收者存在
        if (userMapper.selectById(dto.getReceiverId()) == null) {
            throw new NotFoundException("接收者", dto.getReceiverId());
        }
        // 不能给自己发消息
        if (senderId.equals(dto.getReceiverId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能给自己发送消息");
        }
        // 验证商品（如果提供了）
        if (dto.getProductId() != null && productMapper.selectById(dto.getProductId()) == null) {
            throw new NotFoundException("商品", dto.getProductId());
        }

        // 查找或创建会话
        ChatConversation conversation = conversationMapper.findByUserPair(senderId, dto.getReceiverId());
        if (conversation == null) {
            conversation = new ChatConversation();
            conversation.setUser1Id(senderId);
            conversation.setUser2Id(dto.getReceiverId());
            conversation.setProductId(dto.getProductId());
            conversationMapper.insert(conversation);
        }

        // 发送消息
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversation.getId());
        message.setSenderId(senderId);
        message.setReceiverId(dto.getReceiverId());
        message.setContent(dto.getContent());
        message.setMessageType(dto.getMessageType() != null ? dto.getMessageType() : "text");
        messageMapper.insert(message);

        // 更新会话的最后消息信息
        conversationMapper.updateLastMessage(
                conversation.getId(),
                dto.getContent(),
                LocalDateTime.now(),
                senderId
        );

        log.info("消息发送成功: from={}, to={}", senderId, dto.getReceiverId());
        return convertMessageToVO(message);
    }

    @Override
    public List<ChatMessageVO> getConversationMessages(Long conversationId, Long userId) {
        // 验证当前用户是会话参与者
        ChatConversation conv = conversationMapper.findById(conversationId);
        if (conv == null) {
            throw new NotFoundException("会话", conversationId);
        }
        if (!isParticipant(conv, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看此会话");
        }

        List<ChatMessage> messages = messageMapper.selectAllByConversationId(conversationId);
        return messages.stream().map(this::convertMessageToVO).collect(Collectors.toList());
    }

    @Override
    public List<ConversationVO> getConversations(Long userId) {
        List<ChatConversation> conversations = conversationMapper.findByUserId(userId);

        return conversations.stream().map(conv -> {
            ConversationVO vo = new ConversationVO();
            BeanUtils.copyProperties(conv, vo);

            // 计算当前用户的未读数
            Long otherUserId = conv.getUser1Id().equals(userId) ? conv.getUser2Id() : conv.getUser1Id();
            Integer unread = conv.getUser1Id().equals(userId) ? conv.getUser1Unread() : conv.getUser2Unread();
            vo.setUnreadCount(unread != null ? unread : 0);

            // 对方用户信息
            ConversationVO.UserSimpleVO otherUser = buildUserSimpleVO(otherUserId);
            vo.setOtherUser(otherUser);

            // 关联商品信息
            if (conv.getProductId() != null) {
                var product = productMapper.selectById(conv.getProductId());
                if (product != null) {
                    ConversationVO.ProductBriefVO pvo = new ConversationVO.ProductBriefVO();
                    pvo.setId(product.getId());
                    pvo.setName(product.getName());
                    pvo.setCoverImage(product.getCoverImage());
                    pvo.setPrice(product.getPrice());
                    vo.setProductInfo(pvo);
                }
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public int getUnreadCount(Long userId) {
        List<ChatConversation> conversations = conversationMapper.findByUserId(userId);
        int total = 0;
        for (ChatConversation conv : conversations) {
            Long otherUserId = conv.getUser1Id().equals(userId) ? conv.getUser2Id() : conv.getUser1Id();
            Integer unread = conv.getUser1Id().equals(userId) ? conv.getUser1Unread() : conv.getUser2Unread();
            total += (unread != null ? unread : 0);
        }
        return total;
    }

    @Override
    @Transactional
    public void markMessageAsRead(Long messageId, Long userId) {
        ChatMessage msg = messageMapper.findById(messageId);
        if (msg == null) {
            throw new NotFoundException("消息", messageId);
        }
        if (!msg.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此消息");
        }
        messageMapper.markAsRead(messageId, userId);
    }

    @Override
    @Transactional
    public void markConversationAsRead(Long conversationId, Long userId) {
        ChatConversation conv = conversationMapper.findById(conversationId);
        if (conv == null) {
            throw new NotFoundException("会话", conversationId);
        }
        if (!isParticipant(conv, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此会话");
        }
        messageMapper.markConversationAsRead(conversationId, userId);
        conversationMapper.clearUnreadCount(conversationId, userId);
    }

    /** 检查是否是会话参与者 */
    private boolean isParticipant(ChatConversation conv, Long userId) {
        return conv.getUser1Id().equals(userId) || conv.getUser2Id().equals(userId);
    }

    /** 构建用户简要VO */
    private ConversationVO.UserSimpleVO buildUserSimpleVO(Long userId) {
        var user = userMapper.selectById(userId);
        if (user == null) return null;
        ConversationVO.UserSimpleVO vo = new ConversationVO.UserSimpleVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setSchool(user.getSchool());
        return vo;
    }

    /** 消息 Entity -> VO */
    private ChatMessageVO convertMessageToVO(ChatMessage msg) {
        ChatMessageVO vo = new ChatMessageVO();
        BeanUtils.copyProperties(msg, vo);

        try {
            if (msg.getSenderId() != null) {
                var sender = userMapper.selectById(msg.getSenderId());
                if (sender != null) {
                    ChatMessageVO.UserSimpleVO svo = new ChatMessageVO.UserSimpleVO();
                    svo.setId(sender.getId());
                    svo.setUsername(sender.getUsername());
                    svo.setNickname(sender.getNickname());
                    svo.setAvatar(sender.getAvatar());
                    vo.setSenderInfo(svo);
                }
            }
        } catch (Exception ignored) {}

        // 商品简要信息
        if (msg.getConversationId() != null) {
            try {
                var conv = conversationMapper.findById(msg.getConversationId());
                if (conv != null && conv.getProductId() != null) {
                    var product = productMapper.selectById(conv.getProductId());
                    if (product != null) {
                        ChatMessageVO.ProductBriefVO pvo = new ChatMessageVO.ProductBriefVO();
                        pvo.setId(product.getId());
                        pvo.setName(product.getName());
                        pvo.setCoverImage(product.getCoverImage());
                        pvo.setPrice(product.getPrice());
                        vo.setProductInfo(pvo);
                    }
                }
            } catch (Exception ignored) {}
        }

        return vo;
    }
}
