package com.campus.backend.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Aspect
@Component
public class LoggingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
    private final ObjectMapper objectMapper;
    
    public LoggingAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Pointcut("execution(* com.campus.backend.controller..*.*(..))")
    public void controllerPointcut() {
    }
    
    @Pointcut("execution(* com.campus.backend.service..*.*(..))")
    public void servicePointcut() {
    }
    
    @Before("controllerPointcut()")
    public void logBeforeController(JoinPoint joinPoint) {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            String args = Arrays.toString(joinPoint.getArgs());
            
            logger.info("=== 请求开始 ===");
            logger.info("请求URL: {} {}", request.getMethod(), request.getRequestURI());
            logger.info("控制器: {}.{}", className, methodName);
            logger.info("请求参数: {}", args);
            logger.info("客户端IP: {}", getClientIp(request));
            logger.info("用户代理: {}", request.getHeader("User-Agent"));
        } catch (Exception e) {
            logger.warn("记录请求日志失败: {}", e.getMessage());
        }
    }
    
    @AfterReturning(pointcut = "controllerPointcut()", returning = "result")
    public void logAfterController(JoinPoint joinPoint, Object result) {
        try {
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            
            String resultJson = objectMapper.writeValueAsString(result);
            // 限制日志长度，避免过长的响应体
            if (resultJson.length() > 1000) {
                resultJson = resultJson.substring(0, 1000) + "...[截断]";
            }
            
            logger.info("控制器响应: {}.{} -> {}", className, methodName, resultJson);
            logger.info("=== 请求结束 ===");
        } catch (Exception e) {
            logger.warn("记录响应日志失败: {}", e.getMessage());
        }
    }
    
    @AfterThrowing(pointcut = "controllerPointcut()", throwing = "exception")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable exception) {
        try {
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            
            logger.error("控制器异常: {}.{} -> {}", className, methodName, exception.getMessage(), exception);
        } catch (Exception e) {
            logger.warn("记录异常日志失败: {}", e.getMessage());
        }
    }
    
    @Around("servicePointcut()")
    public Object logAroundService(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String args = Arrays.stream(joinPoint.getArgs())
                .map(arg -> {
                    try {
                        return objectMapper.writeValueAsString(arg);
                    } catch (Exception e) {
                        return String.valueOf(arg);
                    }
                })
                .collect(Collectors.joining(", "));
        
        logger.debug("服务调用开始: {}.{}", className, methodName);
        logger.debug("服务参数: {}", args);
        
        try {
            Object result = joinPoint.proceed();
            long elapsedTime = System.currentTimeMillis() - startTime;
            
            String resultJson = objectMapper.writeValueAsString(result);
            if (resultJson.length() > 500) {
                resultJson = resultJson.substring(0, 500) + "...[截断]";
            }
            
            logger.debug("服务调用成功: {}.{} -> 耗时: {}ms", className, methodName, elapsedTime);
            logger.debug("服务结果: {}", resultJson);
            
            return result;
        } catch (Throwable throwable) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.error("服务调用失败: {}.{} -> 耗时: {}ms, 异常: {}", 
                    className, methodName, elapsedTime, throwable.getMessage(), throwable);
            throw throwable;
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}