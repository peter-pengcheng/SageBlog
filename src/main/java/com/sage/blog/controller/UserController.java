package com.sage.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sage.blog.common.Result;
import com.sage.blog.entity.User;
import com.sage.blog.model.ApiResponse;
import com.sage.blog.model.ProfileUpdateDto;
import com.sage.blog.model.PasswordUpdateDto;
import com.sage.blog.service.UserService;
import com.sage.blog.util.SecurityUtils;
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
@RequestMapping("/api/user")
public class UserController {

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
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> getCurrentUserInfo() {
        User user = SecurityUtils.getCurrentUser();
        if (user == null) {
            return ApiResponse.error("用户未登录");
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("email", user.getEmail());
        userInfo.put("avatar", user.getAvatar());
        // 临时注释掉这行，直到实现获取用户角色的功能
        // userInfo.put("roles", user.getRoles());

        return ApiResponse.success(userInfo);
    }

    /**
     * 获取用户个人资料
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> getUserProfile() {
        User user = SecurityUtils.getCurrentUser();
        if (user == null) {
            return ApiResponse.error("用户未登录");
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
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> updateProfile(@Valid @RequestBody ProfileUpdateDto profileDto) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.error("用户未登录");
        }

        try {
            userService.updateProfile(currentUser.getId(), profileDto);
            return ApiResponse.success("个人资料更新成功");
        } catch (Exception e) {
            return ApiResponse.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> updatePassword(@Valid @RequestBody PasswordUpdateDto passwordDto) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.error("用户未登录");
        }

        try {
            userService.updatePassword(currentUser.getId(), passwordDto.getOldPassword(), passwordDto.getNewPassword());
            return ApiResponse.success("密码修改成功");
        } catch (Exception e) {
            return ApiResponse.error("密码修改失败: " + e.getMessage());
        }
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.error("用户未登录");
        }

        try {
            String avatarUrl = userService.updateAvatar(currentUser.getId(), file);

            Map<String, String> result = new HashMap<>();
            result.put("avatar", avatarUrl);

            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("头像上传失败: " + e.getMessage());
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