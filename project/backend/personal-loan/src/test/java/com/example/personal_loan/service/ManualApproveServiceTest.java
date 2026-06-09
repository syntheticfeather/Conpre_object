package com.example.personal_loan.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.personal_loan.dto.ApplicationDetailResponse;
import com.example.personal_loan.dto.ManualCheckResponse;
import com.example.personal_loan.dto.PendingApprovalResponse;
import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.entity.User;
import com.example.personal_loan.entity.UserCert;
import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.mapper.PostponeRequestMapper;
import com.example.personal_loan.mq.NotificationOutboxPublisher;
import com.example.personal_loan.service.impl.ManualApproveServiceImpl;

@ExtendWith(MockitoExtension.class)
class ManualApproveServiceTest {

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserService userService;

    @Mock
    private NotificationOutboxPublisher notificationOutboxPublisher;

    @Mock
    private RepaymentScheduleService repaymentScheduleService;

    @Mock
    private PostponeRequestMapper postponeRequestMapper;

    @InjectMocks
    private ManualApproveServiceImpl manualApproveService;

    private User admin;
    private User user;
    private UserCert userCert;
    private LoanApplication loanApplication;
    private PendingApprovalResponse pendingApproval;
    private ApplicationDetailResponse applicationDetail;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setUserName("admin");
        admin.setPhone("13800138000");
        admin.setRole(1);

        user = new User();
        user.setId(2L);
        user.setUserName("testuser");
        user.setPhone("13900139000");

        userCert = new UserCert();
        userCert.setUserId(2L);
        userCert.setCreditScore(80);

        loanApplication = new LoanApplication();
        loanApplication.setId(1L);
        loanApplication.setUserId(2L);
        loanApplication.setProductId(1L);
        loanApplication.setLoanAmount(new BigDecimal("10000"));
        loanApplication.setInterestRate(new BigDecimal("0.05"));
        loanApplication.setLoanPeriod(12);
        loanApplication.setTerm(12);
        loanApplication.setRepaidType(com.example.personal_loan.enums.RepaidType.等额本息);
        loanApplication.setStatus(ApplicationStatus.审核中);
        loanApplication.setApplyTime(LocalDateTime.now());

        pendingApproval = new PendingApprovalResponse();
        pendingApproval.setApplicationId(1L);
        pendingApproval.setUserName("testuser");
        pendingApproval.setProductName("个人消费贷");
        pendingApproval.setLoanAmount(new BigDecimal("10000"));
        pendingApproval.setLoanPeriod(12);
        pendingApproval.setTerm(12);
        pendingApproval.setStatus(ApplicationStatus.审核中);
        pendingApproval.setApplyTime(LocalDateTime.now());

