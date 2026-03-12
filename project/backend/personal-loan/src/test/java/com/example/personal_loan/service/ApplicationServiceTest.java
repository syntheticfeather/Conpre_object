package com.example.personal_loan.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.personal_loan.dto.ApplicationRequest;
import com.example.personal_loan.dto.UserGetAppResponse;
import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.LoanOption;
import com.example.personal_loan.entity.LoanProduct;
import com.example.personal_loan.entity.OutboxMessage;
import com.example.personal_loan.entity.User;
import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.mapper.LoanOptionMapper;
import com.example.personal_loan.mapper.LoanProductMapper;
import com.example.personal_loan.mapper.OutboxMapper;
import com.example.personal_loan.service.impl.ApplicationServiceImpl;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private LoanOptionMapper loanOptionMapper;

    @Mock
    private LoanProductMapper loanProductMapper;

    @Mock
    private OutboxMapper outboxMapper;

    @Mock
    private AuthService authService;

    @Mock
    private LoanProductService loanProductService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    private LoanApplication loanApplication;
    private LoanProduct loanProduct;
    private LoanOption loanOption;
    private User user;
    private ApplicationRequest request;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUserName("testuser");
        user.setPhone("13800138000");

        loanProduct = new LoanProduct();
        loanProduct.setId(1L);
        loanProduct.setProductName("个人消费贷");
        loanProduct.setMinAmount(new BigDecimal("1000"));
        loanProduct.setMaxAmount(new BigDecimal("50000"));
        loanProduct.setMinTerm(3);
        loanProduct.setMaxTerm(24);

        loanOption = new LoanOption();
        loanOption.setOptionId(1L);
        loanOption.setProductId(1L);
        loanOption.setInterestRate(new BigDecimal("0.05"));
        loanOption.setLoanPeriod(12);
        loanOption.setRepaidType(com.example.personal_loan.enums.RepaidType.等额本息);

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

        request = new ApplicationRequest();
        request.setProductId(1L);
        request.setLoanAmount(new BigDecimal("10000"));
        request.setOptionId(1L);
    }

    @Test
    void testAddApplication_Success() {
        when(loanOptionMapper.selectById(1L)).thenReturn(loanOption);
        doAnswer(invocation -> {
            LoanApplication app = invocation.getArgument(0);
            app.setId(1L);
            return null;
        }).when(applicationMapper).insert(any(LoanApplication.class));
        doAnswer(invocation -> {
            OutboxMessage msg = invocation.getArgument(0);
            msg.setId(1L);
            return null;
        }).when(outboxMapper).insert(any(OutboxMessage.class));

        assertDoesNotThrow(() -> applicationService.addApplication(1L, request));

        verify(applicationMapper).insert(any(LoanApplication.class));
        verify(outboxMapper).insert(any(OutboxMessage.class));
    }

    @Test
    void testAddApplication_OptionNotFound() {
        when(loanOptionMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            applicationService.addApplication(1L, request)
        );
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("贷款选项不存在"));
    }

    @Test
    void testAddApplication_OptionNotBelongToProduct() {
        LoanOption otherOption = new LoanOption();
        otherOption.setOptionId(1L);
        otherOption.setProductId(2L);

        when(loanOptionMapper.selectById(1L)).thenReturn(otherOption);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            applicationService.addApplication(1L, request)
        );
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("贷款选项与产品不匹配"));
    }

    @Test
    void testWithdrawApplication_Success() {
        loanApplication.setStatus(ApplicationStatus.审核中);
        when(applicationMapper.selectById(1L)).thenReturn(loanApplication);

        assertDoesNotThrow(() -> applicationService.withdrawApplication(1L, 1L));

        verify(applicationMapper).update(any(LoanApplication.class));
    }

    @Test
    void testWithdrawApplication_ApplicationNotFound() {
        when(applicationMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            applicationService.withdrawApplication(1L, 1L)
        );
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("申请不存在"));
    }

    @Test
    void testWithdrawApplication_NotOwner() {
        loanApplication.setUserId(2L);
        when(applicationMapper.selectById(1L)).thenReturn(loanApplication);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            applicationService.withdrawApplication(1L, 1L)
        );
        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("无权操作"));
    }

    @Test
    void testWithdrawApplication_NotPending() {
        loanApplication.setStatus(ApplicationStatus.已通过);
        when(applicationMapper.selectById(1L)).thenReturn(loanApplication);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            applicationService.withdrawApplication(1L, 1L)
        );
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("只能撤回审核中的申请"));
    }

    @Test
    void testUserGetApplication_Success() {
        when(applicationMapper.selectById(1L)).thenReturn(loanApplication);
        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        UserGetAppResponse response = applicationService.userGetApplication(1L, 1L);

        assertNotNull(response);
        assertEquals(new BigDecimal("10000"), response.getLoanAmount());
    }

    @Test
    void testUserGetApplication_NotOwner() {
        loanApplication.setUserId(2L);
        when(applicationMapper.selectById(1L)).thenReturn(loanApplication);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            applicationService.userGetApplication(1L, 1L)
        );
        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("无权查看"));
    }

    @Test
    void testUserGetApplication_NotFound() {
        when(applicationMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            applicationService.userGetApplication(1L, 1L)
        );
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("申请不存在"));
    }

    @Test
    void testUserGetAllApplications_Success() {
        when(applicationMapper.selectByUserId(1L)).thenReturn(Arrays.asList(loanApplication));
        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        List<UserGetAppResponse> responses = applicationService.userGetAllApplications(1L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void testUserGetAllApplications_EmptyList() {
        when(applicationMapper.selectByUserId(1L)).thenReturn(Collections.emptyList());

        List<UserGetAppResponse> responses = applicationService.userGetAllApplications(1L);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void testAdminGetApplication_Success() {
        when(applicationMapper.selectById(1L)).thenReturn(loanApplication);
        when(userService.getUserById(1L)).thenReturn(user);
        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        var response = applicationService.adminGetApplication(1L);

        assertNotNull(response);
    }

    @Test
    void testAdminGetApplication_NotFound() {
        when(applicationMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            applicationService.adminGetApplication(1L)
        );
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("申请不存在"));
    }
}
