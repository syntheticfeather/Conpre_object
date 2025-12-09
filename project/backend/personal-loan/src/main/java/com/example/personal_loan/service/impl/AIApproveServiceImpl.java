package com.example.personal_loan.service.impl;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.service.AIApproveService;
import com.example.personal_loan.service.AuthService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AIApproveServiceImpl implements AIApproveService {

    @Autowired
    private AuthService authService;

    @Autowired
    private ApplicationMapper applicationMapper;

    @Override
    public Boolean AICheck(LoanApplication application) {
        if (new Random().nextInt(100) < 50) {
            // AI审核成功
            application.setStatus(ApplicationStatus.APPROVED);
            application.setRejectReason("AI approve\n");
            application.setReviewTime(LocalDateTime.now());
            applicationMapper.update(application);
            log.info("AI approve success");
            return true;
        }
        else {
            // AI审核失败
            application.setStatus(ApplicationStatus.AI_REJECTED);
            application.setRejectReason("AI rejected\n");
            applicationMapper.update(application);
            log.info("AI reject success");
            return false;
        }
    }
}
