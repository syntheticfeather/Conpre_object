package com.example.personal_loan.workflow.Impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.personal_loan.dto.LoanOptionResponse;
import com.example.personal_loan.dto.UserGetProductResponse;
import com.example.personal_loan.service.LoanProductService;
import com.example.personal_loan.workflow.SseUtil;
import com.example.personal_loan.workflow.WorkflowHandler;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ListProductsHandler implements WorkflowHandler {

    private static final int TOP_K = 5;

    @Autowired
    private LoanProductService loanProductService;

    @Override
    public String intent() {
        return "LIST_PRODUCTS";
    }

    @Override
    public SseEmitter handle(String message, Long userId, String sessionId) {
        SseEmitter emitter = new SseEmitter(60_000L);

        try {
            List<UserGetProductResponse> products = loanProductService.getTopLoanProducts(TOP_K);

            StringBuilder sb = new StringBuilder();
            if (products == null || products.isEmpty()) {
                sb.append("目前暂无可申请的贷款产品，请稍后再来查看。");
            } else {
                sb.append("为您展示最新上架的 ").append(products.size()).append(" 款产品：\n\n");
                for (int i = 0; i < products.size(); i++) {
                    UserGetProductResponse p = products.get(i);
                    sb.append(i + 1).append(". ").append(p.getProductName());
                    sb.append("  额度：").append(p.getMinAmount()).append("~").append(p.getMaxAmount()).append("元");
                    sb.append("\n");
                    if (p.getDescription() != null) {
                        sb.append("   ").append(p.getDescription()).append("\n");
                    }
                    if (p.getOptions() != null && !p.getOptions().isEmpty()) {
                        sb.append("   可选方案：");
                        for (LoanOptionResponse opt : p.getOptions()) {
                            sb.append(opt.getLoanPeriod()).append("期");
                            sb.append("(利率").append(opt.getInterestRate()).append("%) ");
                        }
                        sb.append("\n");
                    }
                    sb.append("\n");
                }
                sb.append("如需申请，直接告诉我\"我要申请XX产品\"即可。");
                sb.append("不确定哪个适合？告诉我\"帮我推荐\"，我来帮你分析。");
            }

            SseUtil.sendMessage(emitter, sb.toString());
            SseUtil.complete(emitter);

        } catch (Exception e) {
            log.error("LIST_PRODUCTS 失败", e);
            SseUtil.errorReply(emitter, "查询产品列表失败，请稍后重试。");
        }

        return emitter;
    }
}
