package com.sage.blog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 页面控制器
 */
@Controller
public class PageController {

    /**
     * 首页
     */
    @GetMapping("/")
    public String index() {
        return "simple-index";
    }

    /**
     * 登录页
     */
    @GetMapping("/login")
    public String login() {
        return "simple-login";
    }

    /**
     * 注册页
     */
    @GetMapping("/register")
    public String register() {
        return "simple-register";
    }

    /**
     * 注册成功页
     */
    @GetMapping("/register-success")
    public String registerSuccess() {
        return "simple-register-success";
    }

    /**
     * 找回密码页面
     */
    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    /**
     * 重置密码页面
     * 
     * @param token 重置密码令牌
     * @param model 视图模型
     * @return 重置密码页面
     */
    @GetMapping("/reset-password")
    public String resetPassword(@RequestParam String token, Model model) {
        // 将token传递给视图
        model.addAttribute("token", token);
        return "reset-password";
    }

    /**
     * 个人中心页面
     */
    @GetMapping("/profile")
    public String profile() {
        return "user/profile";
    }

    /**
     * 控制面板页面
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "user/dashboard";
    }
}