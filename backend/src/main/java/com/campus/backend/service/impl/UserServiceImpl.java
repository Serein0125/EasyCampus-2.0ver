package com.campus.backend.service.impl;

import com.campus.backend.common.ErrorCode;
import com.campus.backend.dto.UserRegisterDTO;
import com.campus.backend.dto.UserVO;
import com.campus.backend.entity.User;
import com.campus.backend.entity.VerificationCode;
import com.campus.backend.exception.BusinessException;
import com.campus.backend.exception.NotFoundException;
import com.campus.backend.mapper.UserMapper;
import com.campus.backend.mapper.VerificationCodeMapper;
import com.campus.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeMapper verificationCodeMapper;
    private final JavaMailSender mailSender;

    /** 发件人邮箱（从 spring.mail.username 配置注入，避免硬编码真实邮箱） */
    @Value("${spring.mail.username:}")
    private String mailFrom;

    /** 发送验证码频率限制：记录每个账号上次发送时间，防止短时间内重复发送 */
    private final Map<String, Long> resetCodeSendTimestamps = new ConcurrentHashMap<>();
    /** 同一账号两次发送验证码的最小间隔（毫秒） */
    private static final long RESET_CODE_INTERVAL_MS = 60_000L;

    @Override
    @Transactional
    public UserVO register(UserRegisterDTO dto) {
        // 检查用户名是否已存在
        if (userMapper.selectByUsername(dto.getUsername()) != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
        }

        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        if (user.getIsStudent() == null) {
            user.setIsStudent(true);
        }
        // 新注册用户默认角色为 USER
        if (user.getRole() == null) {
            user.setRole("USER");
        }
        userMapper.insert(user);
        log.info("新用户注册成功: username={}", user.getUsername());
        return convertToVO(user);
    }

    @Override
    public UserVO login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "账号已被禁用");
        }

        // 更新最后登录时间
        userMapper.updateLastLogin(user.getId(), LocalDateTime.now());
        return convertToVO(user);
    }

    @Override
    public UserVO getUserInfo(Long userId) {
        return convertToVO(getUserEntityById(userId));
    }

    @Override
    public UserVO getUserInfoByUsername(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new NotFoundException("用户不存在: " + username);
        }
        return convertToVO(user);
    }

    @Override
    @Transactional
    public UserVO updateProfile(Long userId, User profileData) {
        User existing = getUserEntityById(userId);
        // 只更新非null的字段，避免部分更新时覆盖已有数据
        if (profileData.getNickname() != null) existing.setNickname(profileData.getNickname());
        if (profileData.getAvatar() != null) existing.setAvatar(profileData.getAvatar());
        if (profileData.getGender() != null) existing.setGender(profileData.getGender());
        if (profileData.getSchool() != null) existing.setSchool(profileData.getSchool());
        if (profileData.getCampus() != null) existing.setCampus(profileData.getCampus());
        if (profileData.getMajor() != null) existing.setMajor(profileData.getMajor());
        if (profileData.getGrade() != null) existing.setGrade(profileData.getGrade());
        if (profileData.getWechat() != null) existing.setWechat(profileData.getWechat());
        if (profileData.getQq() != null) existing.setQq(profileData.getQq());
        if (profileData.getBio() != null) existing.setBio(profileData.getBio());
        userMapper.updateProfile(existing);
        return convertToVO(existing);
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = getUserEntityById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "旧密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updatePassword(user);
    }

    @Override
    @Transactional
    public void sendResetCode(String account) {
        // 频率限制：同一账号60秒内只能发送一次验证码
        Long lastTimestamp = resetCodeSendTimestamps.get(account);
        long now = System.currentTimeMillis();
        if (lastTimestamp != null && (now - lastTimestamp) < RESET_CODE_INTERVAL_MS) {
            long remainingSeconds = (RESET_CODE_INTERVAL_MS - (now - lastTimestamp)) / 1000 + 1;
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "操作太频繁，请" + remainingSeconds + "秒后再试");
        }

        // 查找用户，支持用户名或邮箱
        User user = userMapper.selectByUsername(account);
        if (user == null) {
            user = userMapper.selectByEmail(account);
        }
        if (user == null) {
            throw new NotFoundException("用户", account);
        }

        // 获取用户邮箱
        String email = user.getEmail();
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该用户未设置邮箱，无法发送验证码");
        }

        // 生成6位随机验证码
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));

        // 存储验证码到数据库，5分钟有效
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setAccount(email);
        verificationCode.setCode(code);
        verificationCode.setType("RESET_PASSWORD");
        verificationCode.setExpireAt(LocalDateTime.now().plusMinutes(5));
        verificationCodeMapper.insert(verificationCode);

        // 记录发送时间戳，用于频率限制
        resetCodeSendTimestamps.put(account, now);

        // 发送验证码邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject("校园社区平台 - 密码重置验证码");
            message.setText("您的验证码为: " + code + "\n\n验证码5分钟内有效，请勿泄露给他人。\n\n如非本人操作，请忽略此邮件。");
            mailSender.send(message);
            log.info("密码重置验证码已发送: email={}", email);
        } catch (Exception e) {
            // 邮件发送失败时清除时间戳限制，允许用户重试
            resetCodeSendTimestamps.remove(account);
            log.error("验证码邮件发送失败: email={}, error={}", email, e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "验证码发送失败，请稍后重试");
        }
    }

    @Override
    @Transactional
    public void verifyAndResetPassword(String account, String verifyCode, String newPassword) {
        // 查找用户，支持用户名或邮箱
        User user = userMapper.selectByUsername(account);
        if (user == null) {
            user = userMapper.selectByEmail(account);
        }
        if (user == null) {
            throw new NotFoundException("用户", account);
        }

        // 获取用户邮箱用于校验验证码
        String email = user.getEmail();
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该用户未设置邮箱，无法验证");
        }

        // 校验验证码格式
        if (verifyCode == null || verifyCode.length() != 6) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "验证码格式错误");
        }

        // 从数据库查询最新未使用的验证码
        VerificationCode storedCode = verificationCodeMapper.selectLatestUnused(email, "RESET_PASSWORD");
        if (storedCode == null) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "验证码已过期或未发送，请重新获取");
        }

        // 校验验证码是否匹配
        if (!storedCode.getCode().equals(verifyCode)) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "验证码错误");
        }

        // 标记验证码为已使用
        verificationCodeMapper.markUsed(storedCode.getId());

        // 重置密码
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updatePassword(user);
        log.info("密码重置成功: account={}", account);
    }

    @Override
    public User getUserEntityById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new NotFoundException("用户", userId);
        }
        return user;
    }

    @Override
    public java.util.List<User> searchUsers(String keyword, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return java.util.List.of();
        }
        int offset = (page - 1) * size;
        return userMapper.searchUsers(keyword.trim(), offset, size);
    }

    /**
     * Entity -> VO 转换 (排除敏感字段)
     */
    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        // 不复制 passwordHash 等敏感信息
        return vo;
    }
}
