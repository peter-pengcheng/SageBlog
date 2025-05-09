package com.sage.blog.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 资源访问调试过滤器
 * 用于记录资源请求的详细信息，帮助排查资源访问问题
 */
@Component
public class ResourceDebugFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ResourceDebugFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // 只记录资源相关的请求
        if (requestURI.contains("/resources/")) {
            logger.info("【资源请求】URI: {}, 方法: {}", requestURI, method);
            logger.info("【资源请求】Content-Type: {}", request.getContentType());
            logger.info("【资源请求】QueryString: {}", request.getQueryString());
            logger.info("【资源请求】ContextPath: {}", request.getContextPath());
            logger.info("【资源请求】ServletPath: {}", request.getServletPath());
            logger.info("【资源请求】PathInfo: {}", request.getPathInfo());

            // 记录请求头信息
            logger.info("【资源请求】Headers: ");
            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                logger.info("  - {}: {}", headerName, request.getHeader(headerName));
            }
        }

        // 执行过滤器链
        filterChain.doFilter(request, response);

        // 记录响应状态
        if (requestURI.contains("/resources/")) {
            logger.info("【资源响应】URI: {}, 状态码: {}", requestURI, response.getStatus());
        }
    }
}