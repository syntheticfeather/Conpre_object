package com.example.personal_loan.workflow.Impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.personal_loan.entity.RepaymentSchedule;
import com.example.personal_loan.enums.RepaidType;
import com.example.personal_loan.utils.CalculateUtil;
import com.example.personal_loan.workflow.SseUtil;
import com.example.personal_loan.workflow.WorkflowHandler;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CalculateHandler implements WorkflowHandler {

    @Autowired
    private CalculateUtil calculateUtil;

    private static final Pattern AMOUNT = Pattern.compile("(\\d+)\\s*万");
    private static final Pattern MONTHS = Pattern.compile("(\\d+)\\s*(年|期|个月)");
    private static final Pattern RATE   = Pattern.compile("利率\\s*(\\d+\\.?\\d*)");
    private static final Pattern TYPE_EQ   = Pattern.compile("等额本息");
    private static final Pattern TYPE_PRINC = Pattern.compile("等额本金");

    @Override
    public String intent() {
        return "CALCULATE";
    }

    @Override
    public SseEmitter handle(String message, Long userId, String sessionId) {
        SseEmitter emitter = new SseEmitter(60_000L);

        try {
            BigDecimal amount = extractAmount(message);
            Integer months = extractMonths(message);
            BigDecimal annualRate = extractRate(message);
            RepaidType type = extractType(message);

            // 参数不全 → 引导
            if (amount == null || months == null || annualRate == null) {
                StringBuilder prompt = new StringBuilder("好的，我来帮您计算月供。还需要确认以下信息：\n\n");
                if (amount == null)   prompt.append("- 贷款金额是多少？（比如\"20万\"）\n");
                if (months == null)   prompt.append("- 分多少期？（比如\"36期\"或\"3年\"）\n");
                if (annualRate == null) prompt.append("- 利率是多少？（如果不清楚，我可以帮您查产品的标准利率）\n");
                prompt.append("\n您可以一次性告诉我，比如\"20万36期，利率4.5%，等额本息\"。");
                SseUtil.sendMessage(emitter, prompt.toString());
                SseUtil.complete(emitter);
                return emitter;
            }

            // 参数齐全 → 调 CalculateUtil 生成完整还款计划
            List<RepaymentSchedule> plan = calculateUtil.calculateRepaymentPlan(
                    amount, annualRate, months, type, LocalDate.now()
            );

            BigDecimal firstMonth = plan.get(0).getTotalAmount();
            boolean sameEachMonth = type != RepaidType.等额本金;  // 等额本金月供递减
            BigDecimal total = plan.stream()
                    .map(RepaymentSchedule::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            StringBuilder result = new StringBuilder();
            result.append("计算结果如下：\n\n");
            result.append("贷款金额：").append(amount).append("元\n");
            result.append("贷款期限：").append(months).append("期（").append(months / 12).append("年）\n");
            result.append("年利率：").append(annualRate).append("%\n");
            result.append("还款方式：").append(type.name()).append("\n\n");

            if (sameEachMonth) {
                result.append("每月还款：").append(firstMonth).append("元\n");
            } else {
                BigDecimal lastMonth = plan.get(plan.size() - 1).getTotalAmount();
                result.append("首月还款：").append(firstMonth).append("元\n");
                result.append("末月还款：").append(lastMonth).append("元\n");
            }
            result.append("总还款额：").append(total).append("元\n");
            result.append("总利息：").append(total.subtract(amount)).append("元\n\n");
            result.append("这是估算结果，实际利率以审批为准。如需申请，告诉我即可。");

            SseUtil.sendMessage(emitter, result.toString());
            SseUtil.complete(emitter);

        } catch (Exception e) {
            log.error("CALCULATE 失败", e);
            SseUtil.errorReply(emitter, "计算失败，请稍后重试。");
        }

        return emitter;
    }

    // ===== 参数提取 =====

    private BigDecimal extractAmount(String msg) {
        Matcher m = AMOUNT.matcher(msg);
        if (m.find()) {
            return new BigDecimal(m.group(1)).multiply(BigDecimal.valueOf(10000));
        }
        Matcher m2 = Pattern.compile("(\\d{4,})\\s*元").matcher(msg);
        if (m2.find()) {
            return new BigDecimal(m2.group(1));
        }
        return null;
    }

    private Integer extractMonths(String msg) {
        Matcher m = MONTHS.matcher(msg);
        if (m.find()) {
            String num = m.group(1);
            String unit = m.group(2);
            if ("年".equals(unit)) {
                return Integer.parseInt(num) * 12;
            }
            return Integer.parseInt(num);
        }
        return null;
    }

    private BigDecimal extractRate(String msg) {
        Matcher m = RATE.matcher(msg);
        if (m.find()) {
            return new BigDecimal(m.group(1))
                    .divide(BigDecimal.valueOf(100), 6, java.math.RoundingMode.HALF_UP);
        }
        return null;
    }

    private RepaidType extractType(String msg) {
        if (TYPE_PRINC.matcher(msg).find()) return RepaidType.等额本金;
        return RepaidType.等额本息;  // 默认
    }
}
