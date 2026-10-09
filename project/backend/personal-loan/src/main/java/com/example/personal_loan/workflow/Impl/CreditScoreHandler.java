package com.example.personal_loan.workflow.Impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.personal_loan.dto.UserSelfResponse;
import com.example.personal_loan.service.UserService;
import com.example.personal_loan.workflow.SseUtil;
import com.example.personal_loan.workflow.WorkflowHandler;
import com.example.personal_loan.workflow.WorkflowResult;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CreditScoreHandler implements WorkflowHandler {

    @Autowired
    private UserService userService;

    @Override
    public String intent() {
        return "QUERY_CREDIT";
    }

    @Override
    public WorkflowResult handle(String message, Long userId, String sessionId) {
        SseEmitter emitter = SseUtil.newEmitter(sessionId);

        try {
            UserSelfResponse userInfo = userService.getUserSelfInfo(userId);
            Integer score = userInfo.getCreditScore();

            StringBuilder sb = new StringBuilder();
            sb.append("您的当前信用分为：").append(score).append(" 分\n\n");

            // 信用分等级说明
            if (score >= 700) {
                sb.append("信用等级：优秀。您的信用状况良好，可申请多数贷款产品，享受较优利率。");
            } else if (score >= 600) {
                sb.append("信用等级：良好。您可以申请大部分贷款产品，具体利率以审批结果为准。");
            } else if (score >= 500) {
                sb.append("信用等级：一般。部分产品可能受限，建议保持按时还款以提升信用分。");
            } else {
                sb.append("信用等级：较低。可申请的产品较少，建议先改善信用状况后再申请。");
            }
            sb.append("\n\n注：信用分仅供参考，最终以征信机构报告为准。");

            SseUtil.sendMessage(emitter, sb.toString());
            SseUtil.complete(emitter);
            return WorkflowResult.of(emitter, sb.toString());

        } catch (Exception e) {
            log.error("QUERY_CREDIT 失败", e);
            return SseUtil.errorReply(emitter, "查询信用分失败，请稍后重试。");
        }
    }
}
