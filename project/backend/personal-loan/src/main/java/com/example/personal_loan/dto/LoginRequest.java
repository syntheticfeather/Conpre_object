package com.example.personal_loan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @Pattern(regexp="^1[3-9]\\d{9}$", message="手机号格式不正确")
    @NotBlank(message="手机号不能为空")
    private String phone;

    @NotBlank(message="密码不能为空")
    private String password;
}