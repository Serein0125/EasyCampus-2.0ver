package com.campus.backend.websocket;

import com.campus.backend.config.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * WebSocket 握手拦截器：从查询参数 token 中校验 JWT
 *
 * 背景：WS 握手是 HTTP GET 升级请求，浏览器原生 WebSocket 无法自定义 Authorization 头，
 * 因此前端以 ?token=xxx 方式传递 JWT（见前端 api.ts 的 doConnect），这里做等价校验。
 * SecurityConfig 末尾的 anyRequest().permitAll() 放行了握手请求本身，
 * 真正的鉴权在本拦截器完成：token 无效/缺失 → 返回 false，握手直接失败（前端进入退避重连）。
 *
 * 校验通过后把 userId 写入 session attributes，供 ChatWebSocketHandler 识别当前用户。
 */
@Component
public class WsAuthInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WsAuthInterceptor.class);

    private final JwtUtil jwtUtil;

    public WsAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request.getURI().getQuery());
        if (token == null || token.isBlank()) {
            log.warn("WS 握手被拒绝: 缺少 token, uri={}", request.getURI());
            return false;
        }
        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            attributes.put("userId", userId);
            log.info("WS 握手通过: userId={}", userId);
            return true;
        } catch (Exception e) {
            log.warn("WS 握手被拒绝: token 无效 ({})", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手后无需处理
    }

    /** 从查询串中提取 token 参数（仅解析目标参数，避免引入完整 servlet 依赖） */
    private String extractToken(String query) {
        if (query == null || query.isBlank()) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "token".equals(kv[0])) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
