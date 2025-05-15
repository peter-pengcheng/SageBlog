package com.sage.blog.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import com.sage.blog.security.JwtTokenProvider;
import com.sage.blog.security.UserDetailsServiceImpl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Enumeration;

/**
 * 后台管理页面控制器
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('system:user')")
public class AdminPageController {

    private static final Logger logger = LoggerFactory.getLogger(AdminPageController.class);

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    /**
     * 后台首页
     */
    @GetMapping({ "", "/" })
    public String index(Model model) {
        model.addAttribute("title", "管理后台 - SageBlog");
        return "admin/index";
    }

    /**
     * 通用处理带token参数的管理页面请求
     * 无需权限注解，因为我们会在方法内部检查token有效性和权限
     * 支持子路径和自动映射到对应视图
     */
    @GetMapping("/with-token")
    public String processWithToken(
            @RequestParam String token,
            @RequestParam(required = false) String target,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        logger.info("处理带token参数的管理页面请求, 目标路径: {}", target);

        try {
            // 验证token
            if (!tokenProvider.validateToken(token)) {
                logger.warn("无效的token参数");
                return "redirect:/login?error=无效的认证令牌";
            }

            // 获取用户信息
            Long userId = tokenProvider.getUserIdFromJWT(token);
            if (userId == null) {
                logger.warn("无法从token获取用户ID");
                return "redirect:/login?error=无效的用户信息";
            }

            // 获取用户详情
            UserDetails userDetails = userDetailsService.loadUserById(userId);

            // 检查是否有system:user权限
            boolean hasRequiredAuth = userDetails.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("system:user"));

            if (!hasRequiredAuth) {
                logger.warn("用户没有管理权限: {}", userDetails.getUsername());

                // 重定向到友好的权限错误页面，并传递用户信息
                return "redirect:/permission-error?username=" + userDetails.getUsername() +
                        "&requiredPermission=system:user" +
                        "&page=" + (target != null ? target : "/admin");
            }

            // 设置认证上下文
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails,
                    null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 保存token到cookie
            Cookie tokenCookie = new Cookie("jwtToken", token);
            tokenCookie.setPath("/");
            tokenCookie.setHttpOnly(true);
            tokenCookie.setMaxAge(3600); // 1小时
            response.addCookie(tokenCookie);

            logger.info("用户 {} 通过token参数认证成功", userDetails.getUsername());

            // 添加模型属性
            model.addAttribute("title", "管理后台 - SageBlog");

            // 根据目标路径确定返回的视图
            if (target != null && !target.isEmpty()) {
                // 提取目标路径中的关键部分
                String viewName = target.replace("/admin/", "").replace("/admin", "");
                if (viewName.isEmpty()) {
                    return "admin/index"; // 管理后台首页
                }

                // 检查是否有权限访问目标页面
                if (viewName.equals("users") && !hasPermission(userDetails, "system:user")) {
                    return "redirect:/permission-error?username=" + userDetails.getUsername() +
                            "&requiredPermission=system:user&page=/admin/users";
                } else if (viewName.equals("roles") && !hasPermission(userDetails, "system:role")) {
                    return "redirect:/permission-error?username=" + userDetails.getUsername() +
                            "&requiredPermission=system:role&page=/admin/roles";
                } else if (viewName.equals("permissions") && !hasPermission(userDetails, "system:permission")) {
                    return "redirect:/permission-error?username=" + userDetails.getUsername() +
                            "&requiredPermission=system:permission&page=/admin/permissions";
                } else if (viewName.equals("menus") && !hasPermission(userDetails, "system:menu")) {
                    return "redirect:/permission-error?username=" + userDetails.getUsername() +
                            "&requiredPermission=system:menu&page=/admin/menus";
                } else if (viewName.equals("audits") && !hasPermission(userDetails, "system:audit")) {
                    return "redirect:/permission-error?username=" + userDetails.getUsername() +
                            "&requiredPermission=system:audit&page=/admin/audits";
                } else if (viewName.equals("logs") && !hasPermission(userDetails, "system:log")) {
                    return "redirect:/permission-error?username=" + userDetails.getUsername() +
                            "&requiredPermission=system:log&page=/admin/logs";
                }

                // 尝试返回对应的视图
                return "admin/" + viewName;
            }

            return "admin/index";
        } catch (Exception e) {
            logger.error("处理token参数时出错", e);
            return "redirect:/login?error=系统错误";
        }
    }

    /**
     * 检查用户是否有指定权限
     */
    private boolean hasPermission(UserDetails userDetails, String permission) {
        return userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(permission));
    }

    /**
     * 权限调试接口 - 返回当前用户的所有权限信息
     */
    @GetMapping("/debug-auth")
    @ResponseBody
    public Map<String, Object> debugAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> result = new HashMap<>();

        if (auth != null) {
            result.put("authenticated", auth.isAuthenticated());
            result.put("name", auth.getName());
            result.put("principal", auth.getPrincipal());

            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
            result.put("authorities", authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));

            result.put("hasAdminRole", authorities.stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
            result.put("hasSystemUserAuth", authorities.stream()
                    .anyMatch(a -> a.getAuthority().equals("system:user")));
        } else {
            result.put("authenticated", false);
        }

        return result;
    }

    /**
     * 用户管理页面
     */
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('system:user')")
    public String users(Model model) {
        model.addAttribute("title", "用户管理 - SageBlog");
        return "admin/users";
    }

    /**
     * 角色管理页面
     */
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('system:role')")
    public String roles(Model model) {
        model.addAttribute("title", "角色管理 - SageBlog");
        return "admin/roles";
    }

    /**
     * 权限管理页面
     */
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('system:permission')")
    public String permissions(Model model) {
        model.addAttribute("title", "权限管理 - SageBlog");
        return "admin/permissions";
    }

    /**
     * 菜单管理页面
     */
    @GetMapping("/menus")
    @PreAuthorize("hasAuthority('system:menu')")
    public String menus(Model model) {
        model.addAttribute("title", "菜单管理 - SageBlog");
        return "admin/menus";
    }

    /**
     * 用户审核页面
     */
    @GetMapping("/audits")
    @PreAuthorize("hasAuthority('system:audit')")
    public String audits(Model model) {
        model.addAttribute("title", "用户审核 - SageBlog");
        return "admin/audits";
    }

    /**
     * 系统日志页面
     */
    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('system:log')")
    public String logs(Model model) {
        model.addAttribute("title", "系统日志 - SageBlog");
        return "admin/logs";
    }

    /**
     * 诊断端点 - 检查请求中的cookie和token信息
     */
    @GetMapping("/debug-request")
    @ResponseBody
    public Map<String, Object> debugRequest(HttpServletRequest request, @RequestParam(required = false) String token) {
        Map<String, Object> result = new HashMap<>();

        // 收集请求头信息
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        result.put("headers", headers);

        // 收集cookie信息
        Map<String, String> cookies = new HashMap<>();
        Cookie[] requestCookies = request.getCookies();
        if (requestCookies != null) {
            for (Cookie cookie : requestCookies) {
                cookies.put(cookie.getName(), cookie.getValue());
            }
        }
        result.put("cookies", cookies);

        // URL参数token
        result.put("urlToken", token);

        // 当前认证信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Map<String, Object> authInfo = new HashMap<>();
            authInfo.put("authenticated", auth.isAuthenticated());
            authInfo.put("name", auth.getName());
            authInfo.put("type", auth.getClass().getName());
            authInfo.put("authorities", auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));
            result.put("authentication", authInfo);
        }

        return result;
    }

    /**
     * POST方法处理token认证，避免在URL中暴露敏感信息
     */
    @PostMapping("/auth-verify")
    public String verifyAdminAccess(
            @RequestParam String token,
            @RequestParam(required = false) String targetPath,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        logger.info("POST请求验证管理页面访问权限，目标路径: {}", targetPath);

        try {
            // 验证token
            if (!tokenProvider.validateToken(token)) {
                logger.warn("无效的token参数");
                return "redirect:/login?error=无效的认证令牌";
            }

            // 获取用户信息
            Long userId = tokenProvider.getUserIdFromJWT(token);
            if (userId == null) {
                logger.warn("无法从token获取用户ID");
                return "redirect:/login?error=无效的用户信息";
            }

            // 获取用户详情
            UserDetails userDetails = userDetailsService.loadUserById(userId);

            // 检查是否有system:user权限
            boolean hasRequiredAuth = userDetails.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("system:user"));

            if (!hasRequiredAuth) {
                logger.warn("用户没有管理权限: {}", userDetails.getUsername());

                // 重定向到友好的权限错误页面，并传递用户信息
                return "redirect:/permission-error?username=" + userDetails.getUsername() +
                        "&requiredPermission=system:user" +
                        "&page=" + (targetPath != null ? targetPath : "/admin");
            }

            // 设置认证上下文
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails,
                    null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 保存token到cookie
            Cookie tokenCookie = new Cookie("jwtToken", token);
            tokenCookie.setPath("/");
            tokenCookie.setHttpOnly(true);
            tokenCookie.setMaxAge(3600); // 1小时
            response.addCookie(tokenCookie);

            // 设置会话属性（即使使用无状态JWT，这里也用会话标记来简化验证流程）
            HttpSession session = request.getSession(true);
            session.setAttribute("ADMIN_VERIFIED", true);
            session.setAttribute("ADMIN_USERNAME", userDetails.getUsername());

            logger.info("用户 {} 通过POST验证成功，正在跳转到: {}", userDetails.getUsername(), targetPath);

            // 增加缓存控制头，防止页面被缓存
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            // 处理目标路径并返回相应视图
            if (targetPath == null || targetPath.isEmpty()) {
                // 默认管理首页
                model.addAttribute("title", "管理后台 - SageBlog");
                return "admin/index";
            }

            // 提取目标路径中的关键部分
            String viewName = targetPath.replace("/admin/", "").replace("/admin", "");
            if (viewName.isEmpty()) {
                model.addAttribute("title", "管理后台 - SageBlog");
                return "admin/index";
            }

            // 检查特定页面权限
            switch (viewName) {
                case "users":
                    if (!hasPermission(userDetails, "system:user")) {
                        return "redirect:/permission-error?username=" + userDetails.getUsername() +
                                "&requiredPermission=system:user&page=/admin/users";
                    }
                    model.addAttribute("title", "用户管理 - SageBlog");
                    break;
                case "roles":
                    if (!hasPermission(userDetails, "system:role")) {
                        return "redirect:/permission-error?username=" + userDetails.getUsername() +
                                "&requiredPermission=system:role&page=/admin/roles";
                    }
                    model.addAttribute("title", "角色管理 - SageBlog");
                    break;
                case "permissions":
                    if (!hasPermission(userDetails, "system:permission")) {
                        return "redirect:/permission-error?username=" + userDetails.getUsername() +
                                "&requiredPermission=system:permission&page=/admin/permissions";
                    }
                    model.addAttribute("title", "权限管理 - SageBlog");
                    break;
                case "menus":
                    if (!hasPermission(userDetails, "system:menu")) {
                        return "redirect:/permission-error?username=" + userDetails.getUsername() +
                                "&requiredPermission=system:menu&page=/admin/menus";
                    }
                    model.addAttribute("title", "菜单管理 - SageBlog");
                    break;
                case "audits":
                    if (!hasPermission(userDetails, "system:audit")) {
                        return "redirect:/permission-error?username=" + userDetails.getUsername() +
                                "&requiredPermission=system:audit&page=/admin/audits";
                    }
                    model.addAttribute("title", "用户审核 - SageBlog");
                    break;
                case "logs":
                    if (!hasPermission(userDetails, "system:log")) {
                        return "redirect:/permission-error?username=" + userDetails.getUsername() +
                                "&requiredPermission=system:log&page=/admin/logs";
                    }
                    model.addAttribute("title", "系统日志 - SageBlog");
                    break;
                default:
                    // 未知页面，返回管理首页
                    model.addAttribute("title", "管理后台 - SageBlog");
                    return "admin/index";
            }

            // 返回对应视图
            return "admin/" + viewName;
        } catch (Exception e) {
            logger.error("处理token参数时出错", e);
            return "redirect:/login?error=系统错误";
        }
    }

    /**
     * 表单组件示例页面
     */
    @GetMapping("/form-examples")
    public String formExamples(Model model) {
        model.addAttribute("title", "表单组件示例 - SageBlog");
        return "admin/form-examples";
    }
}