package com.sage.blog.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "令牌不能为空")
    private String token;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}