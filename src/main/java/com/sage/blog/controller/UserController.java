package com.sage.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sage.blog.common.Result;
import com.sage.blog.entity.User;
import com.sage.blog.model.ApiResponse;
import com.sage.blog.model.ProfileUpdateDto;
import com.sage.blog.model.PasswordUpdateDto;
import com.sage.blog.service.UserService;
import com.sage.blog.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 用户接口控制器
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    /**
     * 获取用户分页列表
     *
     * @param page     页码
     * @param size     每页数量
     * @param username 用户名（模糊查询）
     * @param status   状态
     * @return 用户分页数据
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<IPage<User>> getUserPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status) {
        IPage<User> userPage = userService.getUserPage(page, size, username, status);
        return Result.success(userPage);
    }

    /**
     * 获取用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.failed("用户不存在");
        }
        // 安全考虑，清除密码
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 获取当前用户信息
     *
     * @return 用户信息
     */
    @GetMapping("/info")
    public Result<User> getCurrentUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        logger.info("获取用户[{}]信息", username);

        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.failed("用户不存在");
        }

        // 安全起见，清除密码信息
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 获取用户个人资料
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> getUserProfile() {
        // 使用Spring Security的认证上下文
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ApiResponse.error("用户未登录");
        }

        // 获取认证用户的用户名
        String username = authentication.getName();

        // 通过用户名获取完整的用户信息
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }

        Map<String, Object> profileInfo = new HashMap<>();
        profileInfo.put("id", user.getId());
        profileInfo.put("username", user.getUsername());
        profileInfo.put("nickname", user.getNickname());
        profileInfo.put("email", user.getEmail());
        profileInfo.put("phone", user.getPhone());
        profileInfo.put("avatar", user.getAvatar());
        profileInfo.put("createTime", user.getCreateTime());

        return ApiResponse.success(profileInfo);
    }

    /**
     * 更新用户个人资料
     *
     * @param profileDto 个人资料数据
     * @return 更新结果
     */
    @PutMapping("/info")
    public Result<User> updateProfile(@RequestBody ProfileUpdateDto profileDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        logger.info("用户[{}]更新个人资料", username);

        User currentUser = userService.getUserByUsername(username);
        if (currentUser == null) {
            return Result.failed("用户不存在");
        }

        User updatedUser = userService.updateProfile(currentUser.getId(), profileDto);
        // 安全起见，清除密码信息
        updatedUser.setPassword(null);
        return Result.success(updatedUser);
    }

    /**
     * 更新用户密码
     *
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 更新结果
     */
    @PutMapping("/password")
    public Result<Void> updatePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        logger.info("用户[{}]更新密码", username);

        User currentUser = userService.getUserByUsername(username);
        if (currentUser == null) {
            return Result.failed("用户不存在");
        }

        boolean success = userService.updatePassword(currentUser.getId(), oldPassword, newPassword);
        if (success) {
            return Result.success();
        } else {
            return Result.failed("密码更新失败，可能是旧密码不正确");
        }
    }

    /**
     * 更新用户头像
     *
     * @param file 头像文件
     * @return 头像URL
     */
    @PostMapping("/avatar")
    public Result<String> updateAvatar(@RequestParam("file") MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        logger.info("用户[{}]更新头像", username);

        User currentUser = userService.getUserByUsername(username);
        if (currentUser == null) {
            return Result.failed("用户不存在");
        }

        try {
            String avatarUrl = userService.updateAvatar(currentUser.getId(), file);
            return Result.success(avatarUrl);
        } catch (Exception e) {
            logger.error("头像上传失败", e);
            return Result.failed("头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 分配用户角色
     *
     * @param userId  用户ID
     * @param roleIds 角色ID列表
     * @return 分配结果
     */
    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> assignRoles(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
        boolean success = userService.assignRoles(userId, roleIds);
        if (success) {
            return Result.success();
        } else {
            return Result.failed("分配角色失败");
        }
    }

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param status 状态
     * @return 更新结果
     */
    @PutMapping("/{userId}/status")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> updateStatus(@PathVariable Long userId, @RequestParam Integer status) {
        Long operatorId = getCurrentUserId();
        boolean success = userService.updateUserStatus(userId, status, operatorId, null);
        if (success) {
            return Result.success();
        } else {
            return Result.failed("更新状态失败");
        }
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public Result<Void> deleteUser(@PathVariable Long id) {
        boolean success = userService.removeById(id);
        if (success) {
            return Result.success();
        } else {
            return Result.failed("删除失败");
        }
    }

    /**
     * 获取当前登录用户名
     *
     * @return 用户名
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    private Long getCurrentUserId() {
        String username = getCurrentUsername();
        User user = userService.getUserByUsername(username);
        return user != null ? user.getId() : null;
    }
}