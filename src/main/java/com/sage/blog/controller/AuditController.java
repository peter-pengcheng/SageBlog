package com.sage.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sage.blog.common.Result;
import com.sage.blog.dto.AuditRequest;
import com.sage.blog.entity.RegisterAudit;
import com.sage.blog.entity.User;
import com.sage.blog.service.EmailService;
import com.sage.blog.service.RegisterAuditService;
import com.sage.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/audits")
public class AuditController {

    @Autowired
    private RegisterAuditService registerAuditService;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    /**
     * 获取审核记录分页列表
     *
     * @param page   页码
     * @param size   每页数量
     * @param status 审核状态
     * @return 审核记录分页数据
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:audit:list')")
    public Result<IPage<RegisterAudit>> getAuditPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        IPage<RegisterAudit> auditPage = registerAuditService.getAuditPage(page, size, status);
        return Result.success(auditPage);
    }

    /**
     * 获取待审核用户列表
     *
     * @param page 页码
     * @param size 每页数量
     * @return 待审核用户分页数据
     */
    @GetMapping("/pending-users")
    @PreAuthorize("hasAuthority('system:audit:list')")
    public Result<IPage<User>> getPendingAuditUserPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        IPage<User> userPage = userService.getPendingAuditUserPage(page, size);
        return Result.success(userPage);
    }

    /**
     * 审核用户注册
     *
     * @param auditRequest 审核请求
     * @return 审核结果
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:audit:operation')")
    public Result<Void> auditRegister(@Valid @RequestBody AuditRequest auditRequest) {
        // 获取当前操作人ID
        Long auditorId = getCurrentUserId();

        // 执行审核
        boolean success = registerAuditService.auditRegister(
                auditRequest.getAuditId(),
                auditRequest.getStatus(),
                auditRequest.getRemark(),
                auditorId);

        if (!success) {
            return Result.failed("审核失败，审核记录可能不存在");
        }

        // 根据审核ID获取审核记录
        RegisterAudit audit = registerAuditService.getById(auditRequest.getAuditId());

        // 根据用户ID获取用户
        User user = userService.getById(audit.getUserId());

        // 发送邮件通知用户审核结果
        if (user != null) {
            if (auditRequest.getStatus() == 1) {
                // 审核通过，发送通过邮件
                emailService.sendRegisterApprovedMail(user.getEmail(), user.getUsername());
            } else if (auditRequest.getStatus() == 2) {
                // 审核拒绝，发送拒绝邮件
                emailService.sendRegisterRejectedMail(user.getEmail(), user.getUsername(), auditRequest.getRemark());
            }
        }

        return Result.success();
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        return user != null ? user.getId() : null;
    }
}