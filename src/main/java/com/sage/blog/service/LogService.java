package com.sage.blog.service;

import com.sage.blog.entity.SystemLog;

import java.util.List;

/**
 * 系统日志服务接口
 */
public interface LogService {

    /**
     * 添加日志
     * 
     * @param log 日志信息
     * @return 是否成功
     */
    boolean add(SystemLog log);

    /**
     * 获取日志总数
     * 
     * @return 日志总数
     */
    long count();

    /**
     * 获取最近的系统日志
     * 
     * @param limit 限制数量
     * @return 日志列表
     */
    List<SystemLog> getRecentLogs(int limit);

    /**
     * 分页获取日志列表
     * 
     * @param page 页码
     * @param size 每页大小
     * @return 日志列表
     */
    List<SystemLog> getPage(int page, int size);
}