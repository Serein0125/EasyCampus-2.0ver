package com.campus.backend.websocket;

import com.campus.backend.dto.ChatMessageVO;
import com.campus.backend.dto.MessageSendDTO;
import com.campus.backend.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 聊天 WebSocket 处理器（原生 WebSocket，非 STOMP）
 *
 * 协议（与前端 WebSocketManager 约定，见 frontend/src/services/api.ts）：
 * - 心跳: 收 {type:"ping", timestamp} → 回 {type:"pong", timestamp}
 * - 发消息: 收 {type:"chat", clientMsgId, content, receiverId, productId, timestamp}
 *          → 回 {type:"ack", clientMsgId} 成功 / {type:"error", clientMsgId, message} 失败
 *          并向接收者的在线会话推送 {type:"chat_message", ...消息VO字段, clientMsgId, timestamp}
 *
 * 持久化复用 ChatService.sendMessage（与 HTTP POST /api/v2/chat/messages 同一入口），
 * 保证会话管理、未读数、消息表写入逻辑与 HTTP 路径完全一致。
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ChatService chatService;
    /** 注入 Spring 容器的 ObjectMapper（已注册 JavaTimeModule，可序列化 LocalDateTime） */
    private final ObjectMapper objectMapper;

    /** 在线会话表：userId → 该用户的所有 WS 会话（支持同一用户多标签页） */
    private final Map<Long, Set<WebSocketSession>> onlineSessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            closeQuietly(session);
            return;
        }
        onlineSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("WS 上线: userId={}, 在线用户数={}", userId, onlineSessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Long senderId = getUserId(session);
        if (senderId == null) return;

        JsonNode node;
        try {
            node = objectMapper.readTree(message.getPayload());
        } catch (Exception e) {
            sendJson(session, errorNode(null, "消息格式错误"));
            return;
        }

        String type = node.path("type").asText("");
        String clientMsgId = node.hasNonNull("clientMsgId") ? node.get("clientMsgId").asText() : null;

        switch (type) {
            case "ping" -> sendJson(session, pongNode());
            case "chat" -> handleChatMessage(session, senderId, clientMsgId, node);
            default -> log.debug("WS 收到未支持的消息类型: {}", type);
        }
    }

    /** 处理聊天消息：校验并持久化 → 给发送方 ACK → 实时推送给接收者 */
    private void handleChatMessage(WebSocketSession session, Long senderId,
                                   String clientMsgId, JsonNode node) {
        try {
            MessageSendDTO dto = new MessageSendDTO();
            dto.setReceiverId(node.path("receiverId").asLong());
            dto.setContent(node.path("content").asText());
            if (node.hasNonNull("productId") && !node.get("productId").asText().isBlank()) {
                dto.setProductId(node.get("productId").asLong());
            }
            // WS 路径不经过 HTTP 层的 @Valid 校验，这里等价补上 content 非空检查
            // （receiverId 缺失/非法会由 ChatService 抛 NotFoundException 走 error 回执，无需重复校验）
            if (dto.getContent() == null || dto.getContent().isBlank()) {
                sendJson(session, errorNode(clientMsgId, "消息内容不能为空"));
                return;
            }

            ChatMessageVO vo = chatService.sendMessage(dto, senderId);

            // 1) 回执 ACK：前端据此确认消息送达（清除 pendingAcks 定时器）
            sendJson(session, ackNode(clientMsgId));

            // 2) 推送给接收者：在线实时收到；离线则无推送，由未读数轮询兜底
            pushToUser(vo.getReceiverId(), chatMessageNode(vo, clientMsgId));
        } catch (Exception e) {
            log.warn("WS 消息处理失败: senderId={}, err={}", senderId, e.getMessage());
            sendJson(session, errorNode(clientMsgId,
                    e.getMessage() != null ? e.getMessage() : "消息发送失败"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WS 传输异常: userId={}, err={}", getUserId(session), exception.getMessage());
        removeSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        removeSession(session);
        log.info("WS 下线: userId={}, status={}", getUserId(session), status);
    }

    /** 从在线表中移除会话（Set 为空时顺带清理用户条目） */
    private void removeSession(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId == null) return;
        Set<WebSocketSession> sessions = onlineSessions.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                onlineSessions.remove(userId, sessions);
            }
        }
    }

    /** 向指定用户的所有在线会话推送消息 */
    private void pushToUser(Long userId, ObjectNode payload) {
        Set<WebSocketSession> sessions = onlineSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) return;
        for (WebSocketSession session : sessions) {
            sendJson(session, payload);
        }
    }

    // ---------- 协议消息构造 ----------

    /** 聊天推送体：VO 字段 + type/clientMsgId/timestamp（前端按 senderId/receiverId/content/id/timestamp 消费） */
    private ObjectNode chatMessageNode(ChatMessageVO vo, String clientMsgId) {
        ObjectNode node = objectMapper.valueToTree(vo);
        node.put("type", "chat_message");
        if (clientMsgId != null) {
            node.put("clientMsgId", clientMsgId);
        }
        node.put("timestamp", System.currentTimeMillis());
        return node;
    }

    private ObjectNode ackNode(String clientMsgId) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "ack");
        if (clientMsgId != null) {
            node.put("clientMsgId", clientMsgId);
        }
        return node;
    }

    private ObjectNode errorNode(String clientMsgId, String errorMessage) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "error");
        if (clientMsgId != null) {
            node.put("clientMsgId", clientMsgId);
        }
        node.put("message", errorMessage);
        return node;
    }

    private ObjectNode pongNode() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "pong");
        node.put("timestamp", System.currentTimeMillis());
        return node;
    }

    // ---------- 底层收发 ----------

    /** 发送 JSON 文本帧；对 session 加锁串行化，避免并发写导致 TEXT_PARTIAL_WRITTEN 异常 */
    private void sendJson(WebSocketSession session, ObjectNode node) {
        if (session == null || !session.isOpen()) return;
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(node)));
            }
        } catch (IOException e) {
            log.warn("WS 发送失败: userId={}, err={}", getUserId(session), e.getMessage());
        }
    }

    private Long getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId instanceof Long ? (Long) userId : null;
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException ignored) {
        }
    }
}
