package com.sage.blog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 认证桥接控制器
 * 用于处理需要特殊认证流程的页面
 */
@Controller
public class AuthBridgeController {

    /**
     * 管理后台认证桥接页面
     * 作为浏览器环境下访问管理后台的中间层，用于获取localStorage中的JWT令牌
     * 并使用JavaScript代码直接发起带认证头的请求
     * 
     * @param redirectPath 重定向的目标页面路径
     * @param model        视图模型
     * @return 管理后台认证桥接页面
     */
    @GetMapping("/admin-auth")
    public String adminAuthBridge(@RequestParam(required = false) String redirect, Model model) {
        // 将重定向路径传递给视图
        model.addAttribute("redirectPath", redirect != null ? redirect : "/admin");
        return "auth/admin/auth";
    }

    /**
     * 权限错误页面
     * 用于显示友好的权限错误信息
     */
    @GetMapping("/permission-error")
    public String permissionError(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String requiredPermission,
            @RequestParam(required = false) String page,
            Model model) {

        model.addAttribute("title", "权限不足 - SageBlog");
        model.addAttribute("username", username);
        model.addAttribute("requiredPermission", requiredPermission);
        model.addAttribute("page", page);

        return "error/permission";
    }
}