package com.example.personal_loan.workflow.Impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.personal_loan.dto.LoanOptionResponse;
import com.example.personal_loan.dto.LoanParams;
import com.example.personal_loan.dto.UserGetProductResponse;
import com.example.personal_loan.service.AgentClientService;
import com.example.personal_loan.service.LoanProductService;
import com.example.personal_loan.utils.RedisUtil;
import com.example.personal_loan.workflow.SseUtil;
import com.example.personal_loan.workflow.WorkflowHandler;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ApplyLoanHandler implements WorkflowHandler {

    private static final String REDIS_PREFIX = "loan:params:";
    private static final int TTL_MINUTES = 10;

    @Autowired private LoanProductService loanProductService;
    @Autowired private AgentClientService agentClient;
    @Autowired private RedisUtil redisUtil;

    @Override public String intent() { return "APPLY_LOAN"; }

    @Override
    public SseEmitter handle(String message, Long userId, String sessionId) {
        SseEmitter emitter = new SseEmitter(60_000L);

        try {
            // 全量产品
            List<UserGetProductResponse> allProducts = loanProductService.getAllLoanProducts();
            List<String> allNames = allProducts.stream().map(UserGetProductResponse::getProductName).toList();
            List<String> topNames = allNames.size() > 5 ? allNames.subList(0, 5) : allNames;

            // ★ 跨轮累积：加载 Redis 中已有参数，合并本轮新提取的参数
            LoanParams existing = loadSessionParams(sessionId);
            LoanParams extracted = agentClient.extractLoanParams(message, allNames);

            LoanParams merged = merge(existing, extracted, allNames);

            // 产品名缺失 → 引导
            if (merged.getProductName() == null) {
                saveSessionParams(sessionId, merged);
                SseUtil.sendMessage(emitter, buildMissingProduct(topNames, merged));
                SseUtil.complete(emitter);
                return emitter;
            }

            // 产品名无效
            ProductInfo product = findProduct(allProducts, merged.getProductName());
            if (product == null) {
                SseUtil.sendMessage(emitter,
                        "未找到产品\"" + merged.getProductName() + "\"，可以问我\"有哪些产品\"。");
                SseUtil.complete(emitter);
                return emitter;
            }

            // 金额缺失 → 引导
            if (merged.getAmount() == null) {
                saveSessionParams(sessionId, merged);
                SseUtil.sendMessage(emitter,
                        "该产品额度范围 " + product.minAmount + "~" + product.maxAmount
                        + " 元，您想贷多少？");
                SseUtil.complete(emitter);
                return emitter;
            }

            // 金额校验
            BigDecimal amount = BigDecimal.valueOf(merged.getAmount());
            if (amount.compareTo(product.minAmount) < 0 || amount.compareTo(product.maxAmount) > 0) {
                SseUtil.sendMessage(emitter,
                        "额度范围 " + product.minAmount + "~" + product.maxAmount
                        + " 元，您输入的 " + amount + " 不在范围内。请重新输入。");
                SseUtil.complete(emitter);
                return emitter;
            }

            // 齐全 → 检查是否在选择 Option
            int optionIndex = extractOptionIndex(message, product.options.size());
            if (optionIndex >= 0) {
                // 用户选了 Option → 提交
                redisUtil.delete(REDIS_PREFIX + sessionId);
                var chosen = product.options.get(optionIndex);
                SseUtil.sendMessage(emitter,
                        "已为您提交申请！\n"
                        + "产品：" + product.name + "\n"
                        + "金额：" + amount + "元\n"
                        + "方案：" + chosen.getLoanPeriod() + "期 "
                        + chosen.getInterestRate() + "% "
                        + chosen.getRepaidType().name() + "\n\n"
                        + "[实际提交需调用 ApplicationService.submit()，当前为演示]");
                SseUtil.complete(emitter);
            } else {
                // 还没选 → 确认卡（参数保留，等用户选）
                saveSessionParams(sessionId, merged);
                SseUtil.sendMessage(emitter, buildConfirmCard(product, amount));
                SseUtil.complete(emitter);
            }

        } catch (Exception e) {
            log.error("APPLY_LOAN 失败", e);
            SseUtil.errorReply(emitter, "处理失败，请稍后重试。");
        }
        return emitter;
    }

    // ===== 跨轮参数管理 =====

    private LoanParams loadSessionParams(String sessionId) {
        try {
            LoanParams p = (LoanParams) redisUtil.get(REDIS_PREFIX + sessionId);
            return p != null ? p : new LoanParams();
        } catch (Exception e) {
            return new LoanParams();
        }
    }

    private void saveSessionParams(String sessionId, LoanParams params) {
        redisUtil.set(REDIS_PREFIX + sessionId, params, TTL_MINUTES, TimeUnit.MINUTES);
    }

    /** 合并：新参数覆盖旧参数的空字段 */
    private LoanParams merge(LoanParams old, LoanParams fresh, List<String> allNames) {
        old.setProductName(or(old.getProductName(), fuzzyMatch(fresh.getProductName(), allNames)));
        old.setAmount(or(old.getAmount(), fresh.getAmount()));
        return old;
    }

    private <T> T or(T existing, T incoming) {
        return existing != null ? existing : incoming;
    }

    // ===== 引导文案 =====

    private String buildMissingProduct(List<String> topNames, LoanParams merged) {
        StringBuilder sb = new StringBuilder();
        if (merged.hasAny()) {
            sb.append("已记录您输入的信息。");
        } else {
            sb.append("好的，帮您申请贷款。");
        }
        sb.append("请补充以下信息：\n\n");
        sb.append("- 您想申请哪个产品？可选：");
        for (String n : topNames) sb.append(n).append("、");
        sb.setLength(sb.length() - 1);
        if (topNames.size() > 1) {
            sb.append("\n例如\"我要申请").append(topNames.get(0)).append("\"。");
        }
        return sb.toString();
    }

    private String buildConfirmCard(ProductInfo product, BigDecimal amount) {
        StringBuilder sb = new StringBuilder();
        sb.append("确认贷款申请：\n\n");
        sb.append("产品：").append(product.name).append("\n");
        sb.append("金额：").append(amount).append("元\n");
        sb.append("额度范围：").append(product.minAmount).append("~").append(product.maxAmount).append("元\n\n");
        sb.append("请选择方案（期限+利率+还款方式）：\n");
        for (int i = 0; i < product.options.size(); i++) {
            var o = product.options.get(i);
            sb.append("  ").append(i + 1).append(". ");
            sb.append(o.getLoanPeriod()).append("期  ");
            sb.append("利率").append(o.getInterestRate()).append("%  ");
            sb.append(o.getRepaidType().name()).append("\n");
        }
        sb.append("\n回复方案编号即可，例如\"选第1个\"。");
        return sb.toString();
    }

    // ===== 辅助 =====

    private ProductInfo findProduct(List<UserGetProductResponse> all, String name) {
        return all.stream().filter(p -> name.equals(p.getProductName()))
                .findFirst().map(ProductInfo::new).orElse(null);
    }

    /** 从消息中提取方案编号，如"选第1个"→0, "第3个"→2。未匹配返回 -1 */
    private int extractOptionIndex(String message, int optionCount) {
        var p = java.util.regex.Pattern.compile("第\\s*(\\d+)\\s*个");
        var m = p.matcher(message);
        if (m.find()) {
            int idx = Integer.parseInt(m.group(1)) - 1;
            return (idx >= 0 && idx < optionCount) ? idx : -1;
        }
        return -1;
    }

    private String fuzzyMatch(String userSaid, List<String> names) {
        if (userSaid == null) return null;
        for (String n : names) if (n.equals(userSaid)) return n;
        for (String n : names) if (n.contains(userSaid)) return n;
        for (String n : names) if (userSaid.contains(n)) return n;
        return null;
    }

    private record ProductInfo(String name, BigDecimal minAmount, BigDecimal maxAmount,
                                List<LoanOptionResponse> options) {
        ProductInfo(UserGetProductResponse p) {
            this(p.getProductName(), p.getMinAmount(), p.getMaxAmount(), p.getOptions());
        }
    }
}
