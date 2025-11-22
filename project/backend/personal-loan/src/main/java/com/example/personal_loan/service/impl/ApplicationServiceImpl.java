package com.example.personal_loan.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.dto.AdminGetAppResponse;
import com.example.personal_loan.dto.ApplicationRequest;
import com.example.personal_loan.dto.UserGetAppResponse;
import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.LoanOption;
import com.example.personal_loan.entity.LoanProduct;
import com.example.personal_loan.entity.User;
import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.mapper.LoanOptionMapper;
import com.example.personal_loan.mapper.LoanProductMapper;
import com.example.personal_loan.service.ApplicationService;
import com.example.personal_loan.service.AuthService;
import com.example.personal_loan.service.LoanProductService;
import com.example.personal_loan.service.UserService;

@Service
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

        // 构建申请记录
        LoanApplication application = new LoanApplication();
        application.setUserId(userId);
        application.setProductId(request.getProductId());
        application.setStatus(ApplicationStatus.PENDING);
        application.setLoanAmount(option.getLoanAmount());
        application.setInterestRate(option.getInterestRate());
        application.setLoanPeriod(option.getLoanPeriod());
        application.setTerm(request.getTerm());
        application.setRepaidType(option.getRepaidType());
        application.setApplyTime(LocalDateTime.now());

        // 插入数据库
        applicationMapper.insert(application);
    }

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
        if (!ApplicationStatus.PENDING.equals(application.getStatus())) {
            throw new BusinessException(400, "仅可撤回待审核的申请");
        }
        // 需要审核？通过后状态变更
        application.setStatus(ApplicationStatus.CANCELLED);
        applicationMapper.updateStatus(application); 
    }

    // @Override
    // public void updateApplication(Long userId, Long proId){

    // }

    @Override
    @Transactional
    public UserGetAppResponse userGetApplication(Long userId, Long applicationId){
        // 查询申请记录
        LoanApplication app = applicationMapper.selectById(applicationId);
        if (app == null) {
            throw new BusinessException(404, "贷款申请不存在");
        }

        //权限校验
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权查看他人的贷款申请");
        }
        LoanProduct product = loanProductMapper.findById(app.getProductId());

        return new UserGetAppResponse(
            app.getId(),
            product.getProductName(),
            app.getLoanAmount(),
            app.getInterestRate(),
            app.getLoanPeriod(),
            app.getTerm(),
            app.getRepaidType(),
            app.getStatus(),
            app.getApplyTime(),
            app.getReviewTime(),
            app.getRejectReason()
        );
    }

    @Override
    @Transactional
    public AdminGetAppResponse adminGetApplication(Long applicationId){

        LoanApplication app = applicationMapper.selectById(applicationId);
        if (app == null) throw new BusinessException(404, "申请不存在");

        LoanProduct product = loanProductMapper.findById(app.getProductId());
        User user = userService.getUserById(app.getUserId()); 

        return new AdminGetAppResponse(
            app.getId(), app.getUserId(), app.getProductId(),
            user.getUserName(), user.getPhone(),product.getProductName(),
            app.getLoanAmount(), app.getInterestRate(),
            app.getLoanPeriod(), app.getTerm(),
            app.getRepaidType(), app.getStatus(),
            app.getApplyTime(), app.getReviewTime(),
            app.getRejectReason()
        );
    }

    @Override
    @Transactional
    public List<UserGetAppResponse> userGetAllApplications(Long userId){

        List<LoanApplication> applications = applicationMapper.selectByUserId(userId);
        
        return applications.stream().map(app -> {
            LoanProduct product = loanProductMapper.findById(app.getProductId());
            String productName = product.getProductName();
            return new UserGetAppResponse(
                app.getId(),
                productName,
                app.getLoanAmount(),
                app.getInterestRate(),
                app.getLoanPeriod(),
                app.getTerm(),
                app.getRepaidType(),
                app.getStatus(),
                app.getApplyTime(),
                app.getReviewTime(),
                app.getStatus().isRejected() ? app.getRejectReason() : null
            );
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<AdminGetAppResponse> adminGetAllApplications(Long userId){

        List<LoanApplication> applications = applicationMapper.selectByUserId(userId);

        return applications.stream().map(app -> {
            LoanProduct product = loanProductMapper.findById(app.getProductId());
            User user = userService.getUserById(app.getUserId());
            return new AdminGetAppResponse(
                app.getId(), app.getUserId(), app.getProductId(),
                user.getUserName(), user.getPhone(),product.getProductName(),
                app.getLoanAmount(), app.getInterestRate(),
                app.getLoanPeriod(), app.getTerm(),
                app.getRepaidType(), app.getStatus(),
                app.getApplyTime(), app.getReviewTime(),
                app.getStatus().isRejected() ?  app.getRejectReason() : null
            );
        }).collect(Collectors.toList());
    }
}