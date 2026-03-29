package com.example.personal_loan.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentRecordMapper {
    @Insert("""
            INSERT INTO payment_record (tx_id, order_id, amount, status, paid_at)
            VALUES (#{txId}, #{orderId}, #{amount}, #{status}, #{paidAt})
            """)
    int insert(@Param("txId") String txId, @Param("orderId") Long orderId, @Param("amount") java.math.BigDecimal amount, @Param("status") String status, @Param("paidAt") java.time.LocalDateTime paidAt);
}
