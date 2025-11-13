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

import com.example.personal_loan.dto.ProductDto;
import com.example.personal_loan.service.LoanProductService;
import com.example.personal_loan.vo.LoanProductVO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/loan-products")
public class LoanProductController {
    
    @Autowired
    private LoanProductService service;
    
    // ========== 用户端 ==========
    @GetMapping("/user")
    public ResponseEntity<List<LoanProductVO>> listForUser() {
        return ResponseEntity.ok(service.getAllLoanProducts());
    }

    @GetMapping("/user/{productId}")
    public ResponseEntity<LoanProductVO> getProductForUser(@PathVariable Long productId) {
        return ResponseEntity.ok(service.getLoanProductById(productId));
    }

    // ========== 管理端 ==========
    @GetMapping("/admin")
    public ResponseEntity<List<ProductDto>> listForAdmin() {
        return ResponseEntity.ok(service.adminGetAllProducts());
    }

    @GetMapping("/admin/{productId}")
    public ResponseEntity<ProductDto> getProductForAdmin(@PathVariable Long productId) {
        return ResponseEntity.ok(service.adminGetProductById(productId));
    }

    @PostMapping("/admin")
    public ResponseEntity<ProductDto> create(@RequestBody ProductDto dto) {
        return ResponseEntity.ok(service.createLoanProduct(dto));
    }

    @PatchMapping("/admin/products/{id}")
    public ResponseEntity<ProductDto> update(@PathVariable Long id, @RequestBody @Valid ProductDto dto) {
        ProductDto updated = service.updateLoanProduct(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/admin/products/{productId}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long productId) {
        service.deleteLoanProduct(productId);
        return ResponseEntity.ok("Loan product delete success");
    }

    @DeleteMapping("/admin/options/{optionId}")
    public ResponseEntity<String> deleteOption(@PathVariable Long optionId) {
        service.deleteLoanOption(optionId);
        return ResponseEntity.ok("The option of product delete success");
    }
}
