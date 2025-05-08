package com.sage.blog.util;

import com.sage.blog.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户
     *
     * @return 当前登录用户，如果未登录则返回null
     */
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }

        return null;
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 当前登录用户ID，如果未登录则返回null
     */
    public static Long getCurrentUserId() {
        User currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getId() : null;
    }

    /**
     * 检查用户是否已登录
     *
     * @return 是否已登录
     */
    public static boolean isAuthenticated() {
        return getCurrentUser() != null;
    }
}