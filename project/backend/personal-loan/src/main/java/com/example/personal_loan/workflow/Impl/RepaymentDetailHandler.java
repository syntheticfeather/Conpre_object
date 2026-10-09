package com.example.personal_loan.workflow.Impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.personal_loan.dto.UserOrderListResponse;
import com.example.personal_loan.entity.RepaymentSchedule;
import com.example.personal_loan.service.OrderService;
import com.example.personal_loan.service.RepaymentScheduleService;
import com.example.personal_loan.workflow.SseUtil;
import com.example.personal_loan.workflow.WorkflowHandler;
import com.example.personal_loan.workflow.WorkflowResult;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RepaymentDetailHandler implements WorkflowHandler {

    @Autowired
    private OrderService orderService;

    @Autowired
    private RepaymentScheduleService repaymentScheduleService;

    @Override
    public String intent() {
        return "QUERY_REPAYMENT";
    }

    @Override
    public WorkflowResult handle(String message, Long userId, String sessionId) {
        SseEmitter emitter = SseUtil.newEmitter(sessionId);

        try {
            List<UserOrderListResponse> orders = orderService.userGetAllOrders(userId);

            if (orders == null || orders.isEmpty()) {
                String text = "您目前还没有贷款订单，暂无还款明细可查。";
                SseUtil.sendMessage(emitter, text);
                SseUtil.complete(emitter);
                return WorkflowResult.of(emitter, text);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("您共有 ").append(orders.size()).append(" 笔贷款订单，还款明细如下：\n\n");

            for (UserOrderListResponse order : orders) {
                sb.append("━━━ 订单 #").append(order.getId()).append(" ━━━\n");
                sb.append("借款金额：").append(order.getLoanAmount()).append("元");
                sb.append("  总期数：").append(order.getTerm()).append("期");
                sb.append("  当前期数：第").append(order.getCurrentTerm()).append("期");
                sb.append("  状态：").append(order.getStatus());
                if (order.getOverdueDays() != null && order.getOverdueDays() > 0) {
                    sb.append("  逾期：").append(order.getOverdueDays()).append("天");
                }
                sb.append("\n");

                // 查该订单的还款计划
                List<RepaymentSchedule> schedule = repaymentScheduleService.getRepaymentSchedule(order.getId());
                if (schedule != null && !schedule.isEmpty()) {
                    sb.append("还款计划：\n");
                    for (RepaymentSchedule r : schedule) {
                        sb.append("  第").append(r.getTerm()).append("期");
                        sb.append("  应还：").append(r.getTotalAmount()).append("元");
                        sb.append("(本金").append(r.getPrincipal());
                        sb.append("+利息").append(r.getInterest()).append(")");
                        sb.append("  到期日：").append(r.getDueDate());
                        sb.append("  状态：").append(r.getStatus());
                        if (r.getActualPayDate() != null) {
                            sb.append("  实还日：").append(r.getActualPayDate());
                        }
                        sb.append("\n");
                    }
                } else {
                    sb.append("还款计划尚未生成。\n");
                }
                sb.append("\n");
            }

            SseUtil.sendMessage(emitter, sb.toString());
            SseUtil.complete(emitter);
            return WorkflowResult.of(emitter, sb.toString());

        } catch (Exception e) {
            log.error("QUERY_REPAYMENT 失败", e);
            return SseUtil.errorReply(emitter, "查询还款明细失败，请稍后重试。");
        }
    }
}
