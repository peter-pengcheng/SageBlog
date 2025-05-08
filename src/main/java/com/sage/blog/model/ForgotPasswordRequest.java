package com.sage.blog.model;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * 找回密码请求 DTO
 */
public class ForgotPasswordRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}