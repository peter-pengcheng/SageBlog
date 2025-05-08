package com.sage.blog.dto;

import com.sage.blog.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtAuthResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private User user;

    public JwtAuthResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    public JwtAuthResponse(String accessToken, User user) {
        this.accessToken = accessToken;
        this.user = user;
    }
}