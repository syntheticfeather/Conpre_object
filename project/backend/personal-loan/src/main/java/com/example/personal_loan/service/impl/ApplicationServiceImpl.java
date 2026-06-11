package com.example.personal_loan.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.aop.RedisLocked;
import com.example.personal_loan.dto.ApplicationRequest;
import com.example.personal_loan.dto.UserAppListResponse;
import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.LoanOption;
import com.example.personal_loan.entity.LoanProduct;
import com.example.personal_loan.entity.OutboxMessage;
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
import com.example.personal_loan.service.ApplicationService;
import com.example.personal_loan.service.AuthService;
import com.example.personal_loan.service.LoanProductService;
import com.example.personal_loan.service.MlTrainingLogService;
import com.example.personal_loan.service.RiskScoringService;
import com.example.personal_loan.service.UserService;
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
    private UserCertMapper userCertMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private LoanProductService loanProductService;

    @Autowired
    private AuthService authService;

    @Autowired
    private OutboxMapper outboxMapper;

    @Autowired
    private OutboxMessageFactory outboxMessageFactory;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private NotificationOutboxPublisher notificationOutboxPublisher;

    @Autowired
    private MlTrainingLogService mlTrainingLogService;

    @Autowired
    private RiskScoringService riskScoringService;

    /**
     * 添加贷款申请
     * @param userId 用户ID
     * @param request 贷款申请请求DTO
     */
    @Override
    @Transactional
    @RedisLocked(key = "'lock:loan-application:add:user:' + #p0")
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
        // 检验是否实名认证
        UserCert userCert = userCertMapper.selectByUserId(userId);
        if (userCert.getRealName() == null || userCert.getIdCard() == null || userCert.getBankCardId() == null) {
            throw new BusinessException(400, "请先完成实名认证");
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
        OutboxMessage outbox = outboxMessageFactory.create(BusinessType.LOAN_APPLICATION, application, application.getId());
        log.info("插入 outbox 消息");
        outboxMapper.insert(outbox); // 同一事务中插入

        notificationOutboxPublisher.enqueueNotification(userId, application.getId(), "LOAN_APPLICATION_STATUS");

        // 🆕 XGBoost 风控打分（失败不阻塞主流程）
        try {
            Map<String, Object> modelFeatures = mlTrainingLogService.extractModelFeatures(
                userId, application.getId());

            int creditScore;
            if (riskScoringService.isReady()) {
                int realCount = riskScoringService.countRealFeatures(modelFeatures);
                if (realCount < 3) {
                    creditScore = 0;  // 数据不足，直接拒绝
                    log.info("风控: userId={} 数据质量不足 realFeatures={}/9 → 信用分=0",
                             userId, realCount);
                } else {
                    double prob = riskScoringService.predict(modelFeatures);
                    creditScore = riskScoringService.toCreditScore(prob);
                    log.info("风控: userId=" + userId
                             + String.format(" defaultProb=%.4f", prob)
                             + " creditScore=" + creditScore
                             + " realFeatures=" + realCount + "/9");
                }
            } else {
                creditScore = 375;  // 模型未就绪，默认中等分
                log.warn("风控: userId={} 模型未就绪 → 默认分 375", userId);
            }

            // 写入 user_certification
            userCert.setCreditScore(creditScore);
            userCertMapper.update(userCert);

        } catch (Exception e) {
            log.error("风控打分失败 userId={}: {}", userId, e.getMessage(), e);
        }

        // 🆕 采集 ML 训练特征（失败不阻塞主流程）
        mlTrainingLogService.collectFeatures(userId, application.getId());
    }

    /**
     * 撤销贷款申请
     * @param userId 用户ID
     * @param applicationId 申请ID
     */
    @Override
    @Transactional
    // @RedisLocked(key = "'lock:loan-application:withdraw:' + #p1")
    public void withdrawApplication(Long userId, Long applicationId){

        LoanApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(404, "申请记录不存在");
        }
        if (!application.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作他人的申请");  // 可选
        }
        if (!(ApplicationStatus.审核中.equals(application.getStatus())||ApplicationStatus.AI拒绝.equals(application.getStatus()))) {
            throw new BusinessException(400, "仅可撤回待审核的申请");
        }
        // 需要审核？通过后状态变更
        application.setStatus(ApplicationStatus.已取消);
        applicationMapper.update(application);

        notificationOutboxPublisher.enqueueNotification(userId, applicationId, "LOAN_APPLICATION_STATUS");
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
        return applicationMapper.selectByUserIdWithProduct(userId);
    }

}
