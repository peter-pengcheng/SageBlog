package com.sage.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sage.blog.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}