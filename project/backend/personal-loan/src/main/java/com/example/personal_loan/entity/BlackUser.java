package com.example.personal_loan.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlackUser {
    private Long id;
    private Long userId;

    private int blackLevel;
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
