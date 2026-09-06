package com.campus.backend.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
    
    public void log(AuditLog auditLog) {
        try {
            // 这里可以扩展为将审计日志保存到数据库
            // 目前先记录到日志文件
            logger.info("审计日志: {}", auditLog);
            
            // 如果审计日志包含错误信息，记录到错误日志
            if (auditLog.getErrorMessage() != null) {
                logger.error("审计错误: {} - {}", auditLog.getOperation(), auditLog.getErrorMessage());
            }
            
            // 记录慢查询（执行时间超过1秒）
            if (auditLog.getExecutionTime() != null && auditLog.getExecutionTime() > 1000) {
                logger.warn("慢查询警告: {} - 耗时: {}ms", auditLog.getOperation(), auditLog.getExecutionTime());
            }
            
        } catch (Exception e) {
            logger.warn("记录审计日志失败: {}", e.getMessage());
        }
    }
    
    public void logSuccess(String userId, String username, String operation, String method, String url, 
                          String ip, String userAgent, String requestParams, String responseResult, 
                          Long executionTime) {
        AuditLog auditLog = new AuditLog(userId, username, operation, method, url);
        auditLog.setIp(ip);
        auditLog.setUserAgent(userAgent);
        auditLog.setRequestParams(requestParams);
        auditLog.setResponseResult(responseResult);
        auditLog.setExecutionTime(executionTime);
        auditLog.setStatus(200);
        
        log(auditLog);
    }
    
    public void logError(String userId, String username, String operation, String method, String url,
                        String ip, String userAgent, String requestParams, String errorMessage,
                        Long executionTime, Integer status) {
        AuditLog auditLog = new AuditLog(userId, username, operation, method, url);
        auditLog.setIp(ip);
        auditLog.setUserAgent(userAgent);
        auditLog.setRequestParams(requestParams);
        auditLog.setErrorMessage(errorMessage);
        auditLog.setExecutionTime(executionTime);
        auditLog.setStatus(status != null ? status : 500);
        
        log(auditLog);
    }
}