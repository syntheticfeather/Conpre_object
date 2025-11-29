package com.example.personal_loan.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlackListDto {
    private Long id;
    private Long userId;
    private String userName;
    private String phone;
    private Integer blackLevel;
    private LocalDateTime createTime;  //加入时间
    private LocalDateTime updateTime;
    private LocalDateTime removeTime;   //解除时间
}
