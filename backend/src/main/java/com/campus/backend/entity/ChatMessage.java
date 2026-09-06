package com.campus.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 * 对应数据库 chat_messages 表
 */
@Data
public class ChatMessage {

    /** 消息ID */
    private Long id;

    /** 所属会话ID */
    private Long conversationId;

    /** 发送者用户ID */
    private Long senderId;

    /** 接收者用户ID */
    private Long receiverId;

    /** 消息内容 */
    private String content;

    /** 消息类型: text文本, image图片, system系统消息 */
    private String messageType;

    /** 是否已读: 0未读, 1已读 */
    private Boolean isRead;

    /** 阅读时间 */
    private LocalDateTime readAt;

    /** 发送时间 */
    private LocalDateTime createdAt;
}
