package com.example.personal_loan.dto;

import com.example.personal_loan.entity.ImmovablesCert;
import com.example.personal_loan.entity.TriCert;
import com.example.personal_loan.entity.UserCert;
import com.example.personal_loan.entity.WorkCert;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetCertResponse {
    private UserCert userCert;
    private WorkCert workCert;
    private TriCert triCert;
    private ImmovablesCert immovablesCert;
}
