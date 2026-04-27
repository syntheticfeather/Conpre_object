package com.example.personal_loan.mq;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.Notification;
import com.example.personal_loan.entity.OutboxMessage;
import com.example.personal_loan.entity.User;
import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.enums.BusinessType;
import com.example.personal_loan.factory.OutboxMessageFactory;
import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.mapper.OutboxMapper;
import com.example.personal_loan.mapper.UserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class NotificationOutboxPublisher {

    @Autowired
    private OutboxMapper outboxMapper;

    @Autowired
    private OutboxMessageFactory outboxMessageFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private UserMapper userMapper;
 
    /**
     * 发布普通通知
     * @param userId 用户ID
     * @param businessId 业务ID
     * @param businessType 业务类型
     */
    public void enqueueNotification(Long userId, Long businessId, String businessType) {
        // 构建通知实体 Notification
        Notification notification = new Notification(
            null,
            userId,
            businessId,
            businessType,
            buildTitle(businessType),
            buildContent(businessId, businessType),
            false,
            LocalDateTime.now(),
            null
        );
        
        // 写 outbox 消息
        OutboxMessage outbox = outboxMessageFactory.create(BusinessType.NOTIFICATION, notification, businessId);
        outboxMapper.insert(outbox);
    }

    /**
     * 发布管理员通知
     * @param businessId 业务ID
     * @param businessType 业务类型
     */
    public void enqueueAdminNotification(Long businessId, String businessType) {
        // 构建通知实体 Notification
        List<User> adminUsers = userMapper.findByRole(1);
        Notification notification = new Notification(
            null,
            adminUsers.get(0).getId(),
            businessId,
            businessType,
            buildTitle(businessType),
            buildContent(businessId, businessType),
            false,
            LocalDateTime.now(),
            null
        );

        // 写 outbox 消息
        OutboxMessage outbox = outboxMessageFactory.create(BusinessType.NOTIFICATION, notification, businessId);
        outboxMapper.insert(outbox);
    }

    /**
     * 构建通知标题
     * @param businessType 业务类型
     * @return 通知标题
     */
    private String buildTitle(String businessType) {

        if ("LOAN_APPLICATION_STATUS".equals(businessType)) {
            return "贷款申请状态更新";
        } else if ("REPAYMENT".equals(businessType)) {
            return "还款状态更新";
        } else if ("LOAN_APPLICATION_APPROVE".equals(businessType)) {
            return "新增贷款申请审核";
        }
        return "通知";
    }

    /**
     * 构建通知内容
     * @param businessId 业务ID
     * @param businessType 业务类型
     * @return 通知内容
     */
    private String buildContent(Long businessId, String businessType) {

        if ("LOAN_APPLICATION_STATUS".equals(businessType)) {
            // 处理贷款申请状态更新通知
            LoanApplication application = applicationMapper.selectById(businessId);
            ApplicationStatus status = application.getStatus();

            if (status == ApplicationStatus.审核中 || status == ApplicationStatus.AI拒绝) {
                return "您的贷款申请(" + businessId + ")已提交，正在审核中";
            }else if (status == ApplicationStatus.人工拒绝) {
                return "您的贷款申请(" + businessId + ")未通过审核，拒绝原因：" + application.getRejectReason();
            }else if (status == ApplicationStatus.已取消) {
                return "您的贷款申请(" + businessId + ")已成功取消";
            }else if (status == ApplicationStatus.AI通过 || status == ApplicationStatus.人工通过) {
                return "您的贷款申请(" + businessId + ")已通过审核";
            }
        } else if ("REPAYMENT".equals(businessType)) {
            // 处理还款状态更新通知
            return "您的还款(" + businessId + ")已处理";
        } else if ("LOAN_APPLICATION_APPROVE".equals(businessType)) {
            // 处理新增贷款申请审核通知
            return "贷款申请(" + businessId + ") 被AI拒绝，需要人工审核";
        }
        return "您有一条新通知";
    }

}

