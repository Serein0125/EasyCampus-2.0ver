package com.campus.backend.common.interceptor;

import com.campus.backend.common.annotation.ApiVersion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

@Component
public class ApiVersionInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiVersionInterceptor.class);
    
    private static final String API_VERSION_HEADER = "X-API-Version";
    private static final String API_VERSION_PARAM = "api_version";
    private static final String DEFAULT_API_VERSION = "v1";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        Class<?> controllerClass = handlerMethod.getBeanType();
        
        // 获取请求的API版本
        String requestedVersion = getRequestedApiVersion(request);
        
        // 检查控制器和方法上的API版本注解
        ApiVersion classAnnotation = controllerClass.getAnnotation(ApiVersion.class);
        ApiVersion methodAnnotation = method.getAnnotation(ApiVersion.class);
        
        // 优先使用方法级别的注解，如果没有则使用类级别的注解
        ApiVersion apiVersion = methodAnnotation != null ? methodAnnotation : classAnnotation;
        
        if (apiVersion != null) {
            // 检查请求的版本是否支持
            if (!isVersionSupported(requestedVersion, apiVersion.supportedVersions())) {
                logger.warn("不支持的API版本: 请求版本={}, 支持版本={}", requestedVersion, apiVersion.supportedVersions());
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(String.format(
                        "{\"code\": 400, \"message\": \"不支持的API版本: %s。支持的版本: %s\"}",
                        requestedVersion, String.join(", ", apiVersion.supportedVersions())));
                return false;
            }
            
            // 检查API是否已弃用
            if (apiVersion.deprecated()) {
                logger.info("已弃用的API被调用: {} {}, 版本: {}", request.getMethod(), request.getRequestURI(), requestedVersion);
                response.setHeader("X-API-Deprecated", "true");
                response.setHeader("X-API-Deprecated-Message", apiVersion.deprecatedMessage());
                response.setHeader("X-API-Supported-Versions", String.join(", ", apiVersion.supportedVersions()));
            }
            
            // 设置响应头
            response.setHeader("X-API-Version", requestedVersion);
            response.setHeader("X-API-Supported-Versions", String.join(", ", apiVersion.supportedVersions()));
            
            if (apiVersion.since() != null && !apiVersion.since().isEmpty()) {
                response.setHeader("X-API-Since", apiVersion.since());
            }
        }
        
        // 将API版本存储到请求属性中，供后续使用
        request.setAttribute("apiVersion", requestedVersion);
        
        return true;
    }
    
    private String getRequestedApiVersion(HttpServletRequest request) {
        // 1. 检查请求头
        String version = request.getHeader(API_VERSION_HEADER);
        if (version != null && !version.trim().isEmpty()) {
            return version.trim().toLowerCase();
        }
        
        // 2. 检查URL路径参数
        version = request.getParameter(API_VERSION_PARAM);
        if (version != null && !version.trim().isEmpty()) {
            return version.trim().toLowerCase();
        }
        
        // 3. 检查URL路径中的版本
        String path = request.getRequestURI();
        if (path.contains("/api/v")) {
            int start = path.indexOf("/api/v") + 5;
            int end = path.indexOf("/", start);
            if (end == -1) {
                end = path.length();
            }
            version = path.substring(start, end);
            if (version.matches("v\\d+")) {
                return version;
            }
        }
        
        // 4. 返回默认版本
        return DEFAULT_API_VERSION;
    }
    
    private boolean isVersionSupported(String requestedVersion, String[] supportedVersions) {
        if (requestedVersion == null || supportedVersions == null) {
            return false;
        }
        
        for (String supportedVersion : supportedVersions) {
            if (supportedVersion.equalsIgnoreCase(requestedVersion)) {
                return true;
            }
        }
        
        return false;
    }
}