package com.sage.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sage.blog.entity.Permission;
import com.sage.blog.mapper.PermissionMapper;
import com.sage.blog.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    @Autowired
    private PermissionMapper permissionMapper;

    @Override
    public IPage<Permission> getPermissionPage(int page, int size, String name) {
        Page<Permission> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Permission> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            queryWrapper.like(Permission::getName, name);
        }
        queryWrapper.orderByAsc(Permission::getSort);
        return page(pageParam, queryWrapper);
    }

    @Override
    public Permission getById(Long id) {
        return super.getById(id);
    }

    @Override
    public Permission getByCode(String code) {
        LambdaQueryWrapper<Permission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Permission::getCode, code);
        return getOne(queryWrapper);
    }

    @Override
    public List<Permission> getAllPermissions() {
        LambdaQueryWrapper<Permission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Permission::getSort);
        return list(queryWrapper);
    }

    @Override
    public List<Permission> getPermissionsByUserId(Long userId) {
        return permissionMapper.selectByUserId(userId);
    }

    @Override
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        return permissionMapper.selectByRoleId(roleId);
    }

    @Override
    public boolean save(Permission permission) {
        return super.save(permission);
    }

    @Override
    public boolean updateById(Permission permission) {
        return super.updateById(permission);
    }

    @Override
    public boolean removeById(Long id) {
        return super.removeById(id);
    }

    @Override
    public List<Permission> getPermissionTree() {
        // 获取所有权限
        List<Permission> allPermissions = list();

        // 构建树形结构
        return buildPermissionTree(allPermissions);
    }

    @Override
    public List<Permission> getMenus() {
        LambdaQueryWrapper<Permission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Permission::getType, 1) // 菜单类型
                .eq(Permission::getVisible, true)
                .orderByAsc(Permission::getSort);

        List<Permission> menus = list(queryWrapper);
        return buildPermissionTree(menus);
    }

    @Override
    public List<Permission> getUserMenus(Long userId) {
        // 获取用户权限中的菜单
        List<Permission> permissions = permissionMapper.selectByUserId(userId);

        List<Permission> menus = permissions.stream()
                .filter(p -> p.getType() == 1 && p.getVisible())
                .collect(Collectors.toList());

        return buildPermissionTree(menus);
    }

    /**
     * 构建权限树形结构
     * 
     * @param permissions 权限列表
     * @return 树形结构
     */
    private List<Permission> buildPermissionTree(List<Permission> permissions) {
        // 按父ID分组
        Map<Long, List<Permission>> parentIdMap = permissions.stream()
                .collect(Collectors.groupingBy(p -> p.getParentId() == null ? 0L : p.getParentId()));

        // 根节点列表
        List<Permission> rootPermissions = parentIdMap.getOrDefault(0L, new ArrayList<>());

        // 为每个节点设置子节点
        for (Permission permission : permissions) {
            Long id = permission.getId();
            List<Permission> children = parentIdMap.getOrDefault(id, new ArrayList<>());

            if (!children.isEmpty()) {
                // 对子节点排序
                children.sort((a, b) -> a.getSort() == null ? -1
                        : (b.getSort() == null ? 1 : a.getSort().compareTo(b.getSort())));

                // 直接设置children属性
                permission.setChildren(children);
            }
        }

        // 对根节点排序
        rootPermissions.sort(
                (a, b) -> a.getSort() == null ? -1 : (b.getSort() == null ? 1 : a.getSort().compareTo(b.getSort())));

        return rootPermissions;
    }
}