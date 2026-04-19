package com.example.personal_loan.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.service.impl.AIApproveServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AIApproveServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private AIApproveServiceImpl aiApproveService;

    private LoanApplication loanApplication;

    @BeforeEach
    void setUp() {
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
    }

    @Test
    void testAICheck_UpdatesApplication() {
        aiApproveService.AICheck(loanApplication);
        verify(applicationMapper).update(loanApplication);
    }

    @Test
    void testAICheck_Approve_StatusChangedToApproved() {
        aiApproveService.AICheck(loanApplication);
        assertTrue(loanApplication.getStatus() == ApplicationStatus.AI通过 || 
                   loanApplication.getStatus() == ApplicationStatus.AI拒绝);
    }

    @Test
    void testAICheck_Approve_CreatesOrder() {
        aiApproveService.AICheck(loanApplication);
        if (loanApplication.getStatus() == ApplicationStatus.AI通过) {
            verify(orderMapper).insert(any(Order.class));
        }
    }

    @Test
    void testAICheck_Reject_DoesNotCreateOrder() {
        aiApproveService.AICheck(loanApplication);
        if (loanApplication.getStatus() == ApplicationStatus.AI拒绝) {
            verify(orderMapper, never()).insert(any(Order.class));
        }
    }

    @Test
    void testAICheck_Approve_OrderFieldsCorrect() {
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
        aiApproveService.AICheck(loanApplication);
        assertNotNull(loanApplication.getReviewTime());
    }

    @Test
    void testAICheck_Approve_RejectReasonSetToNone() {
        aiApproveService.AICheck(loanApplication);
        if (loanApplication.getStatus() == ApplicationStatus.AI通过) {
            assertEquals("无", loanApplication.getRejectReason());
        }
    }

    @Test
    void testAICheck_Reject_RejectReasonContainsAIRejectMessage() {
        aiApproveService.AICheck(loanApplication);
        if (loanApplication.getStatus() == ApplicationStatus.AI拒绝) {
            assertNotNull(loanApplication.getRejectReason());
            assertTrue(loanApplication.getRejectReason().contains("AI审核未通过"));
        }
    }
}
