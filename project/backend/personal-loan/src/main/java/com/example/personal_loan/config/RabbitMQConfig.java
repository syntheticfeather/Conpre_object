package com.example.personal_loan.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 交换机名称
    public static final String REVIEW_EXCHANGE = "pend.exchange";

    // ai审核列队
    public static final String AI_REVIEW_QUEUE = "ai.pend.queue";

    // 贷款资金发送列队
    public static final String MANUAL_REVIEW_QUEUE = "loan.send.queue";

    // Routing Key
    public static final String ROUTING_KEY_AI = "ai";
    public static final String ROUTING_KEY_MANUAL = "loan";

    // ====== 声明交换机 ======
    @Bean
    public DirectExchange reviewExchange() {
        return new DirectExchange(REVIEW_EXCHANGE, true, false); // durable=true
    }

    // ====== 声明 AI 审核队列 ======
    @Bean
    public Queue aiReviewQueue() {
        return QueueBuilder.durable(AI_REVIEW_QUEUE).build();
    }

    // // ====== 声明贷款金额发送队列 ======
    @Bean
    public Queue manualReviewQueue() {
        return QueueBuilder.durable(MANUAL_REVIEW_QUEUE).build();
    }

    // ====== 绑定 AI 队列 ======
    @Bean
    public Binding bindAiQueue(Queue aiReviewQueue, DirectExchange reviewExchange) {
        return BindingBuilder.bind(aiReviewQueue)
                .to(reviewExchange)
                .with(ROUTING_KEY_AI);
    }

    // // ====== 绑定金额发送队列 ======
    @Bean
    public Binding bindManualQueue(Queue manualReviewQueue, DirectExchange reviewExchange) {
        return BindingBuilder.bind(manualReviewQueue)
                .to(reviewExchange)
                .with(ROUTING_KEY_MANUAL);
    }

    // ====== 配置 JSON 消息转换器======
    @Bean
    public org.springframework.amqp.support.converter.MessageConverter jsonMessageConverter() {
        return new org.springframework.amqp.support.converter.Jackson2JsonMessageConverter();
    }
}
