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

import com.example.personal_loan.controller.dto.BatchCreateOptionRequest;
import com.example.personal_loan.controller.dto.BatchCreateProductRequest;
import com.example.personal_loan.controller.dto.BatchDeleteRequest;
import com.example.personal_loan.dto.ProductDto;
import com.example.personal_loan.service.LoanProductService;
import com.example.personal_loan.vo.LoanProductVO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/loan-products")
public class LoanProductController {
    
    @Autowired
    private LoanProductService loanProductService;
    
    // ========== 用户端 ==========
    @GetMapping("/user")
    public ResponseEntity<List<LoanProductVO>> listForUser() {
        return ResponseEntity.ok(loanProductService.getAllLoanProducts());
    }

    @GetMapping("/user/{productId}")
    public ResponseEntity<LoanProductVO> getProductForUser(@PathVariable Long productId) {
        return ResponseEntity.ok(loanProductService.getLoanProductById(productId));
    }

    // ========== 管理端 ==========
    @GetMapping("/admin")
    public ResponseEntity<List<ProductDto>> listForAdmin() {
        return ResponseEntity.ok(loanProductService.adminGetAllProducts());
    }

    @GetMapping("/admin/{productId}")
    public ResponseEntity<ProductDto> getProductForAdmin(@PathVariable Long productId) {
        return ResponseEntity.ok(loanProductService.adminGetProductById(productId));
    }

    @PostMapping("/admin")
    public ResponseEntity<ProductDto> create(@RequestBody ProductDto dto) {
        return ResponseEntity.ok(loanProductService.createLoanProduct(dto));
    }

    @PostMapping("/admin/options/batch-create")
    public ResponseEntity<String> batchCreateOptions(
            @RequestBody @Valid BatchCreateOptionRequest request) {
        loanProductService.batchCreateLoanOptions(request.getProductId(), request.getOptions());
        return ResponseEntity.ok("Batch create loan options success");
    }

    @PatchMapping("/admin/products/{id}")
    public ResponseEntity<ProductDto> update(@PathVariable Long id, @RequestBody @Valid ProductDto dto) {
        ProductDto updated = loanProductService.updateLoanProduct(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/admin/products/{productId}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long productId) {
        loanProductService.deleteLoanProduct(productId);
        return ResponseEntity.ok("Loan product delete success");
    }

    @DeleteMapping("/admin/options/{optionId}")
    public ResponseEntity<String> deleteOption(@PathVariable Long optionId) {
        loanProductService.deleteLoanOption(optionId);
        return ResponseEntity.ok("The option of product delete success");
    }

    @PostMapping("/admin/options/batch-delete")
    public ResponseEntity<String> batchDeleteOptions(@RequestBody BatchDeleteRequest request) {
        loanProductService.batchDeleteLoanOptionsByIds(request.getIds());
        return ResponseEntity.ok("Batch delete specific loan options success");
    }

    @PostMapping("/admin/products/batch-delete")
    public ResponseEntity<String> batchDeleteProducts(@RequestBody BatchDeleteRequest request) {
        loanProductService.batchDeleteLoanProducts(request.getIds());
        return ResponseEntity.ok("Batch delete loan products success");
    }
}
