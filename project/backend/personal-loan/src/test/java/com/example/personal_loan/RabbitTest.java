package com.example.personal_loan;

import com.example.personal_loan.dto.ApplicationRequest;
import com.example.personal_loan.entity.OutboxMessage;
import com.example.personal_loan.mapper.OutboxMapper;
import com.example.personal_loan.mq.OutboxMessagePoller;
import com.example.personal_loan.service.ApplicationService;
import com.example.personal_loan.utils.RabbitUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
public class RabbitTest {

    @Autowired
    private OutboxMapper outboxMapper;

    @Autowired
    private RabbitUtil rabbitUtil;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private OutboxMessagePoller outboxMessagePoller;

    @Test
    void testSend() {

    }

    /*
     * 测试Service层数据插入到LoanProduct表， OutBoxMessage表
     */
    @Test
    @Rollback(true)
    void testInsert() {
        Long userId = 2L;
        ApplicationRequest applicationRequest = new ApplicationRequest();
        applicationRequest.setProductId(2L);
        applicationRequest.setOptionId(2L);
        applicationService.addApplication(userId, applicationRequest);
        // 执行插入

        List<OutboxMessage> messages = outboxMapper.selectPendingMessages(10);

        // 断言
        assertThat(messages.get(0).getStatus()).isEqualTo("PENDING");
        log.info("测试通过");
    }

    /*
     * 测试Poller层从OutBoxMessage表中获取数据RabbitMQ发送消息
     */
    @Test
    @Rollback(true)
    void testDelete() {
        List<OutboxMessage> messages = outboxMapper.selectPendingMessages(10);
        log.info(messages.toString());
        for (OutboxMessage message : messages) {
            outboxMessagePoller.sendAndMarkMessage(message);
            log.info("Success Sent");
        }
    }
}
