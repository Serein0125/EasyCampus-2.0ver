package com.campus.backend.config;

import com.campus.backend.websocket.ChatWebSocketHandler;
import com.campus.backend.websocket.WsAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置：注册 /ws/chat 实时聊天端点
 *
 * - 前端连接: ws://host:port/ws/chat?token=<JWT>（开发环境直连 8080，生产经 Nginx 反代）
 * - 鉴权: 握手请求本身被 SecurityConfig 的 anyRequest().permitAll() 放行，
 *   真正的 JWT 校验在 WsAuthInterceptor 中完成（原生 WS 无法携带 Authorization 头）
 * - setAllowedOriginPatterns("*"): 开发环境 vite 代理与局域网设备（如 192.168.x.x）均可接入
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WsAuthInterceptor wsAuthInterceptor;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler, WsAuthInterceptor wsAuthInterceptor) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.wsAuthInterceptor = wsAuthInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(wsAuthInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
