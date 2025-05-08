package com.sage.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * 静态资源配置类
 * 处理所有静态资源的路径映射和加载
 */
@Configuration
public class ResourceConfig implements WebMvcConfigurer {

    @Value("${sageblog.file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // CSS资源 - 支持直接访问/css/**和带上下文路径/sageblog/css/**
        registry.addResourceHandler("/css/**", "/sageblog/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(3600);

        // JS资源 - 支持直接访问/js/**和带上下文路径/sageblog/js/**
        registry.addResourceHandler("/js/**", "/sageblog/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(3600);

        // 图片资源 - 支持直接访问/img/**和带上下文路径/sageblog/img/**
        registry.addResourceHandler("/img/**", "/sageblog/img/**")
                .addResourceLocations("classpath:/static/img/")
                .setCachePeriod(3600);

        // Favicon - 支持直接访问/favicon.ico和带上下文路径/sageblog/favicon.ico
        registry.addResourceHandler("/favicon.ico", "/sageblog/favicon.ico")
                .addResourceLocations("classpath:/static/favicon.ico")
                .setCachePeriod(3600);

        // 静态HTML和其他资源 - 支持直接访问/static/**和带上下文路径/sageblog/static/**
        registry.addResourceHandler("/static/**", "/sageblog/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);

        // 所有Webjars资源
        registry.addResourceHandler("/webjars/**", "/sageblog/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .setCachePeriod(3600)
                .resourceChain(true);

        // 上传文件的访问映射
        String uploadPath = "file:" + uploadDir + File.separator;
        registry.addResourceHandler("/resources/**", "/sageblog/resources/**")
                .addResourceLocations(uploadPath);
    }
}