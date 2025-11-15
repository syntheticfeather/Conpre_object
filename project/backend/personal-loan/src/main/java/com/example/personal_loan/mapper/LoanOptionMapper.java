package com.example.personal_loan.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.entity.LoanOption;

// LoanOptionMapper.java
@Mapper
public interface LoanOptionMapper {
    
    @Insert("INSERT INTO loan_option(product_id, loan_period, loan_amount, repaid_type, interest_rate) " +
            "VALUES(#{productId}, #{loanPeriod}, #{loanAmount}, #{repaidType}, #{interestRate})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LoanOption option);

    @Insert({
        "<script>",
        "INSERT INTO loan_option (product_id, loan_period, loan_amount, interest_rate, repaid_type)",
        "VALUES ",
        "<foreach collection='list' item='opt' separator=','>",
        "(#{opt.productId}, #{opt.loanPeriod}, #{opt.loanAmount}, #{opt.interestRate}, #{opt.repaidType})",
        "</foreach>",
        "</script>"
    })
    int insertBatch(List<LoanOption> list);
    
    @Update("UPDATE loan_option SET loan_period=#{loanPeriod}, loan_amount=#{loanAmount}, " +
            "repaid_type=#{repaidType}, interest_rate=#{interestRate} WHERE id=#{id}")
    int update(LoanOption option);
    
    @Delete("DELETE FROM loan_option WHERE id=#{id}")
    int deleteById(Long id);
    
    @Delete("DELETE FROM loan_option WHERE product_id=#{productId}")
    int deleteByProductId(Long productId);
    
    @Select("SELECT * FROM loan_option WHERE id=#{id}")
    LoanOption selectById(Long id);
    
    @Select("SELECT * FROM loan_option WHERE product_id=#{productId}")
    List<LoanOption> selectByProductId(Long productId);
    
}