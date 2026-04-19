package com.example.personal_loan.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.entity.Notification;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.mapper.NotificationMapper;
import com.example.personal_loan.service.NotificationService;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    // 获取用户最新通知
    @Override
    @Transactional
    public List<Notification> getMyNotifications(Long userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return notificationMapper.selectLatestByUserId(userId, safeLimit);
    }

    // 标记通知为已读
    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        int rows = notificationMapper.markAsRead(LocalDateTime.now(), notificationId);
        if (rows == 0) {
            throw new BusinessException(404, "通知不存在");
        }
    }

    // 获取管理员最新通知
    @Override
    @Transactional
    public List<Notification> getAdminNotifications(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return notificationMapper.selectByBusinessType("LOAN_APPLICATION_APPROVE", safeLimit);
    }

    // 删除通知
    @Override
    @Transactional
    public void deleteNotification(Long notificationId) {
        int rows = notificationMapper.deleteById(notificationId);
        if (rows == 0) {
            throw new BusinessException(404, "通知不存在");
        }
    }

    // 批量删除通知
    @Override
    @Transactional
    public void batchDeleteNotifications(List<Long> notificationIds) {
        int rows = notificationMapper.batchDelete(notificationIds);
        if (rows == 0) {
            throw new BusinessException(404, "通知不存在");
        }
    }
}

