package com.campus.backend.exception;

import com.campus.backend.common.ErrorCode;

public class BusinessException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final String detailMessage;
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detailMessage = errorCode.getMessage();
    }
    
    public BusinessException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }
    
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.detailMessage = errorCode.getMessage();
    }
    
    public BusinessException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(detailMessage, cause);
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public String getDetailMessage() {
        return detailMessage;
    }
    
    public int getCode() {
        return errorCode.getCode();
    }
    
    @Override
    public String toString() {
        return String.format("BusinessException{code=%d, message='%s', detail='%s'}",
                errorCode.getCode(), errorCode.getMessage(), detailMessage);
    }
}