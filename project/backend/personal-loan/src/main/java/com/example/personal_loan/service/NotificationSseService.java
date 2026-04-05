package com.example.personal_loan.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.personal_loan.entity.Notification;

public interface NotificationSseService {
    SseEmitter subscribe(Long userId);

    void publish(Long userId, Notification notification);
}

