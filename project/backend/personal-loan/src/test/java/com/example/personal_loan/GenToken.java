package com.example.personal_loan;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
public class GenToken {
    public static void main(String[] args) {
        String secret = "5H6fK9pL2mN8qR4sT7vW1xY3zA0bC2dE4fG6hJ8kL0nM2oP4qR6sT8uV0wX2yZ4";
        String token = Jwts.builder()
            .setSubject("17777777777")
            .claim("userId", "4")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 86400000))
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact();
        System.out.println(token);
    }
}
