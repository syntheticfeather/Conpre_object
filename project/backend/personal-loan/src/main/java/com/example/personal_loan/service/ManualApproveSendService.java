package com.example.personal_loan.service;

public interface  ManualApproveSendService {
    void sendToManualApprove(); // 保存待处理人工审核

    void getApproves(); // 获得所有需审核申请

    void getApprove(); // 获得单个审核申请详情

    void autoDistriApprove(); // 自动向审核员发送

    void distriApprove(); // 指定审核员发放

    Boolean ManualCheck(); // 返回审核结果？待定，先不写这个多层审核功能

}
