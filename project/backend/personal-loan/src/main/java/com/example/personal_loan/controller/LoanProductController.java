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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.personal_loan.dto.ApiResponse;
import com.example.personal_loan.dto.BatchCreateOptionRequest;
import com.example.personal_loan.dto.BatchDeleteRequest;
import com.example.personal_loan.dto.ProductDto;
import com.example.personal_loan.dto.UserGetProductResponse;
import com.example.personal_loan.service.LoanProductService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/loan-products")
@Slf4j
public class LoanProductController {
    
    @Autowired
    private LoanProductService loanProductService;
    
    // ========== 用户端 ==========
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<UserGetProductResponse>>> listForUser() {
        log.info("/loan-products/user api success alled"); 
        List<UserGetProductResponse> products = loanProductService.getAllLoanProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/user/search")
    public ResponseEntity<ApiResponse<List<UserGetProductResponse>>> searchProducts(
        @RequestParam String name) {
        log.info("/loan-products/user/search api success called with name: {}", name);
        List<UserGetProductResponse> results = loanProductService.searchProductsByName(name);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    // ========== 管理端 ==========
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<List<ProductDto>>> listForAdmin() {
        log.info("/loan-products/admin api success called to get all products");
        List<ProductDto> products = loanProductService.adminGetAllProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/admin/{productId}")
    public ResponseEntity<ApiResponse<ProductDto>> getProductForAdmin(@PathVariable Long productId) {
        log.info("/loan-products/admin/{} api success called", productId);
        ProductDto product = loanProductService.adminGetProductById(productId);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @PostMapping("/admin")
    public ResponseEntity<ApiResponse<ProductDto>> create(@RequestBody @Valid ProductDto dto) {
        log.info("/loan-products/admin api success called to create product");
        ProductDto created = loanProductService.createLoanProduct(dto);
        return ResponseEntity.ok(ApiResponse.success(created, "贷款产品创建成功"));
    }

    @PostMapping("/admin/options/batch-create")
    public ResponseEntity<ApiResponse<String>> batchCreateOptions(
            @RequestBody @Valid BatchCreateOptionRequest request) {
        log.info("/loan-products/admin/options/batch-create api success called to create options for productId: {}", request.getProductId());
        loanProductService.batchCreateLoanOptions(request.getProductId(), request.getOptions());
        return ResponseEntity.ok(ApiResponse.success("Batch create loan options success"));
    }

    @PatchMapping("/admin/products/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> update(@PathVariable Long id, @RequestBody ProductDto dto) {
        log.info("/loan-products/admin/products/{} api success called to update product", id);
        ProductDto updated = loanProductService.updateLoanProduct(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "贷款产品更新成功"));
    }

    @DeleteMapping("/admin/products/{productId}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(@PathVariable Long productId) {
        log.info("/loan-products/admin/products/{} api success called to delete product", productId);
        loanProductService.deleteLoanProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Loan product delete success"));
    }

    @DeleteMapping("/admin/options/{optionId}")
    public ResponseEntity<ApiResponse<String>> deleteOption(@PathVariable Long optionId) {
        log.info("/loan-products/admin/options/{} api success called to delete option", optionId);
        loanProductService.deleteLoanOption(optionId);
        return ResponseEntity.ok(ApiResponse.success("The option of product delete success"));
    }

    @PostMapping("/admin/options/batch-delete")
    public ResponseEntity<ApiResponse<String>> batchDeleteOptions(@RequestBody BatchDeleteRequest request) {
        log.info("/loan-products/admin/options/batch-delete api success called to delete options");
        loanProductService.batchDeleteLoanOptionsByIds(request.getIds());
        return ResponseEntity.ok(ApiResponse.success("Batch delete specific loan options success"));
    }

    @PostMapping("/admin/products/batch-delete")
    public ResponseEntity<ApiResponse<String>> batchDeleteProducts(@RequestBody BatchDeleteRequest request) {
        log.info("/loan-products/admin/products/batch-delete api success called to delete products");
        loanProductService.batchDeleteLoanProducts(request.getIds());
        return ResponseEntity.ok(ApiResponse.success("Batch delete loan products success"));
    }
}
