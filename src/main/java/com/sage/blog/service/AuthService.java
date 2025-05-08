package com.sage.blog.service;

import com.sage.blog.entity.User;

public interface AuthService {

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return JWT令牌
     */
    String login(String username, String password);

    /**
     * 用户注册
     *
     * @param user 用户信息
     * @return 是否成功
     */
    boolean register(User user);

    /**
     * 刷新令牌
     *
     * @param token 原令牌
     * @return 新令牌
     */
    String refreshToken(String token);

    /**
     * 发起忘记密码流程
     *
     * @param email 用户邮箱
     * @return 是否成功
     */
    boolean forgotPassword(String email);

    /**
     * 验证密码重置令牌
     *
     * @param token 令牌
     * @return 是否有效
     */
    boolean validatePasswordResetToken(String token);

    /**
     * 重置密码
     *
     * @param token    重置令牌
     * @param password 新密码
     * @return 是否成功
     */
    boolean resetPassword(String token, String password);
}