package com.sage.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_password_reset_token")
public class PasswordResetToken {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String token;

    private LocalDateTime expiryDate;

    private LocalDateTime createTime;

    private Integer status; // 0-未使用，1-已使用

    /**
     * 检查令牌是否已过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}