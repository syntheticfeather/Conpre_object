package com.example.personal_loan.service.impl;

import java.time.LocalDateTime;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.personal_loan.mapper.OutboxMapper;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OutboxCleanService {
    @Autowired
    private OutboxMapper outboxMapper;

    // 启动服务时执行清理任务
    @PostConstruct
    @Transactional
    public void cleanExpiredMessages() {
        LocalDateTime thresholdTime = LocalDateTime.now().minusDays(1); // 清理1天前的成功投递的消息
        
        int batchSize = 1000;
        int deletedRows;

        // 2. 循环分批删除
        do {
            deletedRows = outboxMapper.deleteExpiredSuccessMessages(thresholdTime);
            if (deletedRows > 0) {
                log.info("Delete {} rows of expired success messages ", deletedRows);
            }
        } while (deletedRows >= batchSize);
    }
}
