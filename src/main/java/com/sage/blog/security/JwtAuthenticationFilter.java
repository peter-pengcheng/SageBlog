package com.sage.blog.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT认证过滤器
 * 每次请求都会执行，用于验证JWT令牌并设置安全上下文
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    /**
     * 无参构造函数
     */
    public JwtAuthenticationFilter() {
        // 空构造函数
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String path = request.getRequestURI();
            String method = request.getMethod();

            logger.debug("处理请求: {} {}", method, path);
            logger.debug("请求头Authorization: {}", request.getHeader("Authorization"));
            logger.debug("请求参数token: {}", request.getParameter("token"));

            if (path.contains("/api/auth/") ||
                    path.endsWith(".js") ||
                    path.endsWith(".css") ||
                    path.endsWith(".ico") ||
                    path.endsWith(".png") ||
                    path.endsWith(".jpg") ||
                    path.endsWith(".jpeg") ||
                    path.endsWith(".gif") ||
                    path.endsWith(".svg") ||
                    path.contains("/resources/") ||
                    path.contains("/uploads/") ||
                    path.equals("/sageblog") ||
                    path.equals("/sageblog/") ||
                    path.equals("/sageblog/login") ||
                    path.equals("/sageblog/register")) {

                logger.debug("公开资源，跳过JWT验证: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            // 获取JWT令牌
            String jwt = getJwtFromRequest(request);

            if (jwt == null) {
                logger.debug("路径 {} 没有找到JWT令牌", path);

                // 对于dashboard和profile请求，使用特殊处理
                if (path.contains("/dashboard") || path.contains("/profile")) {
                    logger.info("访问受保护页面没有令牌: {}", path);
                    redirectToLogin(response, path, request);
                    return;
                }

                filterChain.doFilter(request, response);
                return;
            }

            logger.debug("找到JWT令牌: {}", jwt.substring(0, Math.min(10, jwt.length())) + "...");

            // 验证JWT令牌
            if (tokenProvider.validateToken(jwt)) {
                // 获取用户ID
                Long userId = tokenProvider.getUserIdFromJWT(jwt);
                logger.debug("JWT令牌有效，用户ID: {}", userId);

                if (userId == null) {
                    logger.warn("JWT令牌中无法获取有效的用户ID");
                    SecurityContextHolder.clearContext();
                    // 如果是需要认证的页面，重定向到登录页
                    if (path.contains("/dashboard") || path.contains("/profile")) {
                        redirectToLogin(response, path, request);
                    }
                    filterChain.doFilter(request, response);
                    return;
                }

                // 创建认证对象
                UserDetails userDetails = userDetailsService.loadUserById(userId);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 设置安全上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("用户 {} 已成功认证", userDetails.getUsername());
            } else {
                logger.warn("JWT令牌无效，清除安全上下文");
                SecurityContextHolder.clearContext();

                // 对于dashboard和profile请求，使用特殊处理
                if (path.contains("/dashboard") || path.contains("/profile")) {
                    logger.info("访问受保护页面令牌无效: {}", path);
                    redirectToLogin(response, path, request);
                    return;
                }
            }
        } catch (Exception ex) {
            logger.error("JWT认证异常", ex);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中提取JWT令牌
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        // 首先从Authorization头中获取
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 从URL参数中获取token参数
        String paramToken = request.getParameter("token");
        if (StringUtils.hasText(paramToken)) {
            logger.debug("从URL参数中获取到令牌");
            return paramToken;
        }

        // 从cookie中尝试获取
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName()) || "jwtToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 从请求header中获取X-Auth-Token
        String xAuthToken = request.getHeader("X-Auth-Token");
        if (StringUtils.hasText(xAuthToken)) {
            return xAuthToken;
        }

        return null;
    }

    private void redirectToLogin(HttpServletResponse response, String path, HttpServletRequest request)
            throws IOException {
        // 构建重定向URL
        String contextPath = request.getContextPath();
        String redirectUrl = contextPath.isEmpty() ? "/login" : contextPath + "/login";

        // 处理带有上下文路径的情况
        if (path.contains("/sageblog/") && !redirectUrl.contains("/sageblog/")) {
            redirectUrl = "/sageblog/login";
        }

        logger.debug("重定向用户到登录页: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}