        applicationDetail = new ApplicationDetailResponse();
        applicationDetail.setUser(user);
        applicationDetail.setUserCert(userCert);
        applicationDetail.setApplication(loanApplication);
    }

    @Test
    void testGetApproves_Success() {
        when(userService.getUserById(1L)).thenReturn(admin);
        when(applicationMapper.listPendingApprovals()).thenReturn(Arrays.asList(pendingApproval));

        List<PendingApprovalResponse> responses = manualApproveService.getApproves(1L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getApplicationId());
        assertEquals("testuser", responses.get(0).getUserName());
    }

    @Test
    void testGetApproves_NotAdmin() {
        admin.setRole(0);
        when(userService.getUserById(1L)).thenReturn(admin);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            manualApproveService.getApproves(1L)
        );
        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("无权限查看代办审核列表"));
    }

    @Test
    void testGetApproves_EmptyList() {
        when(userService.getUserById(1L)).thenReturn(admin);
        when(applicationMapper.listPendingApprovals()).thenReturn(Collections.emptyList());

        List<PendingApprovalResponse> responses = manualApproveService.getApproves(1L);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void testGetApprove_Success() {
        when(userService.getUserById(1L)).thenReturn(admin);
        when(applicationMapper.getApplicationDetail(1L)).thenReturn(applicationDetail);

        ApplicationDetailResponse response = manualApproveService.getApprove(1L, 1L);

        assertNotNull(response);
        assertEquals(user, response.getUser());
        assertEquals(userCert, response.getUserCert());
        assertEquals(loanApplication, response.getApplication());
    }

    @Test
    void testGetApprove_NotAdmin() {
        admin.setRole(0);
        when(userService.getUserById(1L)).thenReturn(admin);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            manualApproveService.getApprove(1L, 1L)
        );
        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("无权限查看代办审核详情"));
    }

    @Test
    void testManualCheck_Approve_Success() {
        when(applicationMapper.selectById(1L)).thenReturn(loanApplication);
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return null;
        });
        doNothing().when(repaymentScheduleService).generateRepaymentSchedule(1L);

        ManualCheckResponse response = manualApproveService.manualCheck(1L, true, null);

        assertNotNull(response);
        assertEquals(1L, response.getLoanApplicationId());
        assertEquals(ApplicationStatus.人工通过, response.getStatus());
        assertEquals("无", response.getRejectReason());
        assertEquals(ApplicationStatus.人工通过, loanApplication.getStatus());
        assertNotNull(response.getReviewTime());
        verify(orderMapper).insert(any(Order.class));
        verify(repaymentScheduleService).generateRepaymentSchedule(1L);
        verify(notificationOutboxPublisher).enqueueNotification(2L, 1L, "LOAN_APPLICATION_STATUS");
    }

    @Test
    void testManualCheck_Reject_WithReason() {
        when(applicationMapper.selectById(1L)).thenReturn(loanApplication);

        ManualCheckResponse response = manualApproveService.manualCheck(1L, false, "资料不完整");

        assertNotNull(response);
        assertEquals(1L, response.getLoanApplicationId());
        assertEquals(ApplicationStatus.人工拒绝, response.getStatus());
        assertTrue(response.getRejectReason().contains("人工审核未通过: 资料不完整"));
        assertEquals(ApplicationStatus.人工拒绝, loanApplication.getStatus());
        verify(notificationOutboxPublisher).enqueueNotification(2L, 1L, "LOAN_APPLICATION_STATUS");
    }

    @Test
    void testManualCheck_Reject_WithoutReason() {
        when(applicationMapper.selectById(1L)).thenReturn(loanApplication);

        ManualCheckResponse response = manualApproveService.manualCheck(1L, false, null);

        assertNotNull(response);
        assertEquals(1L, response.getLoanApplicationId());
        assertEquals(ApplicationStatus.人工拒绝, response.getStatus());
        assertTrue(response.getRejectReason().contains("人工审核未通过: 未填写原因"));
        verify(notificationOutboxPublisher).enqueueNotification(2L, 1L, "LOAN_APPLICATION_STATUS");
    }

    @Test
    void testManualCheck_Reject_WithExistingAIReason() {
        loanApplication.setStatus(ApplicationStatus.AI拒绝);
        loanApplication.setRejectReason("AI审核未通过");
        when(applicationMapper.selectById(1L)).thenReturn(loanApplication);

        ManualCheckResponse response = manualApproveService.manualCheck(1L, false, "人工审核不通过");

        assertNotNull(response);
        assertTrue(response.getRejectReason().contains("AI审核未通过"));
        assertTrue(response.getRejectReason().contains("人工审核未通过: 人工审核不通过"));
        verify(notificationOutboxPublisher).enqueueNotification(2L, 1L, "LOAN_APPLICATION_STATUS");
    }

    @Test
    void testManualCheck_ApplicationNotFound() {
        when(applicationMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            manualApproveService.manualCheck(1L, true, null)
        );
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("贷款申请不存在"));
    }

    @Test
    void testManualCheck_Approve_CreatesOrder() {
        when(applicationMapper.selectById(1L)).thenReturn(loanApplication);
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return null;
        });
        doNothing().when(repaymentScheduleService).generateRepaymentSchedule(1L);

        manualApproveService.manualCheck(1L, true, null);

        verify(orderMapper).insert(argThat(order -> 
            order.getUserId().equals(2L) &&
            order.getProductId().equals(1L) &&
            order.getStatus().equals(OrderStatus.正常) &&
            order.getLoanAmount().equals(new BigDecimal("10000")) &&
            order.getInterestRate().equals(new BigDecimal("0.05")) &&
            order.getLoanPeriod() == 12 &&
            order.getTerm() == 12 &&
            order.getCurrentTerm() == 0 &&
            order.getOverdueDays() == 0 &&
            order.getRepaidAmount().equals(BigDecimal.ZERO)
        ));
        verify(repaymentScheduleService).generateRepaymentSchedule(1L);
    }

    @Test
    void testCompletedApproves_Success() {
        when(userService.getUserById(1L)).thenReturn(admin);
        when(applicationMapper.listCompletedApprovals()).thenReturn(Arrays.asList(pendingApproval));

        List<PendingApprovalResponse> responses = manualApproveService.completedApproves(1L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getApplicationId());
    }

    @Test
    void testCompletedApproves_NotAdmin() {
        admin.setRole(0);
        when(userService.getUserById(1L)).thenReturn(admin);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            manualApproveService.completedApproves(1L)
        );
        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("无权限查看代办审核列表"));
    }

    @Test
    void testCompletedApproves_EmptyList() {
        when(userService.getUserById(1L)).thenReturn(admin);
        when(applicationMapper.listCompletedApprovals()).thenReturn(Collections.emptyList());

        List<PendingApprovalResponse> responses = manualApproveService.completedApproves(1L);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void testManualCheck_Approve_SetsReviewTime() {
        when(applicationMapper.selectById(1L)).thenReturn(loanApplication);
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return null;
        });
        doNothing().when(repaymentScheduleService).generateRepaymentSchedule(1L);

        ManualCheckResponse response = manualApproveService.manualCheck(1L, true, null);

        assertNotNull(response.getReviewTime());
        assertNotNull(loanApplication.getReviewTime());
        verify(repaymentScheduleService).generateRepaymentSchedule(1L);
    }
}
