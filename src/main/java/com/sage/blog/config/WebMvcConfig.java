package com.sage.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.sage.blog.interceptor.AdminCleanUrlInterceptor;
import com.sage.blog.interceptor.JwtInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web MVC配置
 * 主要负责URL映射和路径转发，静态资源已移至ResourceConfig中处理
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebMvcConfig.class);

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        logger.info("注册拦截器");

        // 添加干净URL管理页面拦截器（优先级高于JWT拦截器）
        registry.addInterceptor(new AdminCleanUrlInterceptor())
                .addPathPatterns("/admin/**") // 管理后台路径
                .excludePathPatterns(
                        "/admin/auth-verify", "/admin/with-token",
                        "/admin/api/**", // 排除API接口
                        "/**/favicon.ico", "/error");

        logger.info("干净URL管理页面拦截器已注册，拦截路径: /admin/**");

        // JWT拦截器现在只用于处理with-token端点
        // 其他端点由干净URL拦截器处理
        registry.addInterceptor(new JwtInterceptor())
                .addPathPatterns("/admin/with-token") // 只拦截需要token参数的端点
                .excludePathPatterns(
                        "/css/**", "/js/**", "/img/**", "/resources/**", "/static/**", "/api/**",
                        "/", "/login", "/register", "/uploads/**", "/admin-auth",
                        "/**/favicon.ico", "/error");

        logger.info("JWT拦截器已注册，拦截路径: /admin/with-token");
    }

    /**
     * 添加视图控制器映射
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 首页映射
        registry.addViewController("/").setViewName("index");
        // 登录页映射
        registry.addViewController("/login").setViewName("login");
    }
}