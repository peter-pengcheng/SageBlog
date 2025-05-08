package com.sage.blog.controller;

import com.sage.blog.common.Result;
import com.sage.blog.dto.UpdatePasswordRequest;
import com.sage.blog.dto.UserProfileRequest;
import com.sage.blog.entity.User;
import com.sage.blog.service.FileService;
import com.sage.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;

/**
 * 用户个人中心控制器
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileService fileService;

    /**
     * 获取当前登录用户的个人信息
     *
     * @return 用户信息
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<User> getCurrentUserProfile() {
        String username = getCurrentUsername();
        User user = userService.getUserByUsername(username);
        if (user != null) {
            // 安全考虑，不返回密码
            user.setPassword(null);
            return Result.success(user);
        }
        return Result.failed("获取用户信息失败");
    }

    /**
     * 更新当前登录用户的个人信息
     *
     * @param profileRequest 更新请求
     * @return 更新结果
     */
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public Result<Void> updateUserProfile(@Valid @RequestBody UserProfileRequest profileRequest) {
        String username = getCurrentUsername();
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.failed("用户不存在");
        }

        // 更新用户信息
        user.setNickname(profileRequest.getNickname());
        user.setEmail(profileRequest.getEmail());
        user.setPhone(profileRequest.getPhone());

        boolean success = userService.updateById(user);
        if (success) {
            return Result.success();
        }
        return Result.failed("更新用户信息失败");
    }

    /**
     * 更新当前登录用户的密码
     *
     * @param updatePasswordRequest 更新密码请求
     * @return 更新结果
     */
    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest updatePasswordRequest) {
        String username = getCurrentUsername();
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.failed("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(updatePasswordRequest.getOldPassword(), user.getPassword())) {
            return Result.validateFailed("旧密码不正确");
        }

        // 验证新密码与确认密码是否一致
        if (!updatePasswordRequest.getNewPassword().equals(updatePasswordRequest.getConfirmPassword())) {
            return Result.validateFailed("两次输入的新密码不一致");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(updatePasswordRequest.getNewPassword()));
        boolean success = userService.updateById(user);
        if (success) {
            return Result.success();
        }
        return Result.failed("更新密码失败");
    }

    /**
     * 上传用户头像
     *
     * @param file 头像文件
     * @return 上传结果
     */
    @PostMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String username = getCurrentUsername();
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.failed("用户不存在");
        }

        try {
            // 上传文件并获取URL
            String avatarUrl = fileService.uploadAvatar(file, username);

            // 更新用户头像URL
            user.setAvatar(avatarUrl);
            userService.updateById(user);

            return Result.success(avatarUrl);
        } catch (IOException e) {
            return Result.failed("头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户名
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }

        return authentication.getName();
    }
}