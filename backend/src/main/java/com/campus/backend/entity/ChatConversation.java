package com.campus.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天会话实体类
 * 对应数据库 chat_conversations 表
 * 用于管理用户间的私信会话
 */
@Data
public class ChatConversation {

    /** 会话ID */
    private Long id;

    /** 参与者1的用户ID */
    private Long user1Id;

    /** 参与者2的用户ID */
    private Long user2Id;

    /** 关联的商品ID (可选，用于商品咨询场景) */
    private Long productId;

    /** 最后一条消息内容 */
    private String lastMessage;

    /** 最后消息时间 */
    private LocalDateTime lastMessageAt;

    /** user1的未读消息数 */
    private Integer user1Unread;

    /** user2的未读消息数 */
    private Integer user2Unread;

    /** 状态: 0已关闭, 1正常 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
