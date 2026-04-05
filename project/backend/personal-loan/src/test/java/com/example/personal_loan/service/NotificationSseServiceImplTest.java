package com.example.personal_loan.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.personal_loan.entity.Notification;
import com.example.personal_loan.service.impl.NotificationSseServiceImpl;

class NotificationSseServiceImplTest {

    @Test
    void subscribe_shouldRegisterEmitter() {
        NotificationSseServiceImpl service = new NotificationSseServiceImpl();

        SseEmitter emitter = service.subscribe(2L);

        assertNotNull(emitter);
        Map<Long, CopyOnWriteArrayList<SseEmitter>> map = getEmittersMap(service);
        assertTrue(map.containsKey(2L));
        assertEquals(1, map.get(2L).size());
        assertTrue(map.get(2L).contains(emitter));
    }

    @Test
    void subscribe_completeShouldRemoveEmitter() {
        NotificationSseServiceImpl service = new NotificationSseServiceImpl();
        SseEmitter emitter = service.subscribe(2L);

        Map<Long, CopyOnWriteArrayList<SseEmitter>> map = getEmittersMap(service);
        assertTrue(map.containsKey(2L));

        emitter.complete();

        Map<Long, CopyOnWriteArrayList<SseEmitter>> mapAfter = getEmittersMap(service);
        assertFalse(mapAfter.containsKey(2L));
    }

    @Test
    void publish_withoutSubscriber_shouldNotThrow() {
        NotificationSseServiceImpl service = new NotificationSseServiceImpl();
        Notification notification = new Notification(1L, 2L, 11L, "type", "t", "c", false, LocalDateTime.now(), null);

        assertDoesNotThrow(() -> service.publish(2L, notification));
    }

    @Test
    void publish_withSubscriber_shouldNotThrow() {
        NotificationSseServiceImpl service = new NotificationSseServiceImpl();
        service.subscribe(2L);
        Notification notification = new Notification(1L, 2L, 11L, "type", "t", "c", false, LocalDateTime.now(), null);

        assertDoesNotThrow(() -> service.publish(2L, notification));
    }

    @SuppressWarnings("unchecked")
    private static Map<Long, CopyOnWriteArrayList<SseEmitter>> getEmittersMap(NotificationSseServiceImpl service) {
        return (Map<Long, CopyOnWriteArrayList<SseEmitter>>) ReflectionTestUtils.getField(service, "emittersByUserId");
    }
}

