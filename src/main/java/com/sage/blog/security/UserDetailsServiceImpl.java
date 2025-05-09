package com.sage.blog.security;

import com.sage.blog.entity.Permission;
import com.sage.blog.entity.User;
import com.sage.blog.service.PermissionService;
import com.sage.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UserDetailsService实现类
 * 用于根据用户名加载用户详情
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Autowired
    private PermissionService permissionService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 查询用户信息
        User user = userService.getUserByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() != 1) {
            throw new UsernameNotFoundException("账号已被禁用或未审核通过");
        }

        // 获取用户权限
        List<Permission> permissions = permissionService.getPermissionsByUserId(user.getId());

        // 转换为GrantedAuthority集合
        List<GrantedAuthority> authorities = permissions.stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getCode()))
                .collect(Collectors.toList());

        // 构建UserDetails对象
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities);
    }

    /**
     * 根据用户ID加载用户信息
     * 
     * @param userId 用户ID
     * @return 用户详情
     * @throws UsernameNotFoundException 如果用户不存在
     */
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        // 根据ID查询用户信息
        User user = userService.findById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 检查用户状态
        if (user.getStatus() != 1) {
            throw new UsernameNotFoundException("账号已被禁用或未审核通过");
        }

        // 获取用户权限
        List<Permission> permissions = permissionService.getPermissionsByUserId(userId);

        // 转换为GrantedAuthority集合
        List<GrantedAuthority> authorities = permissions.stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getCode()))
                .collect(Collectors.toList());

        // 构建UserDetails对象
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities);
    }
}