package com.example.personal_loan.service.impl;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.personal_loan.entity.Notification;
import com.example.personal_loan.service.NotificationSseService;

@Service
public class NotificationSseServiceImpl implements NotificationSseService {

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(Long userId) {
        // 创建一个 SSE 发射器，参数 0L 表示连接永不超时
        SseEmitter emitter = new SseEmitter(0L);
        // 2. 将新的emitter添加到对应用户的列表中
        // computeIfAbsent确保如果userId不存在，则先创建一个列表
        emittersByUserId.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // 3. 注册回调函数，在连接完成、超时或出错时，自动移除emitter，防止内存泄漏
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError((ex) -> removeEmitter(userId, emitter));

        // 4. 连接建立后，立即发送一个确认消息给客户端
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            // 如果发送失败（例如客户端瞬间断开），则立即清理emitter
            removeEmitter(userId, emitter);
        }

        return emitter; // 将这个emitter返回给Spring MVC，由它写入HTTP响应
    }

    @Override
    public void publish(Long userId, Notification notification) {
        // 1. 获取该用户的所有活跃连接
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return; // 如果用户不在线（没有活跃连接），则直接返回
        }

        // 2. 遍历所有连接，并尝试发送消息
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
            } catch (IOException e) {
                // 3. 如果发送失败，说明连接已失效，需要将其移除
                removeEmitter(userId, emitter);
            }
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null) {
            return;
        }
        // 从用户的列表中移除指定的emitter
        emitters.remove(emitter);
        // 如果该用户的所有连接都已断开，则从Map中移除整个用户条目，彻底释放内存
        if (emitters.isEmpty()) {
            emittersByUserId.remove(userId);
        }
    }
}

