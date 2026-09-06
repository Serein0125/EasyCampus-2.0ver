package com.campus.backend.common;

import com.campus.backend.config.JwtUtil;
import com.campus.backend.exception.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 安全工具类 - 用于从JWT Token中提取当前用户ID
 * 解决Controller层循环依赖问题
 */
@Component
public class SecurityUtils {

    private static JwtUtil jwtUtil;

    public SecurityUtils(JwtUtil jwtUtil) {
        SecurityUtils.jwtUtil = jwtUtil;
    }

    /**
     * 获取当前登录用户的ID
     * 优先从SecurityContext获取username再查库，或直接从JWT解析userId
     */
    public static Long getCurrentUserId() {
        Long userId = getCurrentUserIdOrNull();
        if (userId == null) {
            throw new AuthenticationException("无法获取当前用户信息，请先登录");
        }
        return userId;
    }

    /**
     * 获取当前登录用户的ID，未登录时返回null（不抛异常）
     */
    public static Long getCurrentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String token = extractToken(request);
                if (token != null) {
                    try {
                        return jwtUtil.getUserIdFromToken(token);
                    } catch (Exception e) {
                        // Token解析失败返回null
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        throw new AuthenticationException("未认证");
    }

    /**
     * 从请求头提取Bearer Token
     */
    private static String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
