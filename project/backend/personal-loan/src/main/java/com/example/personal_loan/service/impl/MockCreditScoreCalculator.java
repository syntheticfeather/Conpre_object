package com.example.personal_loan.service.impl;

import java.util.Random;

import org.springframework.stereotype.Service;

import com.example.personal_loan.service.CreditScoreCalculator;

@Service
public class MockCreditScoreCalculator implements CreditScoreCalculator {

    @Override
    public int calculate(Long userId) {
        // TODO: 以后替换成真正的ML模型
        return new Random().nextInt(100);
    }
}