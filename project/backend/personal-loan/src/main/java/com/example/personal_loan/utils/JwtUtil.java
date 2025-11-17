package com.example.personal_loan.utils;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String SECRET_KEY;
    @Value("${jwt.expiration}")
    private long ACCESS_EXPIRATION_TIME;
    @Value("${jwt.refresh.expiration}") // refresh token 过期时间
    private long REFRESH_EXPIRATION_TIME;

    // 生成access token
    public String generateToken(String userPhone, String userId) {
        return Jwts.builder()
                .setSubject(userPhone) //主题：userPhone
                .claim("userId", userId) // 声明：userId
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    // 生成refresh token
    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .setSubject(userId) //subject 是 userId
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    // 提取JWT中的载荷
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY) // 设置签名密钥
                .build()
                .parseClaimsJws(token) // 解析令牌
                .getBody();               // 获取声明内容
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            return false;
        }
        // ... 其他异常处理?
    }

    public String getPhoneFromToken(String token) {
        return getClaims(token).getSubject();
    }

    // 获取用户ID
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        Object userIdObj = claims.get("userId");
    
        if (userIdObj == null) {
            return null;
        }
    
        String userIdStr = claims.get("userId", String.class);
        Long userId = Long.parseLong(userIdStr); // 安全地转回 Long
        return userId;
    }

    // 从 refresh token 中提取 userId
    public Long getUserIdFromRefreshToken(String refreshToken) {
        try {
            String subject = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody()
                    .getSubject();
            return Long.parseLong(subject);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
    }

    // 验证 refresh token 是否有效
    public boolean validateRefreshToken(String refreshToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(refreshToken);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
