package com.example.personal_loan.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.personal_loan.entity.Notification;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.mapper.NotificationMapper;
import com.example.personal_loan.service.impl.NotificationServiceImpl;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void getMyNotifications_limitUpperBound_shouldClampTo100() {
        List<Notification> expected = Collections.emptyList();
        when(notificationMapper.selectLatestByUserId(3L, 100)).thenReturn(expected);

        List<Notification> actual = notificationService.getMyNotifications(3L, 1000);

        assertEquals(expected, actual);
        verify(notificationMapper).selectLatestByUserId(3L, 100);
    }

    @Test
    void getMyNotifications_limitLowerBound_shouldClampTo1() {
        List<Notification> expected = Collections.emptyList();
        when(notificationMapper.selectLatestByUserId(3L, 1)).thenReturn(expected);

        List<Notification> actual = notificationService.getMyNotifications(3L, 0);

        assertEquals(expected, actual);
        verify(notificationMapper).selectLatestByUserId(3L, 1);
    }

    @Test
    void getMyNotifications_normalLimit_shouldUseGivenLimit() {
        List<Notification> expected = Collections.emptyList();
        when(notificationMapper.selectLatestByUserId(3L, 50)).thenReturn(expected);

        List<Notification> actual = notificationService.getMyNotifications(3L, 50);

        assertEquals(expected, actual);
        verify(notificationMapper).selectLatestByUserId(3L, 50);
    }

    @Test
    void markAsRead_success_shouldNotThrow() {
        when(notificationMapper.markAsRead(org.mockito.ArgumentMatchers.any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq(9L))).thenReturn(1);

        assertDoesNotThrow(() -> notificationService.markAsRead(9L));
        verify(notificationMapper).markAsRead(org.mockito.ArgumentMatchers.any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq(9L));
    }

    @Test
    void markAsRead_notFound_shouldThrowBusinessException() {
        when(notificationMapper.markAsRead(org.mockito.ArgumentMatchers.any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq(9L))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> notificationService.markAsRead(9L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void getAdminNotifications_limitUpperBound_shouldClampTo100() {
        List<Notification> expected = Collections.emptyList();
        when(notificationMapper.selectByBusinessType("LOAN_APPLICATION_APPROVE", 100)).thenReturn(expected);

        List<Notification> actual = notificationService.getAdminNotifications(1000);

        assertEquals(expected, actual);
        verify(notificationMapper).selectByBusinessType("LOAN_APPLICATION_APPROVE", 100);
    }

    @Test
    void getAdminNotifications_limitLowerBound_shouldClampTo1() {
        List<Notification> expected = Collections.emptyList();
        when(notificationMapper.selectByBusinessType("LOAN_APPLICATION_APPROVE", 1)).thenReturn(expected);

        List<Notification> actual = notificationService.getAdminNotifications(0);

        assertEquals(expected, actual);
        verify(notificationMapper).selectByBusinessType("LOAN_APPLICATION_APPROVE", 1);
    }

    @Test
    void getAdminNotifications_normalLimit_shouldUseGivenLimit() {
        List<Notification> expected = Collections.emptyList();
        when(notificationMapper.selectByBusinessType("LOAN_APPLICATION_APPROVE", 50)).thenReturn(expected);

        List<Notification> actual = notificationService.getAdminNotifications(50);

        assertEquals(expected, actual);
        verify(notificationMapper).selectByBusinessType("LOAN_APPLICATION_APPROVE", 50);
    }
}

