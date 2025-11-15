package com.example.personal_loan.service;

public interface AIApprovceService {

    // AI审核
    Boolean AIApprovce();

    // 转交人工审核
    void sendToManualApprove();
}
