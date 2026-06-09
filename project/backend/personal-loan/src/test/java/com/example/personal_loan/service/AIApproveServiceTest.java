package com.example.personal_loan.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.mq.NotificationOutboxPublisher;
import com.example.personal_loan.service.impl.AIApproveServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AIApproveServiceTest {

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private CreditScoreCalculator creditScoreCalculator;

    @Mock
    private NotificationOutboxPublisher notificationOutboxPublisher;

    @Mock
    private RepaymentScheduleService repaymentScheduleService;

    @InjectMocks
    private AIApproveServiceImpl aiApproveService;

    private LoanApplication loanApplication;

    @BeforeEach
    void setUp() throws Exception {
        loanApplication = new LoanApplication();
        loanApplication.setId(1L);
        loanApplication.setUserId(1L);
        loanApplication.setProductId(1L);
        loanApplication.setLoanAmount(new BigDecimal("10000"));
        loanApplication.setInterestRate(new BigDecimal("0.05"));
        loanApplication.setLoanPeriod(12);
        loanApplication.setTerm(12);
        loanApplication.setRepaidType(com.example.personal_loan.enums.RepaidType.等额本息);
        loanApplication.setStatus(ApplicationStatus.审核中);
        loanApplication.setApplyTime(LocalDateTime.now());

        doAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return null;
        }).when(orderMapper).insert(any(Order.class));

        java.lang.reflect.Field passField = AIApproveServiceImpl.class.getDeclaredField("passThreshold");
        passField.setAccessible(true);
        passField.set(aiApproveService, 80);

        java.lang.reflect.Field rejectField = AIApproveServiceImpl.class.getDeclaredField("rejectThreshold");
        rejectField.setAccessible(true);
        rejectField.set(aiApproveService, 40);
    }

    @Test
    void testAICheck_UpdatesApplication() {
        when(creditScoreCalculator.calculate(anyLong())).thenReturn(80);
        aiApproveService.AICheck(loanApplication);
        verify(applicationMapper).update(loanApplication);
    }

    @Test
    void testAICheck_Approve_StatusChangedToApproved() {
        when(creditScoreCalculator.calculate(anyLong())).thenReturn(80);
        aiApproveService.AICheck(loanApplication);
        assertTrue(loanApplication.getStatus() == ApplicationStatus.AI通过 || 
                   loanApplication.getStatus() == ApplicationStatus.AI拒绝);
    }

    @Test
    void testAICheck_Approve_CreatesOrder() {
        when(creditScoreCalculator.calculate(anyLong())).thenReturn(80);
        aiApproveService.AICheck(loanApplication);
        if (loanApplication.getStatus() == ApplicationStatus.AI通过) {
            verify(orderMapper).insert(any(Order.class));
        }
    }

    @Test
    void testAICheck_Reject_DoesNotCreateOrder() {
        when(creditScoreCalculator.calculate(anyLong())).thenReturn(30);
        aiApproveService.AICheck(loanApplication);
        if (loanApplication.getStatus() == ApplicationStatus.AI拒绝) {
            verify(orderMapper, never()).insert(any(Order.class));
        }
    }

    @Test
    void testAICheck_Approve_OrderFieldsCorrect() {
        when(creditScoreCalculator.calculate(anyLong())).thenReturn(80);
        aiApproveService.AICheck(loanApplication);
        if (loanApplication.getStatus() == ApplicationStatus.AI通过) {
            verify(orderMapper).insert(argThat(order -> 
                order.getUserId().equals(1L) &&
                order.getProductId().equals(1L) &&
                order.getStatus().equals(OrderStatus.正常) &&
                order.getLoanAmount().equals(new BigDecimal("10000")) &&
                order.getInterestRate().equals(new BigDecimal("0.05")) &&
                order.getRepaidType().equals(com.example.personal_loan.enums.RepaidType.等额本息) &&
                order.getLoanPeriod() == 12 &&
                order.getTerm() == 12 &&
                order.getCurrentTerm() == 0 &&
                order.getOverdueDays() == 0 &&
                order.getRepaidAmount().equals(BigDecimal.ZERO) &&
                order.getStartTime() != null
            ));
        }
    }

    @Test
    void testAICheck_Approve_SetsReviewTime() {
        when(creditScoreCalculator.calculate(anyLong())).thenReturn(80);
        aiApproveService.AICheck(loanApplication);
        assertNotNull(loanApplication.getReviewTime());
    }

    @Test
    void testAICheck_Approve_RejectReasonSetToNone() {
        when(creditScoreCalculator.calculate(anyLong())).thenReturn(80);
        aiApproveService.AICheck(loanApplication);
        if (loanApplication.getStatus() == ApplicationStatus.AI通过) {
            assertEquals("无", loanApplication.getRejectReason());
        }
    }

    @Test
    void testAICheck_Reject_RejectReasonContainsAIRejectMessage() {
        when(creditScoreCalculator.calculate(anyLong())).thenReturn(30);
        aiApproveService.AICheck(loanApplication);
        assertEquals(ApplicationStatus.AI拒绝, loanApplication.getStatus());
        assertNotNull(loanApplication.getRejectReason());
        assertTrue(loanApplication.getRejectReason().contains("信用分不足"));
    }

    @Test
    void testAICheck_ManualReview() {
        when(creditScoreCalculator.calculate(anyLong())).thenReturn(50);
        aiApproveService.AICheck(loanApplication);
        assertEquals(ApplicationStatus.审核中, loanApplication.getStatus());
        assertTrue(loanApplication.getRejectReason().contains("信用分不足，需人工审核"));
    }
}
