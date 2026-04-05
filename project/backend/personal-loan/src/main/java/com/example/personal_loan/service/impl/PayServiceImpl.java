package com.example.personal_loan.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.personal_loan.dto.PaymentRequestedEvent;
import com.example.personal_loan.dto.PaymentSuccessEvent;
import com.example.personal_loan.service.PayService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PayServiceImpl implements PayService {

    @Override
    public PaymentSuccessEvent pay(PaymentRequestedEvent request) {
        if (request == null) {
            throw new IllegalArgumentException("request is null");
        }
        log.info("mock pay start: orderId={}, amount={}, requestTime={}",
                request.getOrderId(), request.getAmount(), request.getRequestTime());

        // 模拟支付成功的交易号
        String txId = "mock_tx_" + request.getOrderId() + "_" + System.currentTimeMillis();
        LocalDateTime paidAt = LocalDateTime.now();

        log.info("mock pay success: orderId={}, txId={}, paidAt={}", request.getOrderId(), txId, paidAt);
        return new PaymentSuccessEvent(request.getOrderId(), request.getAmount(), txId, paidAt);
    }
}
