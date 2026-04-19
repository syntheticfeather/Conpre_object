package com.example.personal_loan.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.personal_loan.dto.ApiResult;
import com.example.personal_loan.dto.ApprovalTypeStatistics;
import com.example.personal_loan.dto.MonthlyStatistics;
import com.example.personal_loan.mapper.ApplicationMapper;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private ApplicationMapper applicationMapper;

    // 获取每月申请量统计
    @GetMapping("/monthly-applications")
    public ApiResult<List<MonthlyStatistics>> getMonthlyApplications() {
        List<MonthlyStatistics> statistics = applicationMapper.countMonthlyApplications();
        return ApiResult.success(statistics);
    }

    // 获取每月通过量统计
    @GetMapping("/monthly-approvals")
    public ApiResult<List<MonthlyStatistics>> getMonthlyApprovals() {
        List<MonthlyStatistics> statistics = applicationMapper.countMonthlyApprovals();
        return ApiResult.success(statistics);
    }

    // 获取每月AI通过和人工通过的数量统计
    @GetMapping("/approval-types")
    public ApiResult<List<ApprovalTypeStatistics>> getApprovalTypesByMonth() {
        List<ApprovalTypeStatistics> statistics = applicationMapper.countApprovalTypesByMonth();
        return ApiResult.success(statistics);
    }
}
