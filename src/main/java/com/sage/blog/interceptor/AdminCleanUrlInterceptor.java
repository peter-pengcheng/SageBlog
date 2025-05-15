package com.sage.blog.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 管理页面干净URL拦截器
 * 用于处理直接访问/admin/xxx等路径的请求，确保用户已通过权限验证
 * 避免在URL中暴露token等敏感信息
 */
public class AdminCleanUrlInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AdminCleanUrlInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();

        // 仅处理admin页面请求，排除API和特殊端点
        if (path.startsWith("/admin") &&
                !path.contains("/admin/with-token") &&
                !path.contains("/admin/auth-verify") &&
                !path.contains("/admin/api/") &&
                !path.equals("/admin-auth")) {

            logger.info("干净URL拦截器处理管理页面请求: {}", path);

            // 首先检查会话中是否已有ADMIN_VERIFIED标记
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("ADMIN_VERIFIED") != null) {
                Boolean isVerified = (Boolean) session.getAttribute("ADMIN_VERIFIED");
                if (isVerified) {
                    String username = (String) session.getAttribute("ADMIN_USERNAME");
                    logger.info("用户 {} 已通过会话验证，允许访问: {}", username, path);
                    return true;
                }
            }

            // 检查认证状态
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) auth.getPrincipal();

                // 检查是否有system:user权限
                boolean hasAdminAuth = userDetails.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("system:user"));

                if (hasAdminAuth) {
                    // 用户已认证且有权限，设置会话标记并放行
                    logger.info("用户 {} 已通过权限检查，允许访问: {}", userDetails.getUsername(), path);
                    if (session != null) {
                        session.setAttribute("ADMIN_VERIFIED", true);
                        session.setAttribute("ADMIN_USERNAME", userDetails.getUsername());
                    }
                    return true;
                } else {
                    // 有认证但权限不足
                    logger.warn("用户 {} 权限不足，拒绝访问: {}", userDetails.getUsername(), path);
                    response.sendRedirect("/permission-error?username=" + userDetails.getUsername() +
                            "&requiredPermission=system:user&page=" + path);
                    return false;
                }
            }

            // 检查Cookie中是否有JWT令牌
            Cookie[] cookies = request.getCookies();
            String jwtToken = null;

            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("jwtToken".equals(cookie.getName()) && cookie.getValue() != null
                            && !cookie.getValue().isEmpty()) {
                        jwtToken = cookie.getValue();
                        break;
                    }
                }
            }

            // 从localStorage获取token (通过验证页面)
            if (jwtToken != null) {
                // 有令牌但未验证，创建美观的验证页面
                logger.info("发现JWT Cookie但未验证，显示美观验证页面");

                // 创建美观的表单验证页面
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(
                        "<!DOCTYPE html>\n" +
                                "<html lang='zh'>\n" +
                                "<head>\n" +
                                "  <meta charset='UTF-8'>\n" +
                                "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                                "  <title>管理页面访问验证</title>\n" +
                                "  <link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css'>\n"
                                +
                                "  <style>\n" +
                                "    body {\n" +
                                "      display: flex;\n" +
                                "      justify-content: center;\n" +
                                "      align-items: center;\n" +
                                "      min-height: 100vh;\n" +
                                "      background-color: #f8f9fa;\n" +
                                "    }\n" +
                                "    .auth-container {\n" +
                                "      text-align: center;\n" +
                                "      max-width: 500px;\n" +
                                "      padding: 2rem;\n" +
                                "      border-radius: 8px;\n" +
                                "      box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);\n" +
                                "      background-color: white;\n" +
                                "    }\n" +
                                "    .spinner-container {\n" +
                                "      margin: 2rem 0;\n" +
                                "    }\n" +
                                "  </style>\n" +
                                "</head>\n" +
                                "<body>\n" +
                                "  <div class='auth-container'>\n" +
                                "    <h3>管理页面访问验证</h3>\n" +
                                "    <p class='text-muted'>系统正在验证您的管理员权限，请稍候...</p>\n" +
                                "    \n" +
                                "    <div class='spinner-container'>\n" +
                                "      <div class='spinner-border text-primary' role='status'>\n" +
                                "        <span class='visually-hidden'>Loading...</span>\n" +
                                "      </div>\n" +
                                "    </div>\n" +
                                "    \n" +
                                "    <form id='adminAuthForm' method='POST' action='/admin/auth-verify' style='display:none;'>\n"
                                +
                                "      <input type='hidden' name='token' value='" + jwtToken + "'>\n" +
                                "      <input type='hidden' name='targetPath' value='" + path + "'>\n" +
                                "    </form>\n" +
                                "    \n" +
                                "    <script>\n" +
                                "      // 短暂延迟后提交表单，让用户看到友好的加载界面\n" +
                                "      setTimeout(function() {\n" +
                                "        document.getElementById('adminAuthForm').submit();\n" +
                                "      }, 800);\n" +
                                "    </script>\n" +
                                "  </div>\n" +
                                "</body>\n" +
                                "</html>");
                return false;
            }

            // 未找到认证信息，重定向到登录页面
            logger.info("未找到认证信息，重定向到登录页: {}", path);
            String redirectUrl = "/login?redirect=" + path;
            response.sendRedirect(redirectUrl);
            return false;
        }

        return true;
    }
}