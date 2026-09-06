package com.campus.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户信息VO (返回前端，不含敏感信息)
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private Integer gender;
    private String school;
    private String campus;
    private String major;
    private String grade;
    private String wechat;
    private String qq;
    private String bio;
    private Boolean isStudent;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    /** 获取状态文本描述 */
    public String getStatusText() {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "已禁用";
            case 1 -> "正常";
            case 2 -> "已封禁";
            default -> "未知";
        };
    }

    /** 获取性别文本描述 */
    public String getGenderText() {
        if (gender == null) return "未设置";
        return switch (gender) {
            case 1 -> "男";
            case 2 -> "女";
            default -> "未设置";
        };
    }
}
