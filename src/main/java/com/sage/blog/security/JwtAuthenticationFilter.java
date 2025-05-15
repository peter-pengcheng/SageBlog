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
import java.util.Enumeration;

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
            String contextPath = request.getContextPath();

            // 增加详细日志
            logger.info("JwtAuthenticationFilter处理请求: {} {}", method, path);
            logger.info("Authorization头: {}", request.getHeader("Authorization"));
            logger.info("URL参数token: {}", request.getParameter("token"));
            logger.info("请求来源: {}", request.getHeader("Referer"));

            // 对于管理后台请求的特殊处理
            if (path.startsWith("/admin") && !path.equals("/admin-auth")) {
                logger.info("管理后台请求处理: {}", path);

                // 检查请求是否包含Authorization头或token参数
                String authHeader = request.getHeader("Authorization");
                String paramToken = request.getParameter("token");

                if (paramToken != null && !paramToken.isEmpty()) {
                    // 如果URL中包含有效token参数，先尝试验证这个token
                    logger.info("检测到URL参数token: {}",
                            paramToken.substring(0, Math.min(10, paramToken.length())) + "...");

                    if (tokenProvider.validateToken(paramToken)) {
                        // 获取用户ID
                        Long userId = tokenProvider.getUserIdFromJWT(paramToken);
                        logger.info("URL参数中的token有效，用户ID: {}", userId);

                        if (userId != null) {
                            // 创建认证对象
                            UserDetails userDetails = userDetailsService.loadUserById(userId);

                            // 检查是否有必要的权限
                            boolean hasSystemUserAuth = userDetails.getAuthorities().stream()
                                    .anyMatch(auth -> auth.getAuthority().equals("system:user"));

                            if (hasSystemUserAuth) {
                                logger.info("用户具有system:user权限，可以访问管理页面");

                                // 创建认证令牌并设置安全上下文
                                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authentication);

                                // 存储token到cookie，以便在后续请求中使用
                                Cookie tokenCookie = new Cookie("jwtToken", paramToken);
                                tokenCookie.setPath("/");
                                tokenCookie.setHttpOnly(true);
                                tokenCookie.setMaxAge(3600); // 1小时有效期
                                response.addCookie(tokenCookie);

                                logger.info("已将token设置到Cookie中，继续处理请求");
                            } else {
                                logger.warn("用户没有system:user权限，拒绝访问管理页面");
                                response.sendRedirect("/permission-error?username=" + userDetails.getUsername() +
                                        "&requiredPermission=system:user&page=" + path);
                                return;
                            }
                        } else {
                            logger.warn("无法从token中提取有效的用户ID");
                            response.sendRedirect("/login?error=invalid_token");
                            return;
                        }
                    } else {
                        logger.warn("URL参数中的token无效");
                        response.sendRedirect("/login?error=invalid_token");
                        return;
                    }
                } else if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    // 如果没有认证信息，尝试从cookie中获取
                    Cookie[] cookies = request.getCookies();
                    String cookieToken = null;

                    if (cookies != null) {
                        for (Cookie cookie : cookies) {
                            if ("jwtToken".equals(cookie.getName())) {
                                cookieToken = cookie.getValue();
                                break;
                            }
                        }
                    }

                    if (cookieToken != null && !cookieToken.isEmpty()) {
                        logger.info("从Cookie中找到token，继续处理");

                        if (processToken(cookieToken, request, response)) {
                            // Token处理成功，继续过滤链
                            filterChain.doFilter(request, response);
                            return;
                        } else {
                            // Token处理失败，重定向到登录页
                            response.sendRedirect("/login?error=invalid_token&redirect=" + path);
                            return;
                        }
                    }

                    // 没有任何认证信息，交由拦截器处理
                    logger.info("管理后台请求没有认证信息，交由拦截器处理: {}", path);
                    filterChain.doFilter(request, response);
                    return;
                }

                // 如果有Authorization头，继续处理
                logger.info("管理后台请求包含Authorization头，JWT过滤器处理: {}", path);
            }

            // 静态资源和公开API路径
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
                    path.equals(contextPath) ||
                    path.equals(contextPath + "/") ||
                    path.equals(contextPath + "/login") ||
                    path.equals(contextPath + "/register") ||
                    path.equals("/admin-auth")) { // 增加admin-auth到公开路径

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

            logger.info("找到JWT令牌: {}, 源自: {}", jwt.substring(0, Math.min(10, jwt.length())) + "...",
                    request.getParameter("token") != null ? "URL参数"
                            : request.getHeader("Authorization") != null ? "Authorization头" : "其他来源");

            // 验证JWT令牌并处理认证
            processToken(jwt, request, response);
        } catch (Exception ex) {
            logger.error("JWT认证处理异常", ex);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 处理JWT令牌
     * 
     * @return 处理是否成功
     */
    private boolean processToken(String jwt, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            if (tokenProvider.validateToken(jwt)) {
                // 获取用户ID
                Long userId = tokenProvider.getUserIdFromJWT(jwt);
                logger.debug("JWT令牌有效，用户ID: {}", userId);

                if (userId == null) {
                    logger.warn("JWT令牌中无法获取有效的用户ID");
                    SecurityContextHolder.clearContext();
                    return false;
                }

                // 创建认证对象
                UserDetails userDetails = userDetailsService.loadUserById(userId);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 设置安全上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("用户 {} 已成功认证", userDetails.getUsername());
                return true;
            } else {
                logger.warn("JWT令牌无效，清除安全上下文");
                SecurityContextHolder.clearContext();
                return false;
            }
        } catch (Exception e) {
            logger.error("处理JWT令牌时发生异常", e);
            return false;
        }
    }

    /**
     * 从请求中提取JWT令牌
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        // 首先从Authorization头中获取
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            logger.debug("从Authorization头获取到令牌");
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
                    logger.debug("从Cookie中获取到令牌");
                    return cookie.getValue();
                }
            }
        }

        // 从请求header中获取X-Auth-Token
        String xAuthToken = request.getHeader("X-Auth-Token");
        if (StringUtils.hasText(xAuthToken)) {
            logger.debug("从X-Auth-Token头获取到令牌");
            return xAuthToken;
        }

        logger.debug("在请求中没有找到有效的JWT令牌");
        return null;
    }

    private void redirectToLogin(HttpServletResponse response, String path, HttpServletRequest request)
            throws IOException {
        // 构建重定向URL，始终使用request.getContextPath()获取正确的上下文路径
        String contextPath = request.getContextPath();
        String redirectParam = path;
        String redirectUrl = contextPath.isEmpty() ? "/login?redirect=" + redirectParam
                : contextPath + "/login?redirect=" + redirectParam;

        logger.debug("重定向用户到登录页: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}