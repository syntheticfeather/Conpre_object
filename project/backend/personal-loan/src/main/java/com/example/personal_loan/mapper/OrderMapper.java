package com.example.personal_loan.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.entity.Order;

@Mapper
public interface OrderMapper {
    // 根据ID查询订单
    @Select("SELECT id, user_id, product_id, status, repaid_amount, loan_amount, " +
        "interest_rate, repaid_type, loan_period, term, current_term, contract, " +
        "overdue_days, start_time " +
        "FROM orders WHERE id = #{id}")
    Order selectById(Long id);
    
    // 根据订单ID和用户ID查询订单
    @Select("SELECT id, user_id, product_id, status, repaid_amount, loan_amount, " +
        "interest_rate, repaid_type, loan_period, term, current_term, contract, " +
        "overdue_days, start_time " +
        "FROM orders WHERE id = #{orderId} AND user_id = #{userId}")
    Order selectByIdAndUserId(@Param("orderId") Long orderId, @Param("userId") Long userId);
    
    // 查询用户的所有订单
    @Select("SELECT id, user_id, product_id, status, repaid_amount, loan_amount, " +
        "interest_rate, repaid_type, loan_period, term, current_term, contract, " +
        "overdue_days, start_time " +
        "FROM orders WHERE user_id = #{userId}")
    List<Order> selectAllByUserId(Long userId);
    
    // 插入新订单
    @Insert("INSERT INTO orders (user_id, product_id, status, repaid_amount, loan_amount, " +
            "interest_rate, repaid_type, loan_period, contract, term, current_term, overdue_days , start_time) " +
            "VALUES(#{userId}, #{productId}, #{status}, #{repaidAmount}, #{loanAmount}, " +
            "#{interestRate}, #{repaidType}, #{loanPeriod}, #{contract}, #{term}, #{currentTerm}, #{overdueDays}, " +
            "#{startTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);
    
    // 更新订单信息
    @Update("UPDATE orders SET status = #{status}, repaid_amount = #{repaidAmount}, loan_amount = #{loanAmount}, " +
            "current_term = #{currentTerm}, overdue_days=#{overdueDays}, " +
            " WHERE id = #{id}")
    int update(Order order);
    
    // 还款更新
    @Update("UPDATE orders SET repaid_amount=#{repaidAmount}, outstanding_amount=#{outstandingAmount}, " +
            "current_term=current_term + 1 WHERE id=#{id}")
    int updateForRepayment(@Param("id") Long orderId, 
                          @Param("repaidAmount") BigDecimal repaidAmount, 
                          @Param("outstandingAmount") BigDecimal outstandingAmount,
                          Integer currentTerm);
    
    // 延期更新（加1期）
    @Update("UPDATE orders SET loan_period = loan_period + 1 " +
        "WHERE id = #{orderId}")
    int updateForPostpone(Long orderId);
    
    // 更新订单状态
    @Update("UPDATE orders SET status = #{status} WHERE id = #{orderId}")
    int updateStatus(@Param("orderId") Long orderId, @Param("status") String status);
}