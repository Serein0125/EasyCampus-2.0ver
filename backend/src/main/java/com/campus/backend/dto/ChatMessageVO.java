package com.campus.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息VO (返回前端)
 */
@Data
public class ChatMessageVO {

    private Long id;
    private Long conversationId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private String messageType;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    /** 发送者简要信息 */
    private UserSimpleVO senderInfo;

    /** 接收者简要信息 */
    private UserSimpleVO receiverInfo;

    /** 关联商品信息 (可选) */
    private ProductBriefVO productInfo;

    /**
     * 用户简要信息VO (用于聊天列表展示)
     */
    @Data
    public static class UserSimpleVO {
        private Long id;
        private String username;
        private String nickname;
        private String avatar;
    }

    /**
     * 商品简要信息VO (用于聊天中展示)
     */
    @Data
    public static class ProductBriefVO {
        private Long id;
        private String name;
        private String coverImage;
        private java.math.BigDecimal price;
    }
}
