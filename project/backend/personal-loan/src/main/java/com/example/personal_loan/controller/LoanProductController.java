package com.example.personal_loan.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.personal_loan.entity.LoanProduct;
import com.example.personal_loan.service.LoanProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/loan-products")
public class LoanProductController {
    
    @Autowired
    private LoanProductService loanProductService;
    
    @GetMapping
    public ResponseEntity<List<LoanProduct>> getAllLoanProducts() {
        return ResponseEntity.ok(loanProductService.getAllLoanProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanProduct> getLoanProductById(@PathVariable Long id){
        return ResponseEntity.ok(loanProductService.getLoanProductById(id));
    }

    @PostMapping
    public ResponseEntity<LoanProduct> createLoanProduct(@RequestBody @Valid LoanProduct loanProduct){
        return ResponseEntity.ok(loanProductService.createLoanProduct(loanProduct));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<LoanProduct> updateLoanProduct(@PathVariable Long id,@RequestBody LoanProduct loanProduct){
        loanProductService.updateLoanProduct(id,loanProduct);
        return ResponseEntity.ok(loanProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoanProduct(@PathVariable Long id){
        loanProductService.deleteLoanProduct(id);
        return ResponseEntity.noContent().build();
    }
}
