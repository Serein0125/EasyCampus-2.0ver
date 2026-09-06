package com.campus.backend.exception;

import com.campus.backend.common.ErrorCode;

import java.util.Map;

public class ValidationException extends BusinessException {
    
    private final Map<String, String> fieldErrors;
    
    public ValidationException(Map<String, String> fieldErrors) {
        super(ErrorCode.VALIDATION_ERROR, "数据验证失败");
        this.fieldErrors = fieldErrors;
    }
    
    public ValidationException(String field, String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
        this.fieldErrors = Map.of(field, message);
    }
    
    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
        this.fieldErrors = Map.of();
    }
    
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
    
    public boolean hasFieldErrors() {
        return fieldErrors != null && !fieldErrors.isEmpty();
    }
}