package com.example.personal_loan.dto;


import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.entity.User;
import com.example.personal_loan.entity.UserCert;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailResponse {
    private User user;
    private UserCert userCert;
    private LoanApplication loanApplication;
    private Order order;
}
