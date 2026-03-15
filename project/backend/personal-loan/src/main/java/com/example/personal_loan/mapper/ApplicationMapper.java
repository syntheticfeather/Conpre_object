package com.example.personal_loan.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.dto.ApplicationDetailResponse;
import com.example.personal_loan.dto.PendingApprovalResponse;
import com.example.personal_loan.entity.LoanApplication;

@Mapper
public interface ApplicationMapper {
    @Insert(
        "INSERT INTO loan_applications (" +
        "  user_id, " +
        "  product_id, " +
        "  status, " +
        "  loan_amount, " +
        "  interest_rate, " +
        "  loan_period, " +
        "  term, " +
        "  repaid_type, " +
        "  reject_reason, " +
        "  apply_time, " +
        "  review_time " +
        ") VALUES (" +
        "  #{userId}, " +
        "  #{productId}, " +
        "  #{status}, " +
        "  #{loanAmount}, " +
        "  #{interestRate}, " +
        "  #{loanPeriod}, " +
        "  #{term}, " +
        "  #{repaidType}, " +
        "  #{rejectReason}, " +
        "  #{applyTime}, " +
        "  #{reviewTime} " +
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

    // 根据用户ID查询所有申请（带产品信息）
    List<com.example.personal_loan.dto.UserAppListResponse> selectByUserIdWithProduct(@Param("userId") Long userId);

    @Update(
        "UPDATE loan_applications " +
        "SET " +
        "  user_id = #{userId}, " +
        "  product_id = #{productId}, " +
        "  status = #{status}, " +
        "  loan_amount = #{loanAmount}, " +
        "  interest_rate = #{interestRate}, " +
        "  loan_period = #{loanPeriod}, " +
        "  term = #{term}, " +
        "  repaid_type = #{repaidType}, " +
        "  reject_reason = #{rejectReason}, " +
        "  review_time = #{reviewTime} " +
        "WHERE id = #{id}"
    )
    int update(LoanApplication application);

    @Select(
        "SELECT " +
        "  la.id AS applicationId, " +
        "  u.user_name AS userName, " +
        "  lp.product_name AS productName, " +
        "  la.loan_amount AS loanAmount, " +
        "  la.loan_period AS loanPeriod, " +
        "  la.term AS term, " +
        "  la.status AS status, " +
        "  la.apply_time AS applyTime " +
        "FROM loan_applications la " +
        "JOIN users u ON la.user_id = u.id " +
        "JOIN loan_products lp ON la.product_id = lp.id " +
        "WHERE la.status = 'AI拒绝' " +
        "ORDER BY la.apply_time DESC"
    )
    List<PendingApprovalResponse> listPendingApprovals();

    ApplicationDetailResponse getApplicationDetail(@Param("loanApplicationId") Long loanApplicationId);

    @Select(
        "SELECT " +
        "  la.id AS applicationId, " +
        "  u.user_name AS userName, " +
        "  lp.product_name AS productName, " +
        "  la.loan_amount AS loanAmount, " +
        "  la.loan_period AS loanPeriod, " +
        "  la.term AS term, " +
        "  la.status AS status, " +
        "  la.apply_time AS applyTime " +
        "FROM loan_applications la " +
        "JOIN users u ON la.user_id = u.id " +
        "JOIN loan_products lp ON la.product_id = lp.id " +
        "WHERE la.status = '已通过' OR la.status = '人工拒绝' " +
        "ORDER BY la.apply_time DESC"
    )
    List<PendingApprovalResponse> listCompletedApprovals();
}