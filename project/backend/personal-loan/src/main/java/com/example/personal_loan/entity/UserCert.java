package com.example.personal_loan.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCert {
    private Long userId;
    private Integer creditScore;
    private String idCard;
    private Integer workCertId;
    private Integer triCertId;
    private Integer immovableCertId;
    private Integer bankCardId;
}
