package com.example.personal_loan.entity;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;

    @Size(min=2,max=20,message="用户名长度必须在2-20之间")
    private String userName;

    private String avatar;

    @Pattern(regexp="^1[3-9]\\d{9}$",message="手机号格式不正确")
    @NotBlank(message="手机号不能为空")
    private String phone;

    @NotBlank(message="密码不能为空")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
        message = "密码必须包含大小写字母、数字和特殊字符,长度8-20"
    )
    private String password;

    // 用户权限 (0是客户)
    private Integer role;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public User(String name, String password, String phone) {
        this.userName = name;
        this.password = password;
        this.phone = phone;
    }
}
