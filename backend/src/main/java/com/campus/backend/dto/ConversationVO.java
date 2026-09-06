package com.campus.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会话列表VO (返回前端 - 用于聊天会话列表)
 */
@Data
public class ConversationVO {

    private Long id;
    private Long user1Id;
    private Long user2Id;
    private Long productId;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Integer unreadCount;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 对方用户信息 */
    private UserSimpleVO otherUser;

    /** 关联商品信息 (可选) */
    private ProductBriefVO productInfo;

    /** 用户简要信息VO */
    @Data
    public static class UserSimpleVO {
        private Long id;
        private String username;
        private String nickname;
        private String avatar;
        private String school;
    }

    /** 商品简要信息VO */
    @Data
    public static class ProductBriefVO {
        private Long id;
        private String name;
        private String coverImage;
        private java.math.BigDecimal price;
    }
}
