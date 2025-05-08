package com.sage.blog.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 重置密码请求 DTO
 */
public class ResetPasswordRequest {

    @NotBlank(message = "令牌不能为空")
    private String token;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6到20个字符之间")
    private String password;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}