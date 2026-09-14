package com.example.personal_loan.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.mapping.StatementType;

import com.example.personal_loan.dto.OrderStatisticsResponse;
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

    /**
     * 统计用户所有订单的待还总额、待还本金、待还利息（状态非"已还"的还款计划）
     */
    @Select(
        "SELECT " +
        "  COALESCE(SUM(rs.principal + rs.interest), 0) AS totalOutstandingAmount, " +
        "  COALESCE(SUM(rs.principal), 0) AS totalOutstandingPrincipal, " +
        "  COALESCE(SUM(rs.interest), 0) AS totalOutstandingInterest " +
        "FROM repayment_schedule rs " +
        "JOIN orders o ON rs.order_id = o.id " +
        "WHERE o.user_id = #{userId} AND rs.status != '已还'"
    )
    OrderStatisticsResponse sumOutstandingByUserId(@Param("userId") Long userId);

    @Options(statementType = StatementType.CALLABLE)
    @Select("CALL sp_generate_repayment_schedule(#{orderId, mode=IN}, #{success, mode=OUT, jdbcType=INTEGER})")
    void callGenerateRepaymentSchedule(Map<String, Object> params);

    @Options(statementType = StatementType.CALLABLE)
    @Select("CALL sp_update_due_date_after_postpone(#{orderId, mode=IN}, #{term, mode=IN}, #{success, mode=OUT, jdbcType=INTEGER})")
    void callUpdateDueDateAfterPostpone(Map<String, Object> params);
}
