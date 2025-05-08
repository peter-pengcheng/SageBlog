package com.sage.blog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sage.blog.common.Result;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * JWT认证异常处理
 * 当用户尝试访问安全的REST资源而不提供任何凭据时，将调用此处理程序
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException)
            throws IOException, ServletException {

        // 检查请求的URL路径
        String requestPath = request.getRequestURI();

        // 如果是API请求，返回JSON格式错误
        if (requestPath.startsWith("/api/")) {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");

            PrintWriter writer = response.getWriter();
            Result<Void> result = Result.unauthorized();

            ObjectMapper mapper = new ObjectMapper();
            writer.write(mapper.writeValueAsString(result));
            writer.flush();
        } else {
            // 如果是页面请求，重定向到登录页
            response.sendRedirect("/login");
        }
    }
}