package com.campus.backend.common;

import com.campus.backend.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        logger.warn("业务异常: {} - {}", e.getErrorCode(), e.getDetailMessage(), e);
        return Result.error(e.getErrorCode(), e.getDetailMessage());
    }
    
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNotFoundException(NotFoundException e, HttpServletRequest request) {
        logger.warn("资源未找到: {} - {}", e.getResourceType(), e.getResourceId());
        return Result.error(e.getErrorCode(), e.getDetailMessage());
    }
    
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidationException(ValidationException e, HttpServletRequest request) {
        logger.warn("数据验证失败: {}", e.getDetailMessage());
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("message", e.getDetailMessage());
        if (e.hasFieldErrors()) {
            errorData.put("fieldErrors", e.getFieldErrors());
        }
        return Result.error(e.getErrorCode(), e.getDetailMessage());
    }
    
    @ExceptionHandler(com.campus.backend.exception.AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleAuthenticationException(com.campus.backend.exception.AuthenticationException e, HttpServletRequest request) {
        logger.warn("认证失败: {}", e.getDetailMessage());
        return Result.error(e.getErrorCode(), e.getDetailMessage());
    }
    
    @ExceptionHandler(AuthorizationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleAuthorizationException(AuthorizationException e, HttpServletRequest request) {
        logger.warn("授权失败: {}", e.getDetailMessage());
        return Result.error(e.getErrorCode(), e.getDetailMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "验证失败"
                ));
        
        logger.warn("参数验证失败: {}", fieldErrors);
        return Result.error(ErrorCode.VALIDATION_ERROR, "请求参数验证失败");
    }
    
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e, HttpServletRequest request) {
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "验证失败"
                ));
        
        logger.warn("绑定异常: {}", fieldErrors);
        return Result.error(ErrorCode.VALIDATION_ERROR, "请求参数绑定失败");
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        Map<String, String> fieldErrors = e.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));
        
        logger.warn("约束违反异常: {}", fieldErrors);
        return Result.error(ErrorCode.VALIDATION_ERROR, "数据约束验证失败");
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleBadCredentialsException(BadCredentialsException e, HttpServletRequest request) {
        logger.warn("凭证错误: {}", e.getMessage());
        return Result.error(ErrorCode.INVALID_CREDENTIALS);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        logger.warn("访问被拒绝: {}", e.getMessage());
        return Result.error(ErrorCode.FORBIDDEN, "没有访问权限");
    }
    
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleSpringAuthenticationException(org.springframework.security.core.AuthenticationException e, HttpServletRequest request) {
        logger.warn("Spring Security认证异常: {}", e.getMessage());
        return Result.error(ErrorCode.UNAUTHORIZED, "认证失败");
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        logger.error("系统异常: {} - URI: {}", e.getMessage(), request.getRequestURI(), e);
        String detail = e.getMessage() != null ? e.getMessage() : "系统内部错误";
        if (detail.length() > 200) {
            detail = detail.substring(0, 200) + "...";
        }
        return Result.error(ErrorCode.INTERNAL_SERVER_ERROR, detail);
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        logger.error("运行时异常: {} - URI: {}", e.getMessage(), request.getRequestURI(), e);
        String detail = e.getMessage() != null ? e.getMessage() : "系统运行时错误";
        if (detail.length() > 200) {
            detail = detail.substring(0, 200) + "...";
        }
        return Result.error(ErrorCode.INTERNAL_SERVER_ERROR, detail);
    }
}