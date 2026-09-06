package com.campus.backend.controller;

import com.campus.backend.common.Result;
import com.campus.backend.common.SecurityUtils;
import com.campus.backend.dto.ChatMessageVO;
import com.campus.backend.dto.ConversationVO;
import com.campus.backend.dto.MessageSendDTO;
import com.campus.backend.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天/消息控制器 - 核心功能
 * 用于用户间私信协商交易细节（价格、地点、时间），替代在线支付
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/chat")
@Tag(name = "聊天/消息", description = "用户间私信系统，协商交易细节")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/messages")
    @Operation(summary = "发送消息")
    public Result<ChatMessageVO> sendMessage(@Valid @RequestBody MessageSendDTO dto) {
        return Result.success(chatService.sendMessage(dto, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "获取会话的聊天记录")
    public Result<List<ChatMessageVO>> getConversationMessages(@PathVariable Long conversationId) {
        return Result.success(chatService.getConversationMessages(conversationId, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/conversations")
    @Operation(summary = "我的会话列表 (含最后一条消息摘要和未读数)")
    public Result<List<ConversationVO>> getConversations() {
        return Result.success(chatService.getConversations(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/unread/count")
    @Operation(summary = "未读消息总数")
    public Result<Integer> getUnreadCount() {
        return Result.success(chatService.getUnreadCount(SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/messages/{messageId}/read")
    @Operation(summary = "标记单条消息已读")
    public Result<Void> markMessageAsRead(@PathVariable Long messageId) {
        chatService.markMessageAsRead(messageId, SecurityUtils.getCurrentUserId());
        return Result.success();
    }

    @PutMapping("/conversations/{conversationId}/read")
    @Operation(summary = "标记整个会话已读")
    public Result<Void> markConversationAsRead(@PathVariable Long conversationId) {
        chatService.markConversationAsRead(conversationId, SecurityUtils.getCurrentUserId());
        return Result.success();
    }
}
