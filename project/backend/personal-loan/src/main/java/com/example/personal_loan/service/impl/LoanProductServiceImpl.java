package com.example.personal_loan.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.personal_loan.dao.LoanProductMapper;
import com.example.personal_loan.entity.LoanProduct;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.service.LoanProductService;

@Service
public class LoanProductServiceImpl implements LoanProductService{

    @Autowired
    private LoanProductMapper loanProductMapper;

    @Override
    public LoanProduct createLoanProduct(LoanProduct loanProduct){
        LocalDateTime now = LocalDateTime.now();
        loanProduct.setCreateTime(now);
        loanProduct.setUpdateTime(now);

        if (loanProduct.getMinTerm() > loanProduct.getMaxTerm()) {
            throw new BusinessException("最短期数不能大于最长期数");
        }

        if(loanProduct.getTermStep()<0){
            throw new BusinessException("期数步长不能为负数");
        }

        loanProductMapper.create(loanProduct);
        return loanProduct;
    }

    @Override
    public int deleteLoanProduct(Long id){
        return loanProductMapper.delete(id);
    }

    @Override
    public int updateLoanProduct(Long id,LoanProduct loanProduct){

        LoanProduct existing = loanProductMapper.findById(id);
        
        if (existing == null) {
            throw new BusinessException("贷款产品不存在");
        }
        if (loanProduct.getProductName() != null) {
            existing.setProductName(loanProduct.getProductName());
        }
        if (loanProduct.getMinTerm() != null) {
            existing.setMinTerm(loanProduct.getMinTerm());
        }
        if (loanProduct.getMaxTerm() != null) {
            existing.setMaxTerm(loanProduct.getMaxTerm());
        }
        if (loanProduct.getTermStep() != null) {
            existing.setTermStep(loanProduct.getTermStep());
        }
        if (loanProduct.getPromotionDetails() != null) {
            existing.setPromotionDetails(loanProduct.getPromotionDetails());
        }
        if (existing.getMinTerm() > existing.getMaxTerm()) {
            throw new BusinessException("最短期数不能大于最长期数");
        }
        if (existing.getTermStep() <= 0) {
            throw new BusinessException("期数步长必须大于0");
        }

        return loanProductMapper.update(existing);
    }

    @Override
    public LoanProduct getLoanProductById(Long id){
        return loanProductMapper.findById(id);
    }

    @Override
    public List<LoanProduct> getAllLoanProducts(){
        return loanProductMapper.findAll();
    }
}
