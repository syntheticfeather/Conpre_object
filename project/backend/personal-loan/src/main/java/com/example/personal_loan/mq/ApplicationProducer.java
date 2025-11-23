package com.example.personal_loan.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ApplicationProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendApplication() {
        
    }
}
