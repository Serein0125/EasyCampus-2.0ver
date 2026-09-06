package com.campus.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 验证码实体类
 * 用于密码重置等场景的验证码存储与校验
 */
@Data
public class VerificationCode {

    /** 主键ID */
    private Long id;

    /** 关联的邮箱或手机号 */
    private String account;

    /** 6位数字验证码 */
    private String code;

    /** 验证码类型: RESET_PASSWORD */
    private String type;

    /** 是否已使用: 0未使用, 1已使用 */
    private Integer used;

    /** 过期时间 */
    private LocalDateTime expireAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
