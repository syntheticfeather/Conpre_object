package com.example.personal_loan.service;

import java.util.List;

import com.example.personal_loan.entity.LoanProduct;

public interface LoanProductService {

    LoanProduct createLoanProduct(LoanProduct loanProduct);

    int deleteLoanProduct(Long id);

    int updateLoanProduct(Long id,LoanProduct loanProduct);

    LoanProduct getLoanProductById(Long id);

    List<LoanProduct> getAllLoanProducts();
}
