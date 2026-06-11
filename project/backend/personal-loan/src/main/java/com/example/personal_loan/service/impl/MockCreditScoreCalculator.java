package com.example.personal_loan.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.personal_loan.entity.UserCert;
import com.example.personal_loan.mapper.UserCertMapper;
import com.example.personal_loan.service.CreditScoreCalculator;

import lombok.extern.slf4j.Slf4j;

/**
 * 信用分读取器
 *
 * 从 user_certification.credit_score 读取 XGBoost 模型打分结果。
 * 分数由 ApplicationServiceImpl 在申请提交时计算并写入。
 */
@Slf4j
@Service
public class MockCreditScoreCalculator implements CreditScoreCalculator {

    @Autowired
    private UserCertMapper userCertMapper;

    @Override
    public int calculate(Long userId) {
        UserCert cert = userCertMapper.selectByUserId(userId);
        if (cert != null && cert.getCreditScore() != null) {
            log.debug("读取信用分: userId={} score={}", userId, cert.getCreditScore());
            return cert.getCreditScore();
        }
        log.warn("信用分缺失: userId={} 使用默认分 375", userId);
        return 375;
    }
}
