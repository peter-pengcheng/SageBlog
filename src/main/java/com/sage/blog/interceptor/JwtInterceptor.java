package com.sage.blog.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT拦截器
 * 用于处理带有JWT token的请求
 */
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(JwtInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();
        logger.info("JWT拦截器处理路径: {}", path);

        // 仅处理admin页面，但排除with-token专用端点
        if (path.startsWith("/admin") &&
                !path.equals("/admin-auth") &&
                !path.contains("/admin/with-token")) {

            logger.info("处理管理页面请求: {}", path);

            // 检查是否有token参数
            String token = request.getParameter("token");
            if (token != null && !token.isEmpty()) {
                // 使用token参数直接通过
                logger.info("检测到URL参数中的token，放行请求: {}", path);
                return true;
            }

            // 检查请求中是否已经有Authorization头
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                logger.info("请求已包含Authorization头，放行请求: {}", path);
                return true;
            }

            // 检查Cookie中是否有JWT令牌
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("jwtToken".equals(cookie.getName()) && cookie.getValue() != null
                            && !cookie.getValue().isEmpty()) {
                        logger.info("在Cookie中找到JWT令牌，放行请求: {}", path);
                        return true;
                    }
                }
            }

            // 检查当前认证状态，获取用户名
            String username = "未知用户";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails) {
                username = ((UserDetails) auth.getPrincipal()).getUsername();
            }

            // 未找到认证信息，使用权限错误页面或重定向到登录
            if (auth != null && auth.isAuthenticated()) {
                // 用户已认证但权限不足
                logger.info("用户已认证但权限不足: {}", username);
                response.sendRedirect("/permission-error?username=" + username +
                        "&requiredPermission=system:user&page=" + path);
            } else {
                // 用户未认证，重定向到登录页面
                logger.info("未找到认证信息，重定向到登录页: {}", path);
                String redirectUrl = "/login?redirect=" + path;
                response.sendRedirect(redirectUrl);
            }
            return false;
        }

        return true;
    }
}