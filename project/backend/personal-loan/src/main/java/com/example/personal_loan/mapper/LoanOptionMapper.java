package com.example.personal_loan.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.example.personal_loan.entity.LoanOption;

// LoanOptionMapper.java
@Mapper
public interface LoanOptionMapper {
    
    @Insert(
        "INSERT INTO loan_options (" +
        "  product_id, " +
        "  loan_period, " +
        "  repaid_type, " +
        "  interest_rate, " +
        "  create_time, " +
        "  update_time" +
        ") VALUES (" +
        "  #{productId}, " +
        "  #{loanPeriod}, " +
        "  #{repaidType}, " +
        "  #{interestRate}, " +
        "  #{createTime}, " +
        "  #{updateTime}" +
        ")"
    )
    @Options(useGeneratedKeys = true, keyProperty = "optionId")
    int insert(LoanOption option);

    int insertBatch(List<LoanOption> list);

    // 更新
    void update(LoanOption option);
    
    //删除单个选项
    @Delete(
        "DELETE FROM loan_options WHERE id=#{id}"
    )
    int deleteById(Long id);

    int batchDeleteByIds(List<Long> optionIds);
    
    // 删除某产品的所有选项
    @Delete(
        "DELETE FROM loan_options WHERE product_id=#{productId}"
    )
    int deleteByProductId(Long productId);

    int batchDeleteByProductIds(List<Long> productIds);
    
    @Select(
        "SELECT " +
        "  id as optionId, " +
        "  product_id as productId, " +
        "  loan_period as loanPeriod, " +
        "  interest_rate as interestRate, " +
        "  repaid_type as repaidType, " +
        "  create_time as createTime, " +
        "  update_time as updateTime " +
        "FROM loan_options WHERE id=#{id}"
    )
    LoanOption selectById(Long id);
    
    @Select(
        "SELECT " +
        "  id as optionId, " +
        "  product_id as productId, " +
        "  loan_period as loanPeriod, " +
        "  interest_rate as interestRate, " +
        "  repaid_type as repaidType, " +
        "  create_time as createTime, " +
        "  update_time as updateTime " +
        "FROM loan_options WHERE product_id=#{productId}"
    )
    List<LoanOption> selectByProductId(Long productId);
    
}