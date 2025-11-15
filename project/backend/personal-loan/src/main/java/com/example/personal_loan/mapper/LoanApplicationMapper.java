package com.example.personal_loan.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.personal_loan.entity.LoanApplication;

@Mapper
public interface LoanApplicationMapper {
    int insert(LoanApplication application);
    LoanApplication selectById(Long id);
    List<LoanApplication> selectByUserId(Long userId);
    void updateStatus(@Param("id") Integer id, @Param("status") String status, @Param("reviewTime") LocalDateTime reviewTime);
}