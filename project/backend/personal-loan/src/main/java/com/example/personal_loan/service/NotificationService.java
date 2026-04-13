package com.example.personal_loan.service;

import java.util.List;

import com.example.personal_loan.entity.Notification;

public interface NotificationService {
    List<Notification> getMyNotifications(Long userId, int limit);

    void markAsRead(Long notificationId);
    
    List<Notification> getAdminNotifications(int limit);
    
}

