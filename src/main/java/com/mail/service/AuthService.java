package com.mail.service;

import com.mail.dto.request.ChangePasswordRequest;
import com.mail.dto.request.LoginRequest;
import com.mail.dto.request.RegisterRequest;
import com.mail.dto.response.AuthResponse;
import com.mail.entity.User;
import com.mail.mapper.UserMapper;
import com.mail.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务（v2.0：密码哈希已改用 BCrypt）。
 *
 * ⚠️ 破坏性变更说明：
 * 旧版本使用自定义 SHA-256 + salt 方案（PasswordUtil），
 * 切换到 BCrypt 后，所有已注册用户的旧密码将无法验证，需要重新注册。
 * 如需平滑迁移，可编写一次性脚本读取旧哈希进行迁移。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthResponse register(RegisterRequest req) {
        log.info("用户注册请求: username={}", req.getUsername());
        if (userMapper.findByUsername(req.getUsername()) != null) {
            log.warn("注册失败，用户名已存在: username={}", req.getUsername());
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userMapper.findByEmail(req.getEmail()) != null) {
            log.warn("注册失败，邮箱已被注册: email={}", req.getEmail());
            throw new IllegalArgumentException("邮箱已被注册");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        // BCrypt 内置 salt，不再需要单独的 salt 字段
        user.setSalt(null);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        userMapper.insert(user);
        log.info("新用户注册成功: username={}", req.getUsername());
        String token = jwtUtil.generateToken(user.getId());
        return AuthResponse.of(token, user.getId(), user.getUsername(), user.getEmail(), user.getAvatar(), user.getQqEmail());
    }

    public AuthResponse login(LoginRequest req) {
        log.info("用户登录请求: email={}", req.getEmail());
        User user = userMapper.findByEmail(req.getEmail());
        if (user == null) {
            log.warn("登录失败，邮箱不存在: email={}", req.getEmail());
            throw new IllegalArgumentException("邮箱或密码错误");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            log.warn("登录失败，密码错误: email={}", req.getEmail());
            throw new IllegalArgumentException("邮箱或密码错误");
        }
        log.info("用户登录成功: email={}", req.getEmail());
        String token = jwtUtil.generateToken(user.getId());
        return AuthResponse.of(token, user.getId(), user.getUsername(), user.getEmail(), user.getAvatar(), user.getQqEmail());
    }

    public void changePassword(Long userId, ChangePasswordRequest req) {
        User user = userMapper.findById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("旧密码错误");
        }
        if (req.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("新密码至少6个字符");
        }
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updatePassword(userId, user.getPasswordHash());
        log.info("用户修改密码成功: userId={}", userId);
    }

    public AuthResponse updateUsername(Long userId, String newUsername) {
        if (newUsername == null || newUsername.trim().length() < 3) {
            throw new IllegalArgumentException("用户名至少3个字符");
        }
        User existing = userMapper.findByUsername(newUsername.trim());
        if (existing != null && !existing.getId().equals(userId)) {
            throw new IllegalArgumentException("用户名已被使用");
        }
        userMapper.updateUsername(userId, newUsername.trim());
        User user = userMapper.findById(userId);
        String token = jwtUtil.generateToken(user.getId());
        log.info("用户修改用户名成功: userId={}, newUsername={}", userId, newUsername);
        return AuthResponse.of(token, user.getId(), user.getUsername(), user.getEmail(), user.getAvatar(), user.getQqEmail());
    }
}
