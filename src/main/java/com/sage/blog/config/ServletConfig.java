package com.sage.blog.config;

import com.sage.blog.servlet.FileServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Servlet配置类
 * 显式注册所有Servlet，不依赖于@WebServlet注解
 */
@Configuration
public class ServletConfig {

    /**
     * 注册文件服务Servlet
     */
    @Bean
    public ServletRegistrationBean<FileServlet> fileServletRegistrationBean(FileServlet fileServlet) {
        ServletRegistrationBean<FileServlet> registration = new ServletRegistrationBean<>(fileServlet,
                "/resources/*", "/sageblog/resources/*");
        registration.setLoadOnStartup(1); // 优先级高的servlet，确保尽早加载
        registration.setName("fileServlet");
        return registration;
    }
}