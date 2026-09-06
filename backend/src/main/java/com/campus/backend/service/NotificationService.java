package com.campus.backend.service;

import com.campus.backend.entity.Notification;

import java.util.List;

public interface NotificationService {

    /** 创建通知 */
    void createNotification(Long userId, Long actorId, String type, Long targetId, String targetType, String content);

    /** 获取用户通知列表（分页，可按类型筛选） */
    List<Notification> getNotifications(Long userId, String type, int page, int size);

    /** 获取用户未读通知数 */
    int getUnreadCount(Long userId);

    /** 标记单条通知为已读 */
    void markAsRead(Long id, Long userId);

    /** 标记所有通知为已读 */
    void markAllAsRead(Long userId);

    /** 清空所有通知 */
    void clearAll(Long userId);
}
