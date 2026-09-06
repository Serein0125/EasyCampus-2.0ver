package com.campus.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知实体
 * 支持3种类型：LIKE(点赞)、COMMENT(评论)、FOLLOW(关注)
 */
@Data
public class Notification {

    private Long id;
    /** 接收通知的用户ID */
    private Long userId;
    /** 触发通知的用户ID（操作者） */
    private Long actorId;
    /** 通知类型：LIKE / COMMENT / FOLLOW */
    private String type;
    /** 关联目标ID（帖子ID/商品ID/评论ID等） */
    private Long targetId;
    /** 关联目标类型：POST / PRODUCT / COMMENT */
    private String targetType;
    /** 通知文本内容 */
    private String content;
    /** 是否已读 */
    private Boolean isRead;
    private LocalDateTime createdAt;

    /** 非持久化：操作者头像 */
    private String actorAvatar;
    /** 非持久化：操作者昵称 */
    private String actorNickname;
}
