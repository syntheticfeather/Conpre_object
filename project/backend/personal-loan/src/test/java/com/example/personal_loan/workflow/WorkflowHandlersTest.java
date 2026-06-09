package com.example.personal_loan.workflow;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.personal_loan.dto.LoanOptionResponse;
import com.example.personal_loan.dto.UserAppListResponse;
import com.example.personal_loan.dto.UserGetProductResponse;
import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.enums.ChatIntent;
import com.example.personal_loan.enums.RepaidType;
import com.example.personal_loan.service.ApplicationService;
import com.example.personal_loan.service.LoanProductService;
import com.example.personal_loan.utils.CalculateUtil;
import com.example.personal_loan.workflow.Impl.ApplyLoanHandler;
import com.example.personal_loan.workflow.Impl.CalculateHandler;
import com.example.personal_loan.workflow.Impl.ListProductsHandler;
import com.example.personal_loan.workflow.Impl.QueryStatusHandler;

/**
 * Workflow Handler 单元测试（Mock Service，不连数据库）
 */
@ExtendWith(MockitoExtension.class)
class WorkflowHandlersTest {

    // ========== 各 Handler 依赖的 Mock ==========

    @Mock private ApplicationService applicationService;
    @Mock private LoanProductService loanProductService;
    @Mock private CalculateUtil calculateUtil;

    @InjectMocks private QueryStatusHandler queryStatusHandler;
    @InjectMocks private ListProductsHandler listProductsHandler;
    @InjectMocks private ApplyLoanHandler applyLoanHandler;

    // CalculateHandler 需要单独构造，因为它还有@Autowired的CalculateUtil
    private CalculateHandler calculateHandler;

    @BeforeEach
    void setUp() {
        calculateHandler = new CalculateHandler();
        // 通过反射注入 mock 的 CalculateUtil
        try {
            var field = CalculateHandler.class.getDeclaredField("calculateUtil");
            field.setAccessible(true);
            field.set(calculateHandler, calculateUtil);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ================================================================
    // QUERY_STATUS
    // ================================================================

    @Test
    void queryStatus_noApplications() throws Exception {
        when(applicationService.userGetAllApplications(1L)).thenReturn(List.of());

        SseEmitter emitter = queryStatusHandler.handle("查进度", 1L, "s1");
        assertNotNull(emitter);
        // SseEmitter 没法直接读内容，但至少不抛异常
    }

    @Test
    void queryStatus_hasApplications() throws Exception {
        var app = new UserAppListResponse(
                1001L, "个人消费贷", new BigDecimal("200000"),
                ApplicationStatus.审核中, LocalDateTime.of(2026, 5, 20, 10, 0), null
        );
        when(applicationService.userGetAllApplications(1L)).thenReturn(List.of(app));

        SseEmitter emitter = queryStatusHandler.handle("查进度", 1L, "s1");
        assertNotNull(emitter);
    }

    @Test
    void queryStatus_intent() {
        assertEquals("QUERY_STATUS", queryStatusHandler.intent());
    }

    // ================================================================
    // LIST_PRODUCTS
    // ================================================================

    @Test
    void listProducts_hasData() throws Exception {
        var product = new UserGetProductResponse(
                1L, "个人消费贷", "用于个人消费", "消费",
                null, new BigDecimal("10000"), new BigDecimal("500000"),
                List.of(12, 24, 36), new ArrayList<>()
        );
        when(loanProductService.getTopLoanProducts(5)).thenReturn(List.of(product));

        SseEmitter emitter = listProductsHandler.handle("有哪些产品", 1L, "s1");
        assertNotNull(emitter);
    }

    @Test
    void listProducts_empty() throws Exception {
        when(loanProductService.getTopLoanProducts(5)).thenReturn(List.of());

        SseEmitter emitter = listProductsHandler.handle("有哪些产品", 1L, "s1");
        assertNotNull(emitter);
    }

    @Test
    void listProducts_intent() {
        assertEquals("LIST_PRODUCTS", listProductsHandler.intent());
    }

    // ================================================================
    // APPLY_LOAN
    // ================================================================

    @Test
    void applyLoan_missingProduct() throws Exception {
        when(loanProductService.getTopLoanProducts(5)).thenReturn(List.of());

        SseEmitter emitter = applyLoanHandler.handle("我想申请贷款", 1L, "s1");
        assertNotNull(emitter);
    }

    @Test
    void applyLoan_hasProduct() throws Exception {
        var opt = new LoanOptionResponse(1L, new BigDecimal("4.5"), 36, RepaidType.等额本息);
        var product = new UserGetProductResponse(
                1L, "个人消费贷", "消费用途", "消费",
                null, new BigDecimal("10000"), new BigDecimal("500000"),
                List.of(12, 24, 36), List.of(opt)
        );
        when(loanProductService.getTopLoanProducts(5)).thenReturn(List.of(product));

        SseEmitter emitter = applyLoanHandler.handle("我要申请个人消费贷50万", 1L, "s1");
        assertNotNull(emitter);
    }

    @Test
    void applyLoan_intent() {
        assertEquals("APPLY_LOAN", applyLoanHandler.intent());
    }

    // ================================================================
    // CALCULATE
    // ================================================================

    @Test
    void calculate_missingParams() throws Exception {
        SseEmitter emitter = calculateHandler.handle("算月供", 1L, "s1");
        assertNotNull(emitter);
    }

    @Test
    void calculate_enoughParams() throws Exception {
        var schedule = new com.example.personal_loan.entity.RepaymentSchedule();
        schedule.setTotalAmount(new BigDecimal("5949.38"));
        schedule.setPrincipal(new BigDecimal("5500.00"));
        schedule.setInterest(new BigDecimal("449.38"));

        // 修复后：extractRate 返回 0.045（小数），不再是 4.5（百分比）
        when(calculateUtil.calculateRepaymentPlan(
                new BigDecimal("200000"),           // 20万 = 200000
                new BigDecimal("0.045"),            // 年利率 4.5% → 小数 0.045
                36,                                 // 36期
                RepaidType.等额本息,
                java.time.LocalDate.now()
        )).thenReturn(List.of(schedule));

        SseEmitter emitter = calculateHandler.handle("20万36期，利率4.5%，等额本息", 1L, "s1");
        assertNotNull(emitter);
    }

    @Test
    void calculate_intent() {
        assertEquals("CALCULATE", calculateHandler.intent());
    }

    // ================================================================
    // ChatRouterService
    // ================================================================

    @Test
    void router_unknownIntent() {
        var router = new ChatRouterService(List.of(queryStatusHandler));
        assertTrue(router.hasHandler(ChatIntent.QUERY_STATUS));
        // 未知 intent 不会抛异常
        SseEmitter emitter = router.handleWorkflow(ChatIntent.UNKNOWN, "test", 1L, "s1");
        assertNotNull(emitter);
    }
}
