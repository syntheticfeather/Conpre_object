package com.example.personal_loan.vo;

import java.util.List;

import com.example.personal_loan.entity.LoanOption;
import com.example.personal_loan.entity.LoanProduct;

public class LoanProductVO extends LoanProduct{
    private List<LoanOption> loanOptions;

    public List<LoanOption> getLoanOptions() { 
        return this.loanOptions; 
    }
    public void setLoanOptions(List<LoanOption> loanOptions) { 
        this.loanOptions = loanOptions; 
    }
}
