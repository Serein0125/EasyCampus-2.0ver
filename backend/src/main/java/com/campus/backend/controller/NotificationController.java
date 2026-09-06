package com.campus.backend.controller;

import com.campus.backend.common.Result;
import com.campus.backend.common.SecurityUtils;
import com.campus.backend.dto.ConversationVO;
import com.campus.backend.entity.Notification;
import com.campus.backend.service.ChatService;
import com.campus.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v2/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final ChatService chatService;

    /** 获取通知列表，支持按类型筛选 */
    @GetMapping
    public Result<List<Notification>> getNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Notification> notifications = notificationService.getNotifications(userId, type, page, size);
        return Result.success(notifications);
    }

    /** 获取私信列表（以通知格式返回会话摘要） */
    @GetMapping("/chats")
    public Result<List<Notification>> getChatNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<ConversationVO> conversations = chatService.getConversations(userId);
        List<Notification> chatNotifications = new ArrayList<>();

        int offset = (page - 1) * size;
        int end = Math.min(offset + size, conversations.size());
        for (int i = offset; i < end; i++) {
            ConversationVO conv = conversations.get(i);
            ConversationVO.UserSimpleVO otherUser = conv.getOtherUser();
            if (otherUser == null) continue;

            Notification n = new Notification();
            n.setId(conv.getId());
            n.setUserId(userId);
            n.setActorId(otherUser.getId());
            n.setType("CHAT");
            n.setTargetId(conv.getId());
            n.setTargetType("CONVERSATION");
            n.setContent(conv.getLastMessage() != null ? conv.getLastMessage() : "暂无消息");
            n.setIsRead(conv.getUnreadCount() == null || conv.getUnreadCount() == 0);
            n.setCreatedAt(conv.getLastMessageAt() != null ? conv.getLastMessageAt() : conv.getUpdatedAt());
            n.setActorAvatar(otherUser.getAvatar());
            n.setActorNickname(otherUser.getNickname() != null ? otherUser.getNickname() : otherUser.getUsername());
            chatNotifications.add(n);
        }
        return Result.success(chatNotifications);
    }

    /** 获取未读通知数 */
    @GetMapping("/unread/count")
    public Result<Map<String, Object>> getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        int count = notificationService.getUnreadCount(userId);
        return Result.success(Map.of("count", count));
    }

    /** 标记单条通知为已读 */
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markAsRead(id, userId);
        return Result.success(null);
    }

    /** 标记所有通知为已读 */
    @PutMapping("/read/all")
    public Result<Void> markAllAsRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.success(null);
    }

    /** 清空所有通知 */
    @DeleteMapping("/clear")
    public Result<Void> clearAll() {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.clearAll(userId);
        return Result.success(null);
    }
}
