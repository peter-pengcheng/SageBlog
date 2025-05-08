package com.sage.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sage.blog.entity.SystemLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemLogMapper extends BaseMapper<SystemLog> {
}