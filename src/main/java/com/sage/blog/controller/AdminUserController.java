package com.sage.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sage.blog.common.Result;
import com.sage.blog.entity.User;
import com.sage.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 后台管理用户控制器
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') and hasAuthority('system:user')")
public class AdminUserController {

    @Autowired
    private UserService userService;

    /**
     * 获取用户列表（分页）
     */
    @GetMapping
    public Result<IPage<User>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status) {
        IPage<User> userPage = userService.getUserPage(page, size, username, status);
        return Result.success(userPage);
    }

    /**
     * 获取单个用户详情
     */
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return Result.failed("用户不存在");
        }
        return Result.success(user);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    public Result<Boolean> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> userMap) {
        User user = userService.findById(id);
        if (user == null) {
            return Result.failed("用户不存在");
        }

        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userService.findByUsername(currentUsername);
        if (currentUser == null) {
            return Result.failed("未获取到当前登录用户信息");
        }

        // 更新用户信息
        if (userMap.containsKey("nickname")) {
            user.setNickname((String) userMap.get("nickname"));
        }
        if (userMap.containsKey("email")) {
            user.setEmail((String) userMap.get("email"));
        }
        if (userMap.containsKey("status")) {
            Integer status = Integer.parseInt(userMap.get("status").toString());
            String remark = userMap.containsKey("remark") ? (String) userMap.get("remark") : "";
            return Result.success(userService.updateUserStatus(id, status, currentUser.getId(), remark));
        }

        boolean updated = userService.updateById(user);
        return Result.success(updated);
    }

    /**
     * 分配用户角色
     */
    @PostMapping("/{id}/roles")
    public Result<Boolean> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        User user = userService.findById(id);
        if (user == null) {
            return Result.failed("用户不存在");
        }

        boolean success = userService.assignRoles(id, roleIds);
        return Result.success(success);
    }
}