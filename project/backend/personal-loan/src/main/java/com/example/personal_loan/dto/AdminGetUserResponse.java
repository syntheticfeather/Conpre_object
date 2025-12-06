package com.example.personal_loan.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminGetUserResponse {
    private Long userId;
    private String userName;
    private String avatar;
    private String phone; // 管理员可见
    private Integer role;
    private Integer creditScore;      // 来自 user_certification
    private Integer blackLevel;       // 来自 black_list，若无则为 0

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
