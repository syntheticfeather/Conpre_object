package com.example.personal_loan.service;

import com.example.personal_loan.dto.PaymentRequestedEvent;
import com.example.personal_loan.dto.PaymentSuccessEvent;

public interface PayService {
    PaymentSuccessEvent pay(PaymentRequestedEvent request);
}
