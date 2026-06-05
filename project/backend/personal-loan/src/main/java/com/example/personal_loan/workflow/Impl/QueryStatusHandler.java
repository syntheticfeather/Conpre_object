package com.example.personal_loan.workflow.Impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.personal_loan.dto.UserAppListResponse;
import com.example.personal_loan.service.ApplicationService;
import com.example.personal_loan.workflow.SseUtil;
import com.example.personal_loan.workflow.WorkflowHandler;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class QueryStatusHandler implements WorkflowHandler {

    @Autowired
    private ApplicationService applicationService;

    @Override
    public String intent() {
        return "QUERY_STATUS";
    }

    @Override
    public SseEmitter handle(String message, Long userId, String sessionId) {
        SseEmitter emitter = new SseEmitter(60_000L);

        try {
            List<UserAppListResponse> apps = applicationService.userGetAllApplications(userId);

            StringBuilder sb = new StringBuilder();
            if (apps == null || apps.isEmpty()) {
                sb.append("您目前还没有提交过贷款申请。\n\n");
                sb.append("如果想了解产品，可以问我\"有哪些贷款产品\"。");
            } else {
                sb.append("您共有 ").append(apps.size()).append(" 笔贷款申请：\n\n");
                for (int i = 0; i < apps.size(); i++) {
                    UserAppListResponse app = apps.get(i);
                    sb.append(i + 1).append(". ").append(app.getProductName());
                    sb.append("  金额：").append(app.getLoanAmount()).append("元");
                    sb.append("  状态：").append(app.getStatusDisplay());
                    sb.append("  时间：").append(app.getApplyTime());
                    if (app.getRejectReason() != null && !app.getRejectReason().isEmpty()) {
                        sb.append("  原因：").append(app.getRejectReason());
                    }
                    sb.append("\n");
                }
            }

            SseUtil.sendMessage(emitter, sb.toString());
            SseUtil.complete(emitter);

        } catch (Exception e) {
            log.error("QUERY_STATUS 失败", e);
            SseUtil.errorReply(emitter, "查询失败，请稍后重试。");
        }

        return emitter;
    }
}
