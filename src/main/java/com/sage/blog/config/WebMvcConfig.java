package com.sage.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.sage.blog.interceptor.JwtInterceptor;

/**
 * Web MVC配置
 * 主要负责URL映射和路径转发，静态资源已移至ResourceConfig中处理
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加JWT拦截器
        registry.addInterceptor(new JwtInterceptor())
                .addPathPatterns("/dashboard", "/profile")
                .excludePathPatterns(
                        "/css/**", "/js/**", "/img/**", "/resources/**", "/static/**", "/api/**",
                        "/", "/login", "/register", "/uploads/**");
    }

    /**
     * 添加视图控制器映射
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 简单URL重定向
        registry.addRedirectViewController("/index", "/");
    }
}