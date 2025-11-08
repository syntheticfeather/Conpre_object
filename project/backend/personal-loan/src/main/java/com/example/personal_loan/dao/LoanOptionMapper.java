package com.example.personal_loan.dao;

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
    
    @Update("UPDATE loan_option SET loan_period=#{loanPeriod}, loan_amount=#{loanAmount}, " +
            "repaid_type=#{repaidType}, interest_rate=#{interestRate} WHERE id=#{id}")
    int update(LoanOption option);
    
    @Delete("DELETE FROM loan_option WHERE id=#{id}")
    int deleteById(Integer id);
    
    @Delete("DELETE FROM loan_option WHERE product_id=#{productId}")
    int deleteByProductId(Integer productId);
    
    @Select("SELECT * FROM loan_option WHERE id=#{id}")
    LoanOption selectById(Integer id);
    
    @Select("SELECT * FROM loan_option WHERE product_id=#{productId}")
    List<LoanOption> selectByProductId(Integer productId);
    
}