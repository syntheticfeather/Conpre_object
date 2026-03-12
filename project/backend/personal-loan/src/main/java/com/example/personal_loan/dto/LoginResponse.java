package com.example.personal_loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "登录响应")
public class LoginResponse {
    @Schema(description = "JWT令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;
}
