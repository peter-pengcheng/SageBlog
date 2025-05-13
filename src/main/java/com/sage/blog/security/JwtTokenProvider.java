package com.sage.blog.security;

import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

// 使用全限定名避免冲突
// import com.sage.blog.entity.User;
import com.sage.blog.service.UserService;

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

    @Autowired
    private UserService userService;

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

        // 获取用户ID
        Long userId = null;
        try {
            if (authentication.getPrincipal() instanceof UserDetails) {
                String username = ((UserDetails) authentication.getPrincipal()).getUsername();
                com.sage.blog.entity.User user = userService.getUserByUsername(username);
                if (user != null) {
                    userId = user.getId();
                }
            }
        } catch (Exception e) {
            logger.error("获取用户ID失败", e);
        }

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("auth", authorities)
                .claim("userId", userId)
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

            // 从claims中获取userId字段
            if (claims.containsKey("userId")) {
                Object userIdObj = claims.get("userId");
                if (userIdObj != null) {
                    // 转换为Long类型
                    if (userIdObj instanceof Integer) {
                        return ((Integer) userIdObj).longValue();
                    } else if (userIdObj instanceof Long) {
                        return (Long) userIdObj;
                    } else if (userIdObj instanceof String) {
                        try {
                            return Long.valueOf((String) userIdObj);
                        } catch (NumberFormatException e) {
                            logger.warn("无法将字符串转换为用户ID: {}", userIdObj);
                        }
                    }
                }
            }

            // 如果无法从claims中获取有效的userId，则尝试通过用户名查询
            String username = claims.getSubject();
            if (username != null) {
                com.sage.blog.entity.User user = userService.getUserByUsername(username);
                if (user != null) {
                    return user.getId();
                }
            }

            // 如果无法获取到用户ID，记录警告并返回null
            logger.warn("无法从JWT令牌中获取有效的用户ID");
            return null;
        } catch (Exception e) {
            logger.error("从JWT令牌中解析用户ID出错", e);
            return null;
        }
    }
}