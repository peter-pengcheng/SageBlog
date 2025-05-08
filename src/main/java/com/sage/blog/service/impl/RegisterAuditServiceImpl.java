package com.sage.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sage.blog.entity.RegisterAudit;
import com.sage.blog.entity.User;
import com.sage.blog.entity.UserRole;
import com.sage.blog.mapper.RegisterAuditMapper;
import com.sage.blog.mapper.UserMapper;
import com.sage.blog.mapper.UserRoleMapper;
import com.sage.blog.service.RegisterAuditService;
import com.sage.blog.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RegisterAuditServiceImpl extends ServiceImpl<RegisterAuditMapper, RegisterAudit>
        implements RegisterAuditService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RoleService roleService;

    @Override
    public IPage<RegisterAudit> getAuditPage(int page, int size, Integer status) {
        Page<RegisterAudit> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RegisterAudit> queryWrapper = new LambdaQueryWrapper<>();

        // 添加查询条件
        if (status != null) {
            queryWrapper.eq(RegisterAudit::getStatus, status);
        }

        // 排序
        queryWrapper.orderByDesc(RegisterAudit::getCreateTime);

        return page(pageParam, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean auditRegister(Long auditId, Integer status, String remark, Long auditorId) {
        // 获取审核记录
        RegisterAudit audit = getById(auditId);
        if (audit == null) {
            return false;
        }

        // 更新审核记录
        audit.setStatus(status);
        audit.setRemark(remark);
        audit.setAuditorId(auditorId);
        audit.setAuditTime(LocalDateTime.now());
        updateById(audit);

        // 更新用户状态
        User user = userMapper.selectById(audit.getUserId());
        if (user == null) {
            return false;
        }

        // 1代表通过，2代表拒绝
        if (status == 1) {
            // 通过审核，更新用户状态为正常
            user.setStatus(1);
            userMapper.updateById(user);

            // 给用户分配默认角色（普通用户）
            Long userRoleId = roleService.getRoleByCode("ROLE_USER").getId();
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(userRoleId);
            userRoleMapper.insert(userRole);
        } else if (status == 2) {
            // 拒绝审核，更新用户状态为禁用
            user.setStatus(2);
            userMapper.updateById(user);
        }

        return true;
    }
}