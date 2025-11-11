package com.example.personal_loan.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkCert {
    private Integer workCertId;
    private String employmentCertPath;
    private String salaryCertPath;
}
