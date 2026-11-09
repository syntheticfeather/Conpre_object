package com.example.personal_loan.utils;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.personal_loan.config.RabbitMQConfig;

@Component
public class RabbitUtil {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    /*
     * 向指定队列发送消息
     * @param queueName 队列名称
     * @param routingKey 路由键
     * @param message 消息内容
     */

    public void send(String queueName,String routingKey ,Object message) {
        rabbitTemplate.convertAndSend(queueName, routingKey,message);
    }

     // 向loan.exchange发送消息
    public void sendToApp(String routingKey ,Object message) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.LOAN_EXCHANGE,routingKey,message);
    }

    public void sendToApp(String routingKey ,Object message, CorrelationData correlationData) {
        // L7.2：risk.* 主题走独立的 risk.exchange，其余沿用 loan.exchange
        String exchange = routingKey != null && routingKey.startsWith("risk.")
                ? RabbitMQConfig.RISK_EXCHANGE
                : RabbitMQConfig.LOAN_EXCHANGE;
        rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
    }

    // 向 risk.exchange 发送消息（L7.2）
    public void sendToRisk(String routingKey, Object message) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.RISK_EXCHANGE, routingKey, message);
    }

    public void sendToDLX(String routingKey, Object message) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.DLX, routingKey, message);
    }
    /*
     * 获取消息的delivery tag
     * @param message 消息对象
     * @return delivery tag
     */

    public Long getTag(Message message) {
        return message.getMessageProperties().getDeliveryTag();
    }
}

