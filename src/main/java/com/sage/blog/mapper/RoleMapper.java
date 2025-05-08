package com.sage.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sage.blog.entity.Role;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}