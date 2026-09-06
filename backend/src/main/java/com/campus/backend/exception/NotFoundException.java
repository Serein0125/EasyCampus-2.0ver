package com.campus.backend.exception;

import com.campus.backend.common.ErrorCode;

public class NotFoundException extends BusinessException {
    
    private final String resourceType;
    private final Object resourceId;
    
    public NotFoundException(String resourceType, Object resourceId) {
        super(ErrorCode.NOT_FOUND, String.format("%s不存在: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
        this.resourceType = null;
        this.resourceId = null;
    }
    
    public NotFoundException(ErrorCode errorCode, String resourceType, Object resourceId) {
        super(errorCode, String.format("%s不存在: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    public String getResourceType() {
        return resourceType;
    }
    
    public Object getResourceId() {
        return resourceId;
    }
}