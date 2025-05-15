package com.sage.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sage.blog.entity.SystemLog;
import com.sage.blog.mapper.SystemLogMapper;
import com.sage.blog.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统日志服务实现类
 */
@Service
public class LogServiceImpl implements LogService {

    @Autowired
    private SystemLogMapper systemLogMapper;

    @Override
    public boolean add(SystemLog log) {
        return systemLogMapper.insert(log) > 0;
    }

    @Override
    public long count() {
        return systemLogMapper.selectCount(null);
    }

    @Override
    public List<SystemLog> getRecentLogs(int limit) {
        QueryWrapper<SystemLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        Page<SystemLog> page = new Page<>(1, limit);
        return systemLogMapper.selectPage(page, queryWrapper).getRecords();
    }

    @Override
    public List<SystemLog> getPage(int page, int size) {
        Page<SystemLog> pageParam = new Page<>(page, size);
        QueryWrapper<SystemLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        return systemLogMapper.selectPage(pageParam, queryWrapper).getRecords();
    }
}