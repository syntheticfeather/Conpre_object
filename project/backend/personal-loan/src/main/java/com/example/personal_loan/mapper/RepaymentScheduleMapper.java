package com.example.personal_loan.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.entity.RepaymentSchedule;

@Mapper
public interface RepaymentScheduleMapper {
    
    @Insert("INSERT INTO repayment_schedule (" +
            "order_id, term, principal, interest, total_amount, " +
            "status, remaining_principal, remaining_interest, " +
            "due_date, actual_pay_date" +
            ") VALUES (" +
            "#{orderId}, #{term}, #{principal}, #{interest}, #{totalAmount}, " +
            "#{status}, #{remainingPrincipal}, #{remainingInterest}, " +
            "#{dueDate}, #{actualPayDate}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RepaymentSchedule schedule);
    
    @Update("UPDATE repayment_schedule SET " +
            "term = #{term}, " +
            "principal = #{principal}, " +
            "interest = #{interest}, " +
            "total_amount = #{totalAmount}, " +
            "status = #{status}, " +
            "remaining_principal = #{remainingPrincipal}, " +
            "remaining_interest = #{remainingInterest}, " +
            "due_date = #{dueDate}, " +
            "actual_pay_date = #{actualPayDate} " +
            "WHERE id = #{id}")
    int updateById(RepaymentSchedule schedule);
    
    @Select("SELECT * FROM repayment_schedule WHERE order_id = #{orderId} ORDER BY term")
    List<RepaymentSchedule> selectByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT * FROM repayment_schedule WHERE order_id = #{orderId} AND term = #{term}")
    RepaymentSchedule selectByOrderIdAndTerm(@Param("orderId") Long orderId, @Param("term") Integer term);
}
