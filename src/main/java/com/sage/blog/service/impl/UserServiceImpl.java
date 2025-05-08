package com.sage.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sage.blog.entity.PasswordResetToken;
import com.sage.blog.entity.RegisterAudit;
import com.sage.blog.entity.User;
import com.sage.blog.entity.UserRole;
import com.sage.blog.mapper.PasswordResetTokenMapper;
import com.sage.blog.mapper.RegisterAuditMapper;
import com.sage.blog.mapper.UserMapper;
import com.sage.blog.mapper.UserRoleMapper;
import com.sage.blog.model.ProfileUpdateDto;
import com.sage.blog.service.EmailService;
import com.sage.blog.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RegisterAuditMapper registerAuditMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private PasswordResetTokenMapper passwordResetTokenMapper;

    @Autowired
    private EmailService emailService;

    @Value("${sageblog.password-reset.expiration}")
    private int resetTokenExpiration;

    @Value("${sageblog.file.upload-dir}")
    private String uploadDir;

    @Override
    public User findById(Long id) {
        return getById(id);
    }

    @Override
    public User findByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return getOne(queryWrapper);
    }

    @Override
    public User findByEmail(String email) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        return getOne(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean register(User user) {
        // 设置初始状态为待审核
        user.setStatus(0);
        // 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 保存用户信息
        boolean result = save(user);
        if (!result) {
            return false;
        }

        // 创建审核记录
        RegisterAudit audit = new RegisterAudit();
        audit.setUserId(user.getId());
        audit.setStatus(0); // 待审核
        registerAuditMapper.insert(audit);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateProfile(Long userId, ProfileUpdateDto profile) {
        User user = getById(userId);
        if (user == null) {
            return null;
        }

        // 更新用户信息
        if (StringUtils.isNotBlank(profile.getNickname())) {
            user.setNickname(profile.getNickname());
        }
        if (StringUtils.isNotBlank(profile.getEmail())) {
            user.setEmail(profile.getEmail());
        }
        if (StringUtils.isNotBlank(profile.getPhone())) {
            user.setPhone(profile.getPhone());
        }

        user.setUpdateTime(LocalDateTime.now());
        updateById(user);

        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = getById(userId);
        if (user == null) {
            return false;
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return false;
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        return updateById(user);
    }

    @Override
    public String updateAvatar(Long userId, MultipartFile file) {
        User user = getById(userId);
        if (user == null || file.isEmpty()) {
            return null;
        }

        try {
            // 确保上传目录存在
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = "avatar_" + userId + "_" + UUID.randomUUID().toString() + extension;

            // 保存文件
            Path filePath = Paths.get(uploadDir, newFilename);
            Files.copy(file.getInputStream(), filePath);

            // 更新用户头像路径
            String avatarUrl = "/uploads/" + newFilename;
            user.setAvatar(avatarUrl);
            user.setUpdateTime(LocalDateTime.now());
            updateById(user);

            return avatarUrl;
        } catch (IOException e) {
            logger.error("上传头像失败", e);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserStatus(Long userId, Integer status, Long operatorId, String remark) {
        // 更新用户状态
        User user = getById(userId);
        if (user == null) {
            return false;
        }

        user.setStatus(status);
        boolean result = updateById(user);
        if (!result) {
            return false;
        }

        // 更新审核记录
        LambdaQueryWrapper<RegisterAudit> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RegisterAudit::getUserId, userId)
                .orderByDesc(RegisterAudit::getCreateTime)
                .last("LIMIT 1");
        RegisterAudit audit = registerAuditMapper.selectOne(queryWrapper);

        if (audit != null) {
            audit.setStatus(status);
            audit.setAuditorId(operatorId);
            audit.setRemark(remark);
            audit.setAuditTime(LocalDateTime.now());
            registerAuditMapper.updateById(audit);
        }

        return true;
    }

    @Override
    public IPage<User> getUserPage(int page, int size, String username, Integer status) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        // 添加查询条件
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like(User::getUsername, username);
        }

        if (status != null) {
            queryWrapper.eq(User::getStatus, status);
        }

        // 排序
        queryWrapper.orderByDesc(User::getCreateTime);

        return page(pageParam, queryWrapper);
    }

    @Override
    public IPage<User> getPendingAuditUserPage(int page, int size) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        // 查询待审核用户
        queryWrapper.eq(User::getStatus, 0).orderByAsc(User::getCreateTime);

        return page(pageParam, queryWrapper);
    }

    @Override
    public User getUserByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return getOne(queryWrapper);
    }

    @Override
    public User getUserByEmail(String email) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        return getOne(queryWrapper);
    }

    @Override
    public boolean updateLastLoginTime(Long userId) {
        User user = getById(userId);
        if (user == null) {
            return false;
        }

        user.setLastLoginTime(LocalDateTime.now());
        return updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRoles(Long userId, List<Long> roleIds) {
        // 先删除用户已有的角色
        LambdaQueryWrapper<UserRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRole::getUserId, userId);
        userRoleMapper.delete(queryWrapper);

        // 保存新的角色关系
        if (roleIds != null && !roleIds.isEmpty()) {
            List<UserRole> userRoles = new ArrayList<>();
            for (Long roleId : roleIds) {
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoles.add(userRole);
            }

            for (UserRole userRole : userRoles) {
                userRoleMapper.insert(userRole);
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createPasswordResetToken(String email) {
        // 通过邮箱获取用户
        User user = findByEmail(email);
        if (user == null) {
            return null;
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

        // 发送密码重置邮件
        boolean emailSent = emailService.sendPasswordResetEmail(user.getEmail(), token, user.getUsername());

        return emailSent ? token : null;
    }

    @Override
    public Long validatePasswordResetToken(String token) {
        QueryWrapper<PasswordResetToken> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("token", token)
                .eq("status", 0); // 未使用

        PasswordResetToken resetToken = passwordResetTokenMapper.selectOne(queryWrapper);

        // 检查令牌是否存在且未过期
        if (resetToken == null || resetToken.isExpired()) {
            return null;
        }

        return resetToken.getUserId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(String token, String newPassword) {
        // 验证令牌并获取用户ID
        Long userId = validatePasswordResetToken(token);
        if (userId == null) {
            return false;
        }

        // 获取令牌对象
        QueryWrapper<PasswordResetToken> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("token", token)
                .eq("status", 0); // 未使用
        PasswordResetToken resetToken = passwordResetTokenMapper.selectOne(queryWrapper);

        // 获取用户
        User user = getById(userId);
        if (user == null) {
            return false;
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        boolean updated = updateById(user);

        // 标记令牌为已使用
        if (updated) {
            resetToken.setStatus(1);
            passwordResetTokenMapper.updateById(resetToken);
            return true;
        }

        return false;
    }
}