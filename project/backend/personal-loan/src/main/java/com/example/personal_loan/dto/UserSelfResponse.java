package com.example.personal_loan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSelfResponse {
    private Long userId;
    private String userName;
    private String avatar;

}
