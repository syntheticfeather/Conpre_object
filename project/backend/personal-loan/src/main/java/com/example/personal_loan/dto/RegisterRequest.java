package com.example.personal_loan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @Size(min=2,max=20)
    private String name;

    @Pattern(regexp="^1[3-9]\\d{9}$", message="手机号格式不正确")
    @NotBlank(message="手机号不能为空")
    private String phone;

    @NotBlank(message="密码不能为空")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
        message = "密码必须包含大小写字母、数字和特殊字符,长度8-20"
    )
    private String password;
}
