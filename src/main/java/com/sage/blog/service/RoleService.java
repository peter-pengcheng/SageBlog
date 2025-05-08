package com.sage.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sage.blog.entity.Role;

import java.util.List;

public interface RoleService extends IService<Role> {

    /**
     * 分页查询角色列表
     *
     * @param page 页码
     * @param size 每页数量
     * @param name 角色名称（模糊查询）
     * @return 角色分页数据
     */
    IPage<Role> getRolePage(int page, int size, String name);

    /**
     * 根据用户ID查询角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Role> getRolesByUserId(Long userId);

    /**
     * 为角色分配权限
     *
     * @param roleId        角色ID
     * @param permissionIds 权限ID列表
     * @return 是否成功
     */
    boolean assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 根据角色编码查询角色
     *
     * @param code 角色编码
     * @return 角色信息
     */
    Role getRoleByCode(String code);
}