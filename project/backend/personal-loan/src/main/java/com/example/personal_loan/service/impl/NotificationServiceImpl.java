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

    @Override
    @Transactional
    public List<Notification> getMyNotifications(Long userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return notificationMapper.selectLatestByUserId(userId, safeLimit);
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        int rows = notificationMapper.markAsRead(LocalDateTime.now(), userId, notificationId);
        if (rows == 0) {
            throw new BusinessException(404, "通知不存在");
        }
    }
}

