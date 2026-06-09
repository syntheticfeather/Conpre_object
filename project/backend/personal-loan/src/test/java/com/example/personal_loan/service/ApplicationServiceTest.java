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
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.personal_loan.dto.ApplicationRequest;
import com.example.personal_loan.dto.UserAppListResponse;
import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.LoanOption;
import com.example.personal_loan.entity.LoanProduct;
import com.example.personal_loan.entity.OutboxMessage;
import com.example.personal_loan.entity.User;
import com.example.personal_loan.entity.UserCert;
import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.enums.BusinessType;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.factory.OutboxMessageFactory;
import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.mapper.LoanOptionMapper;
import com.example.personal_loan.mapper.LoanProductMapper;
import com.example.personal_loan.mapper.OutboxMapper;
import com.example.personal_loan.mapper.UserCertMapper;
import com.example.personal_loan.mq.NotificationOutboxPublisher;
import com.example.personal_loan.service.impl.ApplicationServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    private UserCertMapper userCertMapper;

    @Mock
    private AuthService authService;

    @Mock
    private UserService userService;

    @Mock
    private OutboxMessageFactory outboxMessageFactory;

    @Mock
    private NotificationOutboxPublisher notificationOutboxPublisher;

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

        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);
    }

    @Test
    void testAddApplication_Success() {
        when(loanOptionMapper.selectById(1L)).thenReturn(loanOption);
        UserCert userCert = new UserCert();
        userCert.setRealName("测试用户");
        userCert.setIdCard("110101199001011234");
        userCert.setBankCardId("6222021234567890");
        when(userCertMapper.selectByUserId(1L)).thenReturn(userCert);
        doAnswer(invocation -> {
            LoanApplication app = invocation.getArgument(0);
            app.setId(1L);
            return null;
        }).when(applicationMapper).insert(any(LoanApplication.class));
        when(outboxMessageFactory.create(any(BusinessType.class), any(), anyLong())).thenReturn(new OutboxMessage());
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
        assertTrue(exception.getMessage().contains("申请记录不存在"));
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
        loanApplication.setStatus(ApplicationStatus.AI通过);
        when(applicationMapper.selectById(1L)).thenReturn(loanApplication);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            applicationService.withdrawApplication(1L, 1L)
        );
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("仅可撤回待审核的申请"));
    }

    @Test
    void testUserGetApplication_Success() {
        when(applicationMapper.selectById(1L)).thenReturn(loanApplication);

        LoanApplication response = applicationService.userGetApplication(1L, 1L);

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
        assertTrue(exception.getMessage().contains("无权查看他人的贷款申请"));
    }

    @Test
    void testUserGetApplication_NotFound() {
        when(applicationMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            applicationService.userGetApplication(1L, 1L)
        );
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("贷款申请不存在"));
    }

    @Test
    void testUserGetAllApplications_Success() {
        UserAppListResponse response = new UserAppListResponse();
        response.setApplicationId(1L);
        response.setLoanAmount(new BigDecimal("10000"));
        when(applicationMapper.selectByUserIdWithProduct(1L)).thenReturn(Arrays.asList(response));

        List<UserAppListResponse> responses = applicationService.userGetAllApplications(1L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void testUserGetAllApplications_EmptyList() {
        when(applicationMapper.selectByUserIdWithProduct(1L)).thenReturn(Collections.emptyList());

        List<UserAppListResponse> responses = applicationService.userGetAllApplications(1L);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

}
