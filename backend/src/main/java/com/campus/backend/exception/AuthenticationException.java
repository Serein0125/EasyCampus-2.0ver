package com.campus.backend.exception;

import com.campus.backend.common.ErrorCode;

public class AuthenticationException extends BusinessException {
    
    public AuthenticationException() {
        super(ErrorCode.UNAUTHORIZED);
    }
    
    public AuthenticationException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
    
    public AuthenticationException(Throwable cause) {
        super(ErrorCode.UNAUTHORIZED, cause);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(ErrorCode.UNAUTHORIZED, message, cause);
    }
}