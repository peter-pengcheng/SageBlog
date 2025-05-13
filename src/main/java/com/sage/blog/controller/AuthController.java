package com.sage.blog.controller;

import com.sage.blog.common.Result;
import com.sage.blog.dto.JwtAuthResponse;
import com.sage.blog.dto.LoginRequest;
import com.sage.blog.dto.RegisterRequest;
import com.sage.blog.entity.User;
import com.sage.blog.model.ApiResponse;
import com.sage.blog.model.ForgotPasswordRequest;
import com.sage.blog.model.ResetPasswordRequest;
import com.sage.blog.service.AuthService;
import com.sage.blog.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 认证相关接口
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result<JwtAuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            logger.info("用户登录请求: {}", loginRequest.getUsername());

            // 验证用户状态
            User user = userService.getUserByUsername(loginRequest.getUsername());
            if (user != null && user.getStatus() == 0) {
                logger.warn("账号正在审核中: {}", loginRequest.getUsername());
                return Result.failed("您的账号正在审核中，请等待管理员审核通过后再尝试登录");
            }

            // 生成JWT令牌
            String token = authService.login(loginRequest.getUsername(), loginRequest.getPassword());
            if (token == null) {
                logger.error("登录失败，令牌生成失败: {}", loginRequest.getUsername());
                return Result.failed("登录失败，请稍后再试");
            }

            // 获取用户信息 - 确保使用登录用户的用户名而不是从token中获取
            user = userService.getUserByUsername(loginRequest.getUsername());
            if (user == null) {
                logger.error("登录后无法获取用户信息: {}", loginRequest.getUsername());
                return Result.failed("登录后获取用户信息失败");
            }

            // 移除敏感信息
            user.setPassword(null);

            logger.info("用户登录成功: {}", loginRequest.getUsername());

            // 返回令牌和用户信息
            JwtAuthResponse response = new JwtAuthResponse(token, user);
            return Result.success(response);
        } catch (org.springframework.security.authentication.DisabledException e) {
            // 处理账号待审核的情况
            logger.warn("账号被禁用: {}, 原因: {}", loginRequest.getUsername(), e.getMessage());
            return Result.failed(e.getMessage());
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            // 处理用户名或密码错误的情况
            logger.warn("用户名或密码错误: {}", loginRequest.getUsername());
            return Result.failed("用户名或密码错误");
        } catch (org.springframework.security.authentication.LockedException e) {
            // 处理账号被锁定的情况
            logger.warn("账号被锁定: {}", loginRequest.getUsername());
            return Result.failed("您的账号已被锁定，请联系管理员");
        } catch (Exception e) {
            logger.error("用户登录异常: {}", loginRequest.getUsername(), e);
            return Result.failed("登录失败，请稍后再试");
        }
    }

    /**
     * 用户注册
     *
     * @param registerRequest 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest registerRequest) {
        // 检查密码是否一致
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            return Result.validateFailed("两次输入密码不一致");
        }

        // 构建用户实体
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(registerRequest.getPassword());
        user.setNickname(registerRequest.getNickname());
        user.setEmail(registerRequest.getEmail());
        user.setPhone(registerRequest.getPhone());

        // 注册用户
        boolean success = authService.register(user);
        if (success) {
            return Result.success();
        } else {
            return Result.failed("注册失败，用户名或邮箱可能已存在");
        }
    }

    /**
     * 刷新令牌
     *
     * @param token 原令牌
     * @return 新令牌
     */
    @PostMapping("/refresh")
    public Result<JwtAuthResponse> refreshToken(@RequestParam String token) {
        String refreshedToken = authService.refreshToken(token);
        if (refreshedToken == null) {
            return Result.failed("令牌已过期或无效");
        }
        return Result.success(new JwtAuthResponse(refreshedToken));
    }

    /**
     * 发送密码重置邮件
     *
     * @param request 请求参数
     * @return 操作结果
     */
    @PostMapping("/forgot-password")
    public ApiResponse<?> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        logger.info("收到找回密码请求: {}", request.getEmail());

        // 验证邮箱是否存在
        User user = userService.findByEmail(request.getEmail());
        if (user == null) {
            return ApiResponse.error("该邮箱未注册");
        }

        // 创建密码重置令牌并发送邮件
        String token = userService.createPasswordResetToken(request.getEmail());
        if (token == null) {
            return ApiResponse.error("发送重置邮件失败，请稍后重试");
        }

        return ApiResponse.success("重置链接已发送到您的邮箱，请查收");
    }

    /**
     * 验证重置密码令牌
     *
     * @param token 重置令牌
     * @return 令牌是否有效
     */
    @GetMapping("/validate-reset-token")
    public ApiResponse<?> validateResetToken(@RequestParam String token) {
        Long userId = userService.validatePasswordResetToken(token);
        if (userId == null) {
            return ApiResponse.error("重置链接已过期或无效");
        }

        return ApiResponse.success(true);
    }

    /**
     * 重置密码
     *
     * @param request 重置请求
     * @return 操作结果
     */
    @PostMapping("/reset-password")
    public ApiResponse<?> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        // 验证令牌
        Long userId = userService.validatePasswordResetToken(request.getToken());
        if (userId == null) {
            return ApiResponse.error("重置链接已过期或无效");
        }

        // 重置密码
        boolean result = userService.resetPassword(request.getToken(), request.getPassword());
        if (!result) {
            return ApiResponse.error("重置密码失败，请稍后重试");
        }

        return ApiResponse.success("密码重置成功");
    }

    /**
     * 用户登出
     *
     * @return 操作结果
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        // 获取当前用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            logger.info("用户 {} 登出", userDetails.getUsername());
        }

        // 清除安全上下文
        SecurityContextHolder.clearContext();

        return Result.success();
    }
}