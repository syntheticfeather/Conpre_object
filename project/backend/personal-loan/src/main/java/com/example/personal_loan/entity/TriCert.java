package com.example.personal_loan.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TriCert {
    private Integer triCertId;
    private String socialSecurityPath;   //社保证明
    private String creditReportPath;     //征信报告
}
