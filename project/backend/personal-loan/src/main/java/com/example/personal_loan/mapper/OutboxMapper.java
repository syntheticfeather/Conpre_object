package com.example.personal_loan.mapper;

import com.example.personal_loan.entity.OutboxMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OutboxMapper {
    @Insert("""
            INSERT INTO outbox_message (message_id, business_type, business_id, topic, payload, status, created_at)\s
            VALUES (#{messageId}, #{businessType}, #{businessId}, #{topic}, #{payload}, #{status}, #{createdAt});
            """)
    public void insert(OutboxMessage outBoxMessage);

    // 最多选BATCH_SIZE个
    @Select("select * from outbox_message where status = 'PENDING' limit #{BATCH_SIZE}")
    public List<OutboxMessage> selectPendingMessages(int BATCH_SIZE);

    @Update("update outbox_message set status = 'SENT' ,sent_at = now() where message_id = #{messageId}")
    public void markAsSent(String messageId);

    @Update("update outbox_message set status = 'FAILED', sent_at = now() where message_id = #{messageId}")
    public void markAsFailed(String messageId);
}

//public class OutboxMessage {
//
//    private Long id;
//    private String messageId;
//    private String businessType;
//    private Long businessId;
//    private String topic;
//    private String payload;
//    private String status;
//    private LocalDateTime createdAt;
//    private LocalDateTime sentAt;
//}

//CREATE TABLE outbox_messages (
//        id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
//        message_id VARCHAR(64) NOT NULL UNIQUE COMMENT '全局唯一ID，如 UUID 或 loan_app_123_1712345678901',
//business_type VARCHAR(50) NOT NULL COMMENT '业务类型，如 LOAN_APPLICATION',
//business_id BIGINT NOT NULL COMMENT '关联的业务主键，如 loan_application.id',
//topic VARCHAR(255) NOT NULL COMMENT 'RabbitMQ routing key，如 loan.application.submitted',
//payload JSON NOT NULL COMMENT '消息体，通常是 LoanApplication 的 JSON 表示',
//status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING(待发送), SENT(已发送), FAILED(发送失败)',
//created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
//sent_at DATETIME NULL COMMENT '实际发送时间',
//INDEX idx_status_created (status, created_at),
//INDEX idx_business (business_type, business_id),
//INDEX idx_message_id (message_id)
//) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地消息表（Outbox Pattern）';

