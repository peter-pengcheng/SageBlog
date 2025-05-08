package com.sage.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sage.blog.entity.PasswordResetToken;
import com.sage.blog.entity.Role;
import com.sage.blog.entity.User;
import com.sage.blog.mapper.PasswordResetTokenMapper;
import com.sage.blog.security.JwtTokenProvider;
import com.sage.blog.service.AuthService;
import com.sage.blog.service.EmailService;
import com.sage.blog.service.RoleService;
import com.sage.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetTokenMapper passwordResetTokenMapper;

    @Value("${sageblog.init.admin.email}")
    private String adminEmail;

    @Value("${sageblog.password-reset.expiration}")
    private int resetTokenExpiration;

    @Value("${sageblog.password-reset.url}")
    private String resetPasswordBaseUrl;

    @Override
    public String login(String username, String password) {
        // 先查询用户状态
        User user = userService.getUserByUsername(username);
        if (user != null && user.getStatus() == 0) {
            throw new org.springframework.security.authentication.DisabledException("您的账号正在审核中，请等待管理员审核通过后再尝试登录");
        }

        // 认证用户
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));

        // 设置认证信息到上下文
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 生成JWT令牌
        String jwt = tokenProvider.generateToken(authentication);

        // 更新用户最后登录时间
        userService.updateLastLoginTime(user != null ? user.getId() : userService.getUserByUsername(username).getId());

        return jwt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean register(User user) {
        // 检查用户名是否已存在
        if (userService.getUserByUsername(user.getUsername()) != null) {
            return false;
        }

        // 检查邮箱是否已存在
        if (userService.getUserByEmail(user.getEmail()) != null) {
            return false;
        }

        // 注册用户
        boolean result = userService.register(user);

        if (result) {
            try {
                // 发送注册成功邮件
                emailService.sendRegisterSuccessMail(user.getEmail(), user.getUsername());

                // 发送通知给管理员
                emailService.sendPendingAuditNotification(adminEmail, user.getUsername());
            } catch (Exception e) {
                // 捕获邮件发送异常，但不影响注册流程
                logger.error("注册邮件发送失败，但用户注册成功。用户名: {}, 邮箱: {}", user.getUsername(), user.getEmail(), e);
                // 在此处可以添加其他日志记录或报警机制
            }
        }

        return result;
    }

    @Override
    public String refreshToken(String token) {
        // 验证原令牌
        if (!tokenProvider.validateToken(token)) {
            return null;
        }

        // 获取认证信息
        Authentication authentication = tokenProvider.getAuthentication(token);

        // 生成新令牌
        return tokenProvider.generateToken(authentication);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean forgotPassword(String email) {
        // 通过邮箱获取用户
        User user = userService.getUserByEmail(email);
        if (user == null) {
            return false;
        }

        // 生成唯一令牌
        String token = UUID.randomUUID().toString();

        // 设置过期时间（默认24小时）
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(resetTokenExpiration);

        // 保存令牌到数据库
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(user.getId());
        resetToken.setToken(token);
        resetToken.setExpiryDate(expiryDate);
        resetToken.setCreateTime(LocalDateTime.now());
        resetToken.setStatus(0); // 未使用

        passwordResetTokenMapper.insert(resetToken);

        // 构建重置URL
        String resetUrl = resetPasswordBaseUrl + "?token=" + token;

        try {
            // 发送密码重置邮件
            return emailService.sendPasswordResetMail(user.getEmail(), user.getUsername(), token, resetUrl);
        } catch (Exception e) {
            // 捕获邮件发送异常，但令牌已经生成
            logger.error("密码重置邮件发送失败，但重置令牌已生成。用户名: {}, 邮箱: {}", user.getUsername(), user.getEmail(), e);
            return true; // 返回true表示流程基本成功，虽然邮件可能未发送
        }
    }

    @Override
    public boolean validatePasswordResetToken(String token) {
        QueryWrapper<PasswordResetToken> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("token", token)
                .eq("status", 0); // 未使用

        PasswordResetToken resetToken = passwordResetTokenMapper.selectOne(queryWrapper);

        // 检查令牌是否存在且未过期
        return resetToken != null && !resetToken.isExpired();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(String token, String password) {
        QueryWrapper<PasswordResetToken> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("token", token)
                .eq("status", 0); // 未使用

        PasswordResetToken resetToken = passwordResetTokenMapper.selectOne(queryWrapper);

        // 检查令牌是否存在且未过期
        if (resetToken == null || resetToken.isExpired()) {
            return false;
        }

        // 获取用户
        User user = userService.getById(resetToken.getUserId());
        if (user == null) {
            return false;
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(password));
        user.setUpdateTime(LocalDateTime.now());
        userService.updateById(user);

        // 标记令牌为已使用
        resetToken.setStatus(1);
        passwordResetTokenMapper.updateById(resetToken);

        return true;
    }
}