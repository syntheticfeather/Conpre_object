package com.example.personal_loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @Schema(
        description = "用户手机号",
        example = "13800138000",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Pattern(regexp="^1[3-9]\\d{9}$", message="手机号格式不正确")
    @NotBlank(message="手机号不能为空")
    private String phone;

    @Schema(
        description = "用户密码",
        example = "@Zff1234",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message="密码不能为空")
    private String password;
}