package com.sage.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sage.blog.entity.Role;
import com.sage.blog.entity.RolePermission;
import com.sage.blog.entity.UserRole;
import com.sage.blog.mapper.RoleMapper;
import com.sage.blog.mapper.RolePermissionMapper;
import com.sage.blog.mapper.UserRoleMapper;
import com.sage.blog.service.RoleService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Override
    public IPage<Role> getRolePage(int page, int size, String name) {
        Page<Role> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();

        // 添加查询条件
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like(Role::getName, name);
        }

        // 排序
        queryWrapper.orderByAsc(Role::getCreateTime);

        return page(pageParam, queryWrapper);
    }

    @Override
    public List<Role> getRolesByUserId(Long userId) {
        // 先查询用户角色关系
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);

        // 再查询角色信息
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        return listByIds(roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignPermissions(Long roleId, List<Long> permissionIds) {
        // 先删除角色已有的权限
        LambdaQueryWrapper<RolePermission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RolePermission::getRoleId, roleId);
        rolePermissionMapper.delete(queryWrapper);

        // 保存新的权限关系
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<RolePermission> rolePermissions = permissionIds.stream().map(permissionId -> {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(roleId);
                rolePermission.setPermissionId(permissionId);
                return rolePermission;
            }).collect(Collectors.toList());

            for (RolePermission rolePermission : rolePermissions) {
                rolePermissionMapper.insert(rolePermission);
            }
        }

        return true;
    }

    @Override
    public Role getRoleByCode(String code) {
        QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", code);
        return getOne(queryWrapper);
    }
}