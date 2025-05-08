package com.sage.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 * 主要负责URL映射和路径转发，静态资源已移至ResourceConfig中处理
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${sageblog.file.upload-dir}")
    private String uploadDir;

    /**
     * 添加视图控制器映射
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 如有必要，可以在这里添加简单的视图控制器映射
        // 例如，将特定URL直接映射到视图名称
        // registry.addViewController("/").setViewName("simple-index");
        // registry.addViewController("/login").setViewName("simple-login");
    }
}