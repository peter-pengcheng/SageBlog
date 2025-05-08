package com.sage.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sage.blog.entity.Permission;

import java.util.List;

/**
 * 权限服务接口
 */
public interface PermissionService extends IService<Permission> {

    /**
     * 获取权限分页列表
     *
     * @param page 当前页码
     * @param size 每页大小
     * @param name 权限名称(模糊查询)
     * @return 权限分页列表
     */
    IPage<Permission> getPermissionPage(int page, int size, String name);

    /**
     * 根据ID获取权限
     *
     * @param id 权限ID
     * @return 权限信息
     */
    Permission getById(Long id);

    /**
     * 根据权限码获取权限
     *
     * @param code 权限码
     * @return 权限信息
     */
    Permission getByCode(String code);

    /**
     * 获取所有权限
     *
     * @return 权限列表
     */
    List<Permission> getAllPermissions();

    /**
     * 根据用户ID获取权限列表
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<Permission> getPermissionsByUserId(Long userId);

    /**
     * 根据角色ID获取权限列表
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<Permission> getPermissionsByRoleId(Long roleId);

    /**
     * 保存权限
     *
     * @param permission 权限信息
     * @return 是否成功
     */
    boolean save(Permission permission);

    /**
     * 更新权限
     *
     * @param permission 权限信息
     * @return 是否成功
     */
    boolean updateById(Permission permission);

    /**
     * 删除权限
     *
     * @param id 权限ID
     * @return 是否成功
     */
    boolean removeById(Long id);

    /**
     * 获取权限树结构
     *
     * @return 权限树
     */
    List<Permission> getPermissionTree();

    /**
     * 获取菜单列表
     * 
     * @return 菜单列表
     */
    List<Permission> getMenus();

    /**
     * 获取用户的菜单列表
     * 
     * @param userId 用户ID
     * @return 菜单列表
     */
    List<Permission> getUserMenus(Long userId);
}