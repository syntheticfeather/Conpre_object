package com.example.personal_loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(
        description = "用户姓名",
        example = "张三",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Size(min=2,max=20)
    private String name;

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
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%&*()_+=])[A-Za-z\\d!@#$%&*()_+=]{8,20}$",
        message = "密码必须包含大小写字母、数字和特殊字符(!@#$%&*()_+=),长度8-20"
    )
    private String password;
}
