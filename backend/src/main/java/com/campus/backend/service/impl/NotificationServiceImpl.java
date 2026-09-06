package com.campus.backend.service.impl;

import com.campus.backend.entity.Notification;
import com.campus.backend.mapper.NotificationMapper;
import com.campus.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public void createNotification(Long userId, Long actorId, String type, Long targetId, String targetType, String content) {
        // 不给自己发通知（自己点赞/评论自己的内容不通知）
        if (userId.equals(actorId)) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setActorId(actorId);
        notification.setType(type);
        notification.setTargetId(targetId);
        notification.setTargetType(targetType);
        notification.setContent(content);
        notification.setIsRead(false);
        notificationMapper.insert(notification);
        log.info("创建通知: userId={}, actorId={}, type={}, targetId={}", userId, actorId, type, targetId);
    }

    @Override
    public List<Notification> getNotifications(Long userId, String type, int page, int size) {
        int offset = (page - 1) * size;
        return notificationMapper.selectByUserId(userId, type, offset, size);
    }

    @Override
    public int getUnreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    @Override
    public void markAsRead(Long id, Long userId) {
        notificationMapper.markAsRead(id, userId);
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationMapper.markAllAsRead(userId);
    }

    @Override
    public void clearAll(Long userId) {
        notificationMapper.deleteAllByUserId(userId);
        log.info("清空通知: userId={}", userId);
    }
}
