package com.sage.blog.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT拦截器
 * 用于拦截需要认证的页面请求，检查JWT令牌，并在必要时进行重定向
 */
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(JwtInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String uri = request.getRequestURI();
        logger.debug("JWT拦截器处理请求: {}", uri);

        // 获取JWT令牌，从头部、Cookie或参数中
        String jwtToken = getJwtFromRequest(request);

        if (jwtToken == null || jwtToken.isEmpty()) {
            logger.debug("未找到JWT令牌，重定向到首页");

            // 确保重定向URL包含上下文路径
            String contextPath = request.getContextPath();
            String redirectUrl = contextPath.isEmpty() ? "/" : contextPath + "/";

            // 请求中如果包含/sageblog/时，确保重定向到正确的路径
            if (uri.contains("/sageblog/") && !redirectUrl.contains("/sageblog/")) {
                redirectUrl = "/sageblog/";
            }

            logger.debug("重定向到首页: {}", redirectUrl);
            response.sendRedirect(redirectUrl);
            return false;
        }

        // JWT令牌存在，放行请求
        return true;
    }

    /**
     * 从请求中获取JWT令牌
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        // 首先从Authorization头中获取
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 从请求参数中获取
        String paramToken = request.getParameter("token");
        if (paramToken != null && !paramToken.isEmpty()) {
            return paramToken;
        }

        // 从Cookie中获取
        javax.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (javax.servlet.http.Cookie cookie : cookies) {
                if ("jwtToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 尝试从localStorage读取的token可能被存在sessionStorage中
        return null;
    }
}