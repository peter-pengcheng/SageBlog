package com.sage.blog.security;

import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * JWT令牌提供者
 * 负责生成、验证和解析JWT令牌
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${sageblog.jwt.secret}")
    private String jwtSecret;

    @Value("${sageblog.jwt.expiration}")
    private int jwtExpiration;

    /**
     * 生成JWT令牌
     *
     * @param authentication 认证信息
     * @return JWT令牌
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration * 1000);

        // 获取用户权限信息
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("auth", authorities)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    /**
     * 从JWT令牌中获取用户名
     *
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    /**
     * 从JWT令牌中获取认证信息
     *
     * @param token JWT令牌
     * @return 认证信息
     */
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();

        // 获取用户权限信息
        Collection<? extends GrantedAuthority> authorities = Arrays.stream(claims.get("auth").toString().split(","))
                .filter(auth -> !auth.trim().isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // 构建用户详情
        UserDetails principal = new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    /**
     * 验证JWT令牌是否有效
     *
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            // 令牌为空，无需记录错误
            return false;
        }

        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            // 提供更详细的过期信息
            try {
                Date expiration = ex.getClaims().getExpiration();
                Date now = new Date();
                long diffInMillies = now.getTime() - expiration.getTime();
                long diffInMinutes = diffInMillies / (60 * 1000);

                logger.error("Expired JWT token: expired at {}, {} minutes ago",
                        expiration, diffInMinutes);
            } catch (Exception e) {
                logger.error("Expired JWT token, unable to calculate expiration details: {}",
                        ex.getMessage());
            }
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * 从JWT令牌中获取用户ID
     *
     * @param token JWT令牌
     * @return 用户ID
     */
    public Long getUserIdFromJWT(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token)
                    .getBody();

            // 如果claims中包含用户ID字段，则返回该字段值
            if (claims.containsKey("userId")) {
                return Long.valueOf(claims.get("userId").toString());
            }

            // 否则从用户名中获取ID（假设用户名以"user_"开头，例如"user_123"）
            String username = claims.getSubject();
            if (username != null && username.contains("_")) {
                try {
                    String idStr = username.split("_")[1];
                    return Long.valueOf(idStr);
                } catch (Exception e) {
                    logger.warn("无法从用户名{}中提取用户ID", username);
                }
            }

            // 如果无法获取ID，返回默认值1（通常是管理员ID）
            // 在实际使用中，应该确保JWT中包含用户ID或使用其他方式获取
            return 1L;
        } catch (Exception e) {
            logger.error("从JWT令牌中解析用户ID出错", e);
            return 1L; // 默认返回管理员ID
        }
    }
}