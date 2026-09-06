package com.campus.backend.exception;

import com.campus.backend.common.ErrorCode;

public class AuthorizationException extends BusinessException {
    
    public AuthorizationException() {
        super(ErrorCode.FORBIDDEN);
    }
    
    public AuthorizationException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
    
    public AuthorizationException(Throwable cause) {
        super(ErrorCode.FORBIDDEN, cause);
    }
    
    public AuthorizationException(String message, Throwable cause) {
        super(ErrorCode.FORBIDDEN, message, cause);
    }
}