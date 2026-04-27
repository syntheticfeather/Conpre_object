package com.example.personal_loan.mq;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.dto.PaymentSuccessEvent;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.entity.RepaymentSchedule;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.mapper.PaymentRecordMapper;
import com.example.personal_loan.mapper.ProcessMessageMapper;
import com.example.personal_loan.mapper.RepaymentScheduleMapper;
import com.example.personal_loan.utils.RabbitUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSuccessConsumer {
    private final ProcessMessageMapper processedMessageMapper;
    private final ObjectMapper objectMapper;
    private final RabbitUtil rabbitUtil;
    private final OrderMapper orderMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final RepaymentScheduleMapper repaymentScheduleMapper;
    private final NotificationOutboxPublisher notificationOutboxPublisher;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_QUEUE)
    public void consume(Message message, Channel channel) throws IOException {
        String payload = new String(message.getBody());
        String messageId = message.getMessageProperties().getHeader("messageId");
        Long tag = rabbitUtil.getTag(message);
        if (messageId == null) {
            MessageProperties props = new MessageProperties();
            props.getHeaders().putAll(message.getMessageProperties().getHeaders());
            props.setHeader("messageId", "missing_" + System.currentTimeMillis());
            props.setContentType(message.getMessageProperties().getContentType());
            rabbitUtil.sendToDLX(RabbitMQConfig.DLQ, new Message(message.getBody(), props));
            channel.basicAck(tag, false);
            return;
        }
        if (processedMessageMapper.isProcessMessage(messageId)) {
            channel.basicAck(tag, false);
            return;
        }
        try {
            PaymentSuccessEvent event = objectMapper.readValue(payload, PaymentSuccessEvent.class);
            try {
                // 插入支付记录
                paymentRecordMapper.insert(event.getTxId(), event.getOrderId(), event.getAmount(), "SUCCESS", event.getPaidAt());
            } catch (DuplicateKeyException ex) {
                channel.basicAck(tag, false);
                return;
            }
            // 更新订单状态, 增加已还金额, 更新当前期数
            Order order = orderMapper.selectById(event.getOrderId());
            if (order == null) {
                channel.basicAck(tag, false);
                return;
            }
            
            // 检查是否是提前还款（如果还款金额大于当期应还金额，视为提前还款）
            List<RepaymentSchedule> schedules = repaymentScheduleMapper.selectByOrderId(event.getOrderId());
            BigDecimal currentTermAmount = BigDecimal.ZERO;
            for (RepaymentSchedule schedule : schedules) {
                if (schedule.getTerm().equals(order.getCurrentTerm() + 1)) {
                    currentTermAmount = schedule.getTotalAmount();
                    break;
                }
            }
            
            boolean isEarlyRepayment = event.getAmount().compareTo(currentTermAmount) > 0;
            
            if (isEarlyRepayment) {
                // 提前还款：更新所有剩余期数
                for (RepaymentSchedule schedule : schedules) {
                    if (schedule.getTerm() > order.getCurrentTerm()) {
                        schedule.setStatus("已还");
                        schedule.setActualPayDate(LocalDate.now());
                        repaymentScheduleMapper.updateById(schedule);
                    }
                }
                order.setCurrentTerm(order.getTerm());
                order.setRepaidAmount(order.getRepaidAmount().add(event.getAmount()));
                order.setStatus(OrderStatus.已完成);
            } else {
                // 正常还款：更新当期
                BigDecimal newRepaidAmount = order.getRepaidAmount().add(event.getAmount());
                Integer newCurrentTerm = order.getCurrentTerm() + 1;
                OrderStatus newStatus = (newCurrentTerm.equals(order.getTerm())) ? OrderStatus.已完成 : OrderStatus.正常;
                order.setRepaidAmount(newRepaidAmount);
                order.setCurrentTerm(newCurrentTerm);
                order.setStatus(newStatus);
                
                // 更新还款计划表状态
                RepaymentSchedule currentSchedule = repaymentScheduleMapper.selectByOrderIdAndTerm(event.getOrderId(), newCurrentTerm);
                if (currentSchedule != null) {
                    currentSchedule.setStatus("已还");
                    currentSchedule.setActualPayDate(LocalDate.now());
                    repaymentScheduleMapper.updateById(currentSchedule);
                }
            }
            
            orderMapper.update(order);

            // 记录已处理信息，插入幂等表
            processedMessageMapper.insertMessage(messageId, "PAYMENT_SUCCESS", order.getId());

            // 发notification通知
            notificationOutboxPublisher.enqueueNotification(order.getUserId(), order.getId(), "REPAYMENT");
            
            // 确认消费成功
            channel.basicAck(tag, false);
        } catch (Exception e) {
            Message dlqMsg = copyWithHeaders(message, messageId);
            rabbitUtil.sendToDLX(RabbitMQConfig.DLQ, dlqMsg);
            channel.basicAck(tag, false);
            log.error("payment success consume failed, moved to dlq: {}", messageId, e);
        }
    }

    private Message copyWithHeaders(Message origin, String messageId) {
        MessageProperties props = new MessageProperties();
        props.getHeaders().putAll(origin.getMessageProperties().getHeaders());
        props.setHeader("messageId", messageId);
        props.setContentType(origin.getMessageProperties().getContentType());
        return new Message(origin.getBody(), props);
    }
}
