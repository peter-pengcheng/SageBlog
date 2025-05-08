package com.sage.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import java.util.Collections;
import java.util.Set;

/**
 * Thymeleaf配置
 * 增强模板解析和片段支持
 */
@Configuration
public class ThymeleafConfig {

    /**
     * 配置主模板解析器
     */
    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false); // 开发环境不缓存
        resolver.setOrder(1);
        resolver.setCheckExistence(true);
        return resolver;
    }

    /**
     * 配置片段模板解析器，用于处理Thymeleaf片段
     */
    @Bean
    public ClassLoaderTemplateResolver fragmentTemplateResolver() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        resolver.setOrder(2);
        resolver.setCheckExistence(true);

        // 配置支持片段解析的模式 - 匹配所有模板
        Set<String> patterns = Collections.singleton("*");
        resolver.setResolvablePatterns(patterns);

        return resolver;
    }

    /**
     * 配置模板引擎，添加两个模板解析器
     */
    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.addTemplateResolver(templateResolver());
        engine.addTemplateResolver(fragmentTemplateResolver());
        engine.setEnableSpringELCompiler(true);
        return engine;
    }

    /**
     * 配置视图解析器
     */
    @Bean
    public ViewResolver viewResolver() {
        ThymeleafViewResolver resolver = new ThymeleafViewResolver();
        resolver.setTemplateEngine(templateEngine());
        resolver.setCharacterEncoding("UTF-8");
        resolver.setContentType("text/html; charset=UTF-8");
        resolver.setViewNames(new String[] { "*" });
        return resolver;
    }
}