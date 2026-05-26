package com.example.personal_loan.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.dto.UserOrderListResponse;
import com.example.personal_loan.entity.Order;

@Mapper
public interface OrderMapper {
    // 根据ID查询订单
    @Select(
        "SELECT " +
        "  id, " +
        "  user_id, " +
        "  product_id, " +
        "  status, " +
        "  repaid_amount, " +
        "  loan_amount, " +
        "  interest_rate, " +
        "  repaid_type, " +
        "  loan_period, " +
        "  term, " +
        "  current_term, " +
        "  contract, " +
        "  overdue_days, " +
        "  start_time " +
        "FROM orders WHERE id = #{id}"
    )
    Order selectById(Long id);
    
    // 查询用户的所有订单
    @Select(
        "SELECT " +
        "  id, " +
        "  user_id, " +
        "  product_id, " +
        "  status, " +
        "  repaid_amount, " +
        "  loan_amount, " +
        "  interest_rate, " +
        "  repaid_type, " +
        "  loan_period, " +
        "  term, " +
        "  current_term, " +
        "  contract, " +
        "  overdue_days, " +
        "  start_time " +
        "FROM orders WHERE user_id = #{userId}"
    )
    List<Order> selectAllByUserId(Long userId);
    
    // 插入新订单
    @Insert(
        "INSERT INTO orders (" +
        "  user_id, " +
        "  product_id, " +
        "  status, " +
        "  repaid_amount, " +
        "  loan_amount, " +
        "  interest_rate, " +
        "  repaid_type, " +
        "  loan_period, " +
        "  contract, " +
        "  term, " +
        "  current_term, " +
        "  overdue_days, " +
        "  start_time " +
        ") VALUES (" +
        "  #{userId}, " +
        "  #{productId}, " +
        "  #{status}, " +
        "  #{repaidAmount}, " +
        "  #{loanAmount}, " +
        "  #{interestRate}, " +
        "  #{repaidType}, " +
        "  #{loanPeriod}, " +
        "  #{contract}, " +
        "  #{term}, " +
        "  #{currentTerm}, " +
        "  #{overdueDays}, " +
        "  #{startTime} " +
        ")"
    )
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);
    
    // 更新订单信息
    @Update(
        "UPDATE orders " +
        "SET " +
        "  status = #{status}, " +
        "  repaid_amount = #{repaidAmount}, " +
        "  loan_amount = #{loanAmount}, " +
        "  current_term = #{currentTerm}, " +
        "  overdue_days = #{overdueDays} " +
        "WHERE id = #{id}"
    )
    int update(Order order);

    // 获取订单列表
    List<UserOrderListResponse> selectOrderListByUserId(@Param("userId") Long userId);

    // 检查产品是否被订单引用
    @Select("SELECT COUNT(*) FROM orders WHERE product_id = #{productId}")
    int countByProductId(@Param("productId") Long productId);

    // 批量检查产品是否被订单引用
    @Select("<script>" +
        "SELECT COUNT(*) FROM orders WHERE product_id IN " +
        "<foreach collection='productIds' item='productId' open='(' separator=',' close=')'>" +
        "#{productId}" +
        "</foreach>" +
        "</script>")
    int countByProductIds(@Param("productIds") List<Long> productIds);
    
    // 查询未完成的订单
    @Select("SELECT * FROM orders WHERE status != '已完成'")
    List<Order> selectUncompletedOrders();
}