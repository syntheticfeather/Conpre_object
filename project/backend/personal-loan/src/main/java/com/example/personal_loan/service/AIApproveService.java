package com.example.personal_loan.service;

import com.example.personal_loan.entity.LoanApplication;

public interface AIApproveService {
    Boolean AICheck(LoanApplication application);
}
