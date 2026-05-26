package com.example.personal_loan.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.dto.ApplicationDetailResponse;
import com.example.personal_loan.dto.ApprovalTypeStatistics;
import com.example.personal_loan.dto.MonthlyStatistics;
import com.example.personal_loan.dto.PendingApprovalResponse;
import com.example.personal_loan.dto.UserAppListResponse;
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
    List<UserAppListResponse> selectByUserIdWithProduct(@Param("userId") Long userId);

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
        "WHERE la.status = 'AI通过' OR la.status = '人工通过' OR la.status = '人工拒绝' " +
        "ORDER BY la.apply_time DESC"
    )
    List<PendingApprovalResponse> listCompletedApprovals();

    // 检查产品是否被贷款申请引用
    @Select("SELECT COUNT(*) FROM loan_applications WHERE product_id = #{productId}")
    int countByProductId(@Param("productId") Long productId);

    // 批量检查产品是否被贷款申请引用
    @Select("<script>" +
        "SELECT COUNT(*) FROM loan_applications WHERE product_id IN " +
        "<foreach collection='productIds' item='productId' open='(' separator=',' close=')'>" +
        "#{productId}" +
        "</foreach>" +
        "</script>")
    int countByProductIds(@Param("productIds") List<Long> productIds);

    // 统计每月申请量
    @Select("SELECT DATE_FORMAT(apply_time, '%Y-%m') as month, COUNT(*) as count FROM loan_applications GROUP BY DATE_FORMAT(apply_time, '%Y-%m') ORDER BY month DESC")
    List<MonthlyStatistics> countMonthlyApplications();

    // 统计每月通过量（包括AI通过和人工通过）
    @Select("SELECT DATE_FORMAT(apply_time, '%Y-%m') as month, COUNT(*) as count FROM loan_applications WHERE status = 'AI通过' OR status = '人工通过' GROUP BY DATE_FORMAT(apply_time, '%Y-%m') ORDER BY month DESC")
    List<MonthlyStatistics> countMonthlyApprovals();

    // 统计每月AI通过和人工通过的数量
    @Select("SELECT DATE_FORMAT(apply_time, '%Y-%m') as month, status, COUNT(*) as count FROM loan_applications WHERE status = 'AI通过' OR status = '人工通过' GROUP BY DATE_FORMAT(apply_time, '%Y-%m'), status ORDER BY month DESC, status")
    List<ApprovalTypeStatistics> countApprovalTypesByMonth();
}