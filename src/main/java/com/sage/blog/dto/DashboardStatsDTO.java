package com.sage.blog.dto;

import lombok.Data;

/**
 * 仪表盘统计数据传输对象
 */
@Data
public class DashboardStatsDTO {

    /**
     * 总用户数
     */
    private long totalUsers;

    /**
     * 待审核用户数
     */
    private long pendingUsers;

    /**
     * 今日登录数
     */
    private long todayLogins;

    /**
     * 系统日志数
     */
    private long totalLogs;
}