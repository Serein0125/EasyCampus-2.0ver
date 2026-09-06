package com.campus.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库 users 表
 */
@Data
public class User {

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 密码哈希(BCrypt加密) */
    private String passwordHash;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 性别: 0未知, 1男, 2女 */
    private Integer gender;

    /** 学校名称 */
    private String school;

    /** 校区 */
    private String campus;

    /** 专业 */
    private String major;

    /** 年级 */
    private String grade;

    /** 微信号 */
    private String wechat;

    /** QQ号 */
    private String qq;

    /** 个人简介 */
    private String bio;

    /** 是否在校学生 */
    private Boolean isStudent;

    /** 状态: 0禁用, 1正常, 2封禁 */
    private Integer status;

    /** 角色: USER, ADMIN */
    private String role;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
