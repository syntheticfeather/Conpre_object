package com.example.personal_loan.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    // ========== Topic Exchange ==========
    public static final String LOAN_EXCHANGE = "loan.exchange";

    // ========== Queue ==========
    public static final String LOAN_APPLICATION_QUEUE = "loan.application.queue";

    // ========== Routing Key ==========
    public static final String LOAN_APPLICATION_ROUTING_KEY = "loan.application.submitted";


    public static final String DLQ = "order.dlq"; // 死信队列
    public static final String DLX = "dlx"; // 死信交换机

    /**
     * 声明 Topic Exchange（持久化）
     */
    @Bean
    public TopicExchange loanExchange() {
        return new TopicExchange(LOAN_EXCHANGE, true, false); // durable=true, autoDelete=false
    }

    /**
     * 声明队列（持久化）
     */
    @Bean
    public Queue loanApplicationQueue() {
        return QueueBuilder.durable(LOAN_APPLICATION_QUEUE)
                .build();
    }

    /**
     * 将队列绑定到 Exchange + Routing Key
     */
    @Bean
    public Binding bindLoanApplicationQueue() {
        return BindingBuilder.bind(loanApplicationQueue())
                .to(loanExchange())
                .with(LOAN_APPLICATION_ROUTING_KEY);
    }

    /**
     * 使用 Jackson 自动序列化/反序列化 JSON
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /*
     * 定义死信队列dlq()和死信交换机dlx()
     * 并进行DLQ路由键的绑定
     */
    @Bean
    public Queue dlq() {
        // 定义一个持久化的死信队列
        return QueueBuilder
                .durable(DLQ)
                .build();
    }

    @Bean
    public DirectExchange dlx() {
        // 定义死信交换机
        return new DirectExchange(DLX);
    }

    @Bean
    public Binding dlqBinding() {
        // 凡是死信交换机dlx()中的路由键为DLQ的消息，都将被转移到死信队列dlq()中
        return BindingBuilder.bind(dlq()) // 死信列队
                .to(dlx()) // 死信交换机
                .with(DLQ); // 路由键
    }

}
