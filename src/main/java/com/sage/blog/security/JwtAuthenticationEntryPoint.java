package com.sage.blog.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT认证入口点
 * 用于处理未认证的请求
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {

        logger.error("未授权访问: {}", authException.getMessage());

        String requestURI = request.getRequestURI();

        // 判断是API请求还是页面请求
        if (requestURI.contains("/api/")) {
            // API请求返回JSON格式错误
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":401,\"message\":\"未授权访问\",\"data\":null}");
        } else {
            // 页面请求重定向到首页
            String contextPath = request.getContextPath();
            // 修正重定向路径
            String redirectUrl = contextPath.isEmpty() ? "/" : contextPath + "/";

            // 处理带有上下文路径的情况
            if (requestURI.contains("/sageblog/") && !redirectUrl.contains("/sageblog/")) {
                redirectUrl = "/sageblog/";
            }

            response.sendRedirect(redirectUrl);
        }
    }
}