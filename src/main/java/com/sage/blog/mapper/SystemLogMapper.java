package com.sage.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sage.blog.entity.SystemLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统日志Mapper接口
 */
@Mapper
public interface SystemLogMapper extends BaseMapper<SystemLog> {
}