package com.sage.blog.controller;

import com.sage.blog.common.Result;
import com.sage.blog.dto.DashboardStatsDTO;
import com.sage.blog.entity.User;
import com.sage.blog.entity.SystemLog;
import com.sage.blog.service.LogService;
import com.sage.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理API控制器
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private LogService logService;

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/dashboard/stats")
    public Result<DashboardStatsDTO> getDashboardStats() {
        // 统计用户数据
        long totalUsers = userService.count();
        long pendingUsers = userService.countByStatus(0);

        // 统计今日登录数
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        long todayLogins = userService.countLoginsBetween(todayStart, todayEnd);

        // 统计日志数
        long totalLogs = logService.count();

        // 封装统计数据
        DashboardStatsDTO stats = new DashboardStatsDTO();
        stats.setTotalUsers(totalUsers);
        stats.setPendingUsers(pendingUsers);
        stats.setTodayLogins(todayLogins);
        stats.setTotalLogs(totalLogs);

        return Result.success(stats);
    }

    /**
     * 获取最近注册的用户
     */
    @GetMapping("/dashboard/recent-users")
    public Result<List<User>> getRecentUsers() {
        List<User> users = userService.getRecentUsers(5);
        return Result.success(users);
    }

    /**
     * 获取最近的系统日志
     */
    @GetMapping("/dashboard/recent-logs")
    public Result<List<SystemLog>> getRecentLogs() {
        List<SystemLog> logs = logService.getRecentLogs(5);
        return Result.success(logs);
    }
}