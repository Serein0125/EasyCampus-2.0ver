package com.campus.backend.controller;

import com.campus.backend.common.Result;
import com.campus.backend.common.SecurityUtils;
import com.campus.backend.config.JwtUtil;
import com.campus.backend.dto.*;
import com.campus.backend.entity.User;
import com.campus.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/users")
@Tag(name = "用户管理", description = "注册、登录、个人信息")
@RequiredArgsConstructor
public class UserApi {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Map<String, Object>> register(@Valid @RequestBody UserRegisterDTO dto) {
        UserVO userVO = userService.register(dto);
        String token = jwtUtil.generateToken(userVO.getId(), userVO.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("user", userVO);
        result.put("token", token);
        return Result.success("注册成功", result);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<Map<String, Object>> login(@Valid @RequestBody UserLoginDTO dto) {
        UserVO userVO = userService.login(dto.getUsername(), dto.getPassword());
        String token = jwtUtil.generateToken(userVO.getId(), userVO.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("user", userVO);
        result.put("token", token);
        return Result.success("登录成功", result);
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Result<UserVO> getUserInfo() {
        return Result.success(userService.getUserInfo(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取指定用户公开信息")
    public Result<UserVO> getUserPublic(@PathVariable Long id) {
        return Result.success(userService.getUserInfo(id));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新个人资料")
    public Result<UserVO> updateProfile(@RequestBody User profileData) {
        Long userId = SecurityUtils.getCurrentUserId();
        User safeData = new User();
        safeData.setNickname(profileData.getNickname());
        safeData.setAvatar(profileData.getAvatar());
        safeData.setGender(profileData.getGender());
        safeData.setSchool(profileData.getSchool());
        safeData.setCampus(profileData.getCampus());
        safeData.setMajor(profileData.getMajor());
        safeData.setGrade(profileData.getGrade());
        safeData.setWechat(profileData.getWechat());
        safeData.setQq(profileData.getQq());
        safeData.setBio(profileData.getBio());
        return Result.success(userService.updateProfile(userId, safeData));
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public Result<Void> updatePassword(@RequestBody Map<String, String> body) {
        userService.updatePassword(
                SecurityUtils.getCurrentUserId(),
                body.get("oldPassword"),
                body.get("newPassword")
        );
        return Result.success("密码修改成功");
    }

    @PostMapping("/reset-password/send-code")
    @Operation(summary = "发送重置密码验证码")
    public Result<Void> sendResetCode(@RequestBody Map<String, String> body) {
        String account = body.get("account");
        if (account == null || account.trim().isEmpty()) {
            return Result.error("请提供用户名或邮箱");
        }
        userService.sendResetCode(account);
        return Result.success("验证码已发送");
    }

    @PostMapping("/reset-password/verify")
    @Operation(summary = "验证并重置密码")
    public Result<Void> verifyAndResetPassword(@RequestBody Map<String, String> body) {
        String account = body.get("account");
        String verifyCode = body.get("verifyCode");
        String newPassword = body.get("newPassword");

        if (account == null || verifyCode == null || newPassword == null) {
            return Result.error("参数不完整");
        }
        userService.verifyAndResetPassword(account, verifyCode, newPassword);
        return Result.success("密码重置成功");
    }

    @GetMapping("/search")
    @Operation(summary = "搜索用户（按ID/账号/昵称）")
    public Result<java.util.List<com.campus.backend.entity.User>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(userService.searchUsers(keyword, page, size));
    }
}