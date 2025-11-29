package com.example.personal_loan.service.impl;

import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.service.AIApproveService;
import com.example.personal_loan.service.AuthService;

public class AIApproveServiceImpl implements AIApproveService {

    private AuthService authService;

    private ApplicationMapper applicationMapper;

    @Override
    public Boolean AICheck() {
        // TODO
        return true;
    }
    
}
