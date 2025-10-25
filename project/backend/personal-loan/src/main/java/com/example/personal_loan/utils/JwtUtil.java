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
    private long EXPIRATION_TIME;

    public String generateToken(String userPhone,String userId) {
        return Jwts.builder()
                .setSubject(userPhone) //主题：手机号
                .claim("userId", userId) //自定义字段：用户ID
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }
    
    // 提取JWT中的载荷
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(SECRET_KEY)    // 设置签名密钥
            .build()
            .parseClaimsJws(token)    // 解析令牌
            .getBody();               // 获取声明内容
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            throw new RuntimeException("无效的 JWT 签名");
        } 
    // ... 其他异常处理?(要不要放全局)
    }
    
    public String getPhoneFromToken(String token) {
        return getClaims(token).getSubject();  // 从主题中获取手机号
    }

    public Long getUserIdFromToken(String token) {
        return getClaims(token).get("userId", Long.class);  // 获取用户ID
    }
}