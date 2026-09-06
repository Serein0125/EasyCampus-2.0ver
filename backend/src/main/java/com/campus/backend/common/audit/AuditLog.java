package com.campus.backend.common.audit;

import java.time.LocalDateTime;

public class AuditLog {
    
    private Long id;
    private String userId;
    private String username;
    private String operation;
    private String method;
    private String url;
    private String ip;
    private String userAgent;
    private String requestParams;
    private String responseResult;
    private Long executionTime;
    private Integer status;
    private String errorMessage;
    private LocalDateTime createdAt;
    
    public AuditLog() {
        this.createdAt = LocalDateTime.now();
    }
    
    public AuditLog(String userId, String username, String operation, String method, String url) {
        this();
        this.userId = userId;
        this.username = username;
        this.operation = operation;
        this.method = method;
        this.url = url;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getIp() {
        return ip;
    }
    
    public void setIp(String ip) {
        this.ip = ip;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getRequestParams() {
        return requestParams;
    }
    
    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams;
    }
    
    public String getResponseResult() {
        return responseResult;
    }
    
    public void setResponseResult(String responseResult) {
        this.responseResult = responseResult;
    }
    
    public Long getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return String.format(
                "AuditLog{userId='%s', username='%s', operation='%s', method='%s', url='%s', ip='%s', status=%d, time=%dms}",
                userId, username, operation, method, url, ip, status, executionTime);
    }
}