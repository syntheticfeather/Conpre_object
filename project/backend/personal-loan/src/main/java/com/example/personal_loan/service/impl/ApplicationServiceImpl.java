package com.example.personal_loan.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.dto.ApplicationRequest;
import com.example.personal_loan.dto.UserAppListResponse;
import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.LoanOption;
import com.example.personal_loan.entity.LoanProduct;
import com.example.personal_loan.entity.OutboxMessage;
import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.mapper.LoanOptionMapper;
import com.example.personal_loan.mapper.LoanProductMapper;
import com.example.personal_loan.mapper.OutboxMapper;
import com.example.personal_loan.service.ApplicationService;
import com.example.personal_loan.service.AuthService;
import com.example.personal_loan.service.LoanProductService;
import com.example.personal_loan.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 贷款申请服务实现类
 */
@Service
@Slf4j
public class ApplicationServiceImpl implements ApplicationService{

    @Autowired
    private LoanOptionMapper loanOptionMapper;

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private LoanProductMapper loanProductMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private LoanProductService loanProductService;

    @Autowired
    private AuthService authService;

    @Autowired
    private OutboxMapper outboxMapper;

    @Autowired
    private ObjectMapper mapper;

    /**
     * 添加贷款申请
     * @param userId 用户ID
     * @param request 贷款申请请求DTO
     */
    @Override
    @Transactional
    public void addApplication(Long userId, ApplicationRequest request){
        // 校验贷款选项是否存在，且属于该产品
        LoanOption option = loanOptionMapper.selectById(request.getOptionId());
        if (option == null) {
            throw new BusinessException(404, "贷款选项不存在");
        }
        if (!option.getProductId().equals(request.getProductId())) {
            throw new BusinessException(400, "贷款选项与产品不匹配");
        }

        // 校验金额是否符合范围
        LoanProduct product = loanProductMapper.findById(request.getProductId());
        if (product == null) {
            throw new BusinessException(404, "贷款产品不存在");
        }
        if (request.getLoanAmount().compareTo(product.getMinAmount()) < 0 || request.getLoanAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new BusinessException(400, "贷款金额超出范围");
        }

        // 构建申请记录
        LoanApplication application = new LoanApplication();
        application.setUserId(userId);
        application.setProductId(request.getProductId());
        application.setStatus(ApplicationStatus.审核中);
        application.setLoanAmount(request.getLoanAmount());
        application.setInterestRate(option.getInterestRate());
        application.setLoanPeriod(option.getLoanPeriod());
        application.setTerm(request.getTerm());
        application.setRepaidType(option.getRepaidType());
        application.setApplyTime(LocalDateTime.now());

        // 插入数据库
        applicationMapper.insert(application);
        log.info("插入贷款申请记录");
        // 构建 outbox 消息
        OutboxMessage outbox = new OutboxMessage();
        outbox.setMessageId("loan_app_" + application.getId() + "_" + System.currentTimeMillis());
        outbox.setBusinessType("LOAN_APPLICATION");
        outbox.setBusinessId(application.getId());
        outbox.setTopic(RabbitMQConfig.LOAN_APPLICATION_ROUTING_KEY);
        try {
            outbox.setPayload(mapper.writeValueAsString(application));
        } catch (JsonProcessingException e) {
            log.error("序列化 loan application 失败");
            throw new RuntimeException(e);
        }
        outbox.setStatus("PENDING");
        log.info("插入 outbox 消息");
        outboxMapper.insert(outbox); // 同一事务中插入
    }

    /**
     * 撤销贷款申请
     * @param userId 用户ID
     * @param applicationId 申请ID
     */
    @Override
    @Transactional
    public void withdrawApplication(Long userId, Long applicationId){

        LoanApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(404, "申请记录不存在");
        }
        if (!application.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作他人的申请");  // 可选
        }
        if (!ApplicationStatus.审核中.equals(application.getStatus())) {
            throw new BusinessException(400, "仅可撤回待审核的申请");
        }
        // 需要审核？通过后状态变更
        application.setStatus(ApplicationStatus.已取消);
        applicationMapper.update(application);
    }

    /**
     * 用户获取指定贷款申请
     * @param userId 用户ID
     * @param applicationId 申请ID
     * @return 贷款申请实体
     */
    @Override
    @Transactional
    public LoanApplication userGetApplication(Long userId, Long applicationId){
        // 查询申请记录
        LoanApplication app = applicationMapper.selectById(applicationId);
        if (app == null) {
            throw new BusinessException(404, "贷款申请不存在");
        }

        //权限校验
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权查看他人的贷款申请");
        }
        
        return app;
    }


    /**
     * 用户获取所有贷款申请
     * @param userId 用户ID
     * @return 用户贷款申请响应DTO列表
     */
    @Override
    @Transactional
    public List<UserAppListResponse> userGetAllApplications(Long userId){

        List<LoanApplication> applications = applicationMapper.selectByUserId(userId);

        return applications.stream().map(app -> {
            LoanProduct product = loanProductMapper.findById(app.getProductId());
            return new UserAppListResponse(
                    app.getId(),
                    product.getProductName(),
                    app.getLoanAmount(),
                    app.getStatus(),
                    app.getApplyTime(),
                    app.getStatus().isRejected() ? app.getRejectReason() : null
            );
        }).collect(Collectors.toList());
    }

}
