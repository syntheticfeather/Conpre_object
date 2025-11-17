package com.example.personal_loan.service.impl;

import com.example.personal_loan.mapper.ApproveMapper;
import com.example.personal_loan.service.AIApproveService;
import com.example.personal_loan.service.AuthService;

public class AIApproveServiceImpl implements AIApproveService {

    private AuthService authService;

    private ApproveMapper approveMapper;

    @Override
    public Boolean AICheck() {
        // TODO
        return true;
    }
    
}
