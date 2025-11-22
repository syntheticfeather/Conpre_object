package com.example.personal_loan.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.entity.LoanApplication;

@Mapper
public interface ApplicationMapper {
    @Insert(
        "INSERT INTO loan_applications ("+
        "user_id, product_id, status, loan_amount, interest_rate,"+
        "loan_period, term, repaid_type, reject_reason, apply_time, review_time"+
        ") VALUES ("+
        "#{userId}, #{productId}, #{status}, #{loanAmount}, #{interestRate},"+
        "#{loanPeriod}, #{term}, #{repaidType}, #{rejectReason}, #{applyTime}, #{reviewTime}"+
        ")"
    )
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(LoanApplication application);  // 添加贷款申请

    // 根据ID查询申请
    @Select("SELECT " +
            "id, " +
            "user_id as userId, " +
            "product_id as productId, " +
            "status, " +
            "loan_amount as loanAmount, " +
            "interest_rate as interestRate, " +
            "loan_period as loanPeriod, " +
            "term as term, " +
            "repaid_type as repaidType, " +
            "reject_reason as rejectReason, " +
            "apply_time as applyTime, " +
            "review_time as reviewTime " +
            "FROM loan_applications WHERE id = #{id}")
    LoanApplication selectById(@Param("id") Long id);
 
    // 根据用户ID查询所有申请
    @Select("SELECT " +
        "id, " +
        "user_id as userId, " +
        "product_id as productId, " +
        "status, " +
        "loan_amount as loanAmount, " +
        "interest_rate as interestRate, " +
        "loan_period as loanPeriod, " +
        "term as term, " +
        "repaid_type as repaidType, " +
        "reject_reason as rejectReason, " +
        "apply_time as applyTime, " +
        "review_time as reviewTime " +
        "FROM loan_applications WHERE user_id = #{userId} ORDER BY apply_time DESC")
    List<LoanApplication> selectByUserId(@Param("userId") Long userId);
 
    /**
     * 根据产品ID查询所有相关申请（管理员视角，二期可能用到）
     */
    @Select("SELECT * FROM loan_applications WHERE product_id = #{productId} ORDER BY apply_time DESC")
    List<LoanApplication> findByProductId(@Param("productId") Long productId);

    /**
     * （可选）根据状态查询申请（如 PENDDING, APPROVED）
     */
    @Select("SELECT * FROM loan_applications WHERE status = #{status} ORDER BY apply_time DESC")
    List<LoanApplication> findByStatus(@Param("status") String status);

    /**
     * 更新申请状态
     */
    @Update({
        "<script>",
        "UPDATE loan_applications",
        "SET status = #{status}",
        "<if test='reviewTime != null'>, review_time = #{reviewTime}</if>",
        "<if test='rejectReason != null'>, reject_reason = #{rejectReason}</if>",
        "WHERE id = #{id}",
        "</script>"
    })
    void updateStatus(LoanApplication application);
}