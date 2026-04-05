package com.example.personal_loan.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
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
    public static final String LOAN_APPLICATION_RETRY_QUEUE = "loan.application.retry.queue";

    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String NOTIFICATION_RETRY_QUEUE = "notification.retry.queue";

    public static final String ORDER_REPAID_QUEUE = "order.repaid.queue";
    public static final String PAYMENT_REQUESTED_QUEUE = "payment.requested.queue";
    public static final String PAYMENT_SUCCESS_QUEUE = "payment.success.queue";
    public static final String PAYMENT_SUCCESS_RETRY_QUEUE = "payment.success.retry.queue";

    // ========== Routing Key ==========
    public static final String LOAN_APPLICATION_ROUTING_KEY = "loan.application.submitted";
    public static final String LOAN_APPLICATION_RETRY_ROUTING_KEY = "loan.application.retry";

    public static final String NOTIFICATION_ROUTING_KEY = "notification.loan-application";
    public static final String NOTIFICATION_RETRY_ROUTING_KEY = "notification.retry";
    
    public static final String ORDER_REPAID_ROUTING_KEY = "order.repaid";
    public static final String PAYMENT_REQUESTED_ROUTING_KEY = "payment.requested";
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.success";
    public static final String PAYMENT_SUCCESS_RETRY_ROUTING_KEY = "payment.success.retry";


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
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", LOAN_APPLICATION_RETRY_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_RETRY_ROUTING_KEY)
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

    @Bean
    public Binding bindNotificationQueue() {
        return BindingBuilder.bind(notificationQueue())
                .to(loanExchange())
                .with(NOTIFICATION_ROUTING_KEY);
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

    @Bean
    public Queue loanApplicationRetryQueue() {
        return QueueBuilder.durable(LOAN_APPLICATION_RETRY_QUEUE)
                .withArgument("x-message-ttl", 10000)
                .withArgument("x-dead-letter-exchange", LOAN_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", LOAN_APPLICATION_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue notificationRetryQueue() {
        return QueueBuilder.durable(NOTIFICATION_RETRY_QUEUE)
                .withArgument("x-message-ttl", 10000)
                .withArgument("x-dead-letter-exchange", LOAN_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding retryBinding() {
        return BindingBuilder.bind(loanApplicationRetryQueue())
                .to(dlx())
                .with(LOAN_APPLICATION_RETRY_ROUTING_KEY);
    }

    @Bean
    public Binding notificationRetryBinding() {
        return BindingBuilder.bind(notificationRetryQueue())
                .to(dlx())
                .with(NOTIFICATION_RETRY_ROUTING_KEY);
    }

    @Bean
    public Queue orderRepaidQueue() {
        return QueueBuilder.durable(ORDER_REPAID_QUEUE).build();
    }

    @Bean
    public Binding bindOrderRepaidQueue() {
        return BindingBuilder.bind(orderRepaidQueue())
                .to(loanExchange())
                .with(ORDER_REPAID_ROUTING_KEY);
    }

    @Bean
    public Queue paymentRequestedQueue() {
        return QueueBuilder.durable(PAYMENT_REQUESTED_QUEUE).build();
    }

    @Bean
    public Binding bindPaymentRequestedQueue() {
        return BindingBuilder.bind(paymentRequestedQueue())
                .to(loanExchange())
                .with(PAYMENT_REQUESTED_ROUTING_KEY);
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return QueueBuilder.durable(PAYMENT_SUCCESS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", PAYMENT_SUCCESS_RETRY_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding bindPaymentSuccessQueue() {
        return BindingBuilder.bind(paymentSuccessQueue())
                .to(loanExchange())
                .with(PAYMENT_SUCCESS_ROUTING_KEY);
    }

    @Bean
    public Queue paymentSuccessRetryQueue() {
        return QueueBuilder.durable(PAYMENT_SUCCESS_RETRY_QUEUE)
                .withArgument("x-message-ttl", 10000)
                .withArgument("x-dead-letter-exchange", LOAN_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PAYMENT_SUCCESS_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding paymentSuccessRetryBinding() {
        return BindingBuilder.bind(paymentSuccessRetryQueue())
                .to(dlx())
                .with(PAYMENT_SUCCESS_RETRY_ROUTING_KEY);
    }

}
