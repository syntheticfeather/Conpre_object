package com.example.personal_loan.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
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

import com.example.personal_loan.dto.AdminGetProDetailResponse;
import com.example.personal_loan.dto.ApiResult;
import com.example.personal_loan.dto.BatchCreateOptionRequest;
import com.example.personal_loan.dto.BatchDeleteRequest;
import com.example.personal_loan.dto.ListProductResponse;
import com.example.personal_loan.dto.ProductDto;
import com.example.personal_loan.dto.SearchByDateRequest;
import com.example.personal_loan.dto.UserGetProductResponse;
import com.example.personal_loan.service.LoanProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/loan-products")
@Slf4j
@Tag(name = "贷款产品管理", description = "贷款产品相关接口")
public class LoanProductController {
    
    @Autowired
    private LoanProductService loanProductService;
    
    // ========== 用户端 ==========
    @GetMapping(value = "/user", produces = "application/json")
    @Operation(summary = "获取所有贷款产品", description = "用户获取所有可用的贷款产品列表")
    public ResponseEntity<ApiResult<List<UserGetProductResponse>>> listForUser() {
        log.info("/loan-products/user api success alled"); 
        List<UserGetProductResponse> products = loanProductService.getAllLoanProducts();
        return ResponseEntity.ok(ApiResult.success(products));
    }

    @GetMapping(value = "/user/search", produces = "application/json")
    @Operation(summary = "搜索贷款产品", description = "用户根据产品名称搜索贷款产品")
    public ResponseEntity<ApiResult<List<UserGetProductResponse>>> searchProducts(
            @Parameter(description = "产品名称") @RequestParam String name) {
        log.info("/loan-products/user/search api success called with name: {}", name);
        List<UserGetProductResponse> results = loanProductService.searchProductsByName(name);
        return ResponseEntity.ok(ApiResult.success(results));
    }

    // ========== 管理端 ==========

    // 新增产品
    @PostMapping(value = "/admin", produces = "application/json")
    @Operation(summary = "创建贷款产品", description = "管理员创建新的贷款产品")
    public ResponseEntity<ApiResult<ProductDto>> create(@Parameter(description = "产品信息") @RequestBody @Valid ProductDto dto) {
        log.info("/loan-products/admin api success called to create product");
        ProductDto created = loanProductService.createLoanProduct(dto);
        return ResponseEntity.ok(ApiResult.success(created, "贷款产品创建成功"));
    }

    // 上架
    @PostMapping(value = "/admin/{productId}/active", produces = "application/json")
    @Operation(summary = "上架贷款产品", description = "管理员上架指定的贷款产品")
    public ResponseEntity<ApiResult<Void>> activeProduct(@Parameter(description = "产品ID") @PathVariable Long productId) {
        log.info("/loan-products/admin/{}/active success called for admin to active product with id: {}", productId, productId);
        loanProductService.activeProduct(productId);
        return ResponseEntity.ok(ApiResult.success(null, "产品上架成功"));
    }

    // 下架
    @PostMapping(value = "/admin/{productId}/deactive", produces = "application/json")
    @Operation(summary = "下架贷款产品", description = "管理员下架指定的贷款产品")
    public ResponseEntity<ApiResult<Void>> deactiveProduct(@Parameter(description = "产品ID") @PathVariable Long productId) {
        log.info("/loan-products/admin/{}/deactive success called for admin to deactive product with id: {}", productId, productId);
        loanProductService.deactiveProduct(productId);
        return ResponseEntity.ok(ApiResult.success(null, "产品下架成功"));
    }

    // 获取产品列表
    @GetMapping(value = "/admin", produces = "application/json")
    @Operation(summary = "获取产品列表", description = "管理员获取所有贷款产品列表")
    public ResponseEntity<ApiResult<List<ListProductResponse>>> listForAdmin() {
        log.info("/loan-products/admin api success called to get all products");
        List<ListProductResponse> products = loanProductService.adminGetAllProducts();
        return ResponseEntity.ok(ApiResult.success(products));
    }

    // 获取单个产品详情
    @GetMapping(value = "/admin/{productId}", produces = "application/json")
    @Operation(summary = "获取产品详情", description = "管理员获取指定贷款产品的详细信息")
    public ResponseEntity<ApiResult<AdminGetProDetailResponse>> getProductForAdmin(@Parameter(description = "产品ID") @PathVariable Long productId) {
        log.info("/loan-products/admin/{} api success called", productId);
        AdminGetProDetailResponse product = loanProductService.adminGetProductById(productId);
        return ResponseEntity.ok(ApiResult.success(product));
    }

    
    // 批量创建选项
    @PostMapping(value = "/admin/options/batch-create", produces = "application/json")
    @Operation(summary = "批量创建产品选项", description = "管理员为指定产品批量创建贷款选项")
    public ResponseEntity<ApiResult<String>> batchCreateOptions(
            @Parameter(description = "批量创建选项请求") @RequestBody @Valid BatchCreateOptionRequest request) {
        log.info("/loan-products/admin/options/batch-create api success called to create options for productId: {}", request.getProductId());
        loanProductService.batchCreateLoanOptions(request.getProductId(), request.getOptions());
        return ResponseEntity.ok(ApiResult.success("Batch create loan options success"));
    }


    // 更新产品信息
    @PatchMapping(value = "/admin/products/{id}", produces = "application/json")
    @Operation(summary = "更新产品信息", description = "管理员更新指定贷款产品的信息")
    public ResponseEntity<ApiResult<ProductDto>> update(@Parameter(description = "产品ID") @PathVariable Long id, @Parameter(description = "产品信息") @RequestBody ProductDto dto) {
        log.info("/loan-products/admin/products/{} api success called to update product", id);
        ProductDto updated = loanProductService.updateLoanProduct(id, dto);
        return ResponseEntity.ok(ApiResult.success(updated, "贷款产品更新成功"));
    }

    // 删除单个产品
    @DeleteMapping(value = "/admin/products/{productId}", produces = "application/json")
    @Operation(summary = "删除贷款产品", description = "管理员删除指定的贷款产品")
    public ResponseEntity<ApiResult<String>> deleteProduct(@Parameter(description = "产品ID") @PathVariable Long productId) {
        log.info("/loan-products/admin/products/{} api success called to delete product", productId);
        loanProductService.deleteLoanProduct(productId);
        return ResponseEntity.ok(ApiResult.success("Loan product delete success"));
    }

    // 删除单个选项
    @DeleteMapping(value = "/admin/options/{optionId}", produces = "application/json")
    @Operation(summary = "删除产品选项", description = "管理员删除指定的产品选项")
    public ResponseEntity<ApiResult<String>> deleteOption(@Parameter(description = "选项ID") @PathVariable Long optionId) {
        log.info("/loan-products/admin/options/{} api success called to delete option", optionId);
        loanProductService.deleteLoanOption(optionId);
        return ResponseEntity.ok(ApiResult.success("The option of product delete success"));
    }

    // 批量删除选项
    @DeleteMapping(value = "/admin/options/batch-delete", produces = "application/json")
    @Operation(summary = "批量删除产品选项", description = "管理员批量删除指定的产品选项")
    public ResponseEntity<ApiResult<String>> batchDeleteOptions(@Parameter(description = "批量删除请求") @RequestBody BatchDeleteRequest request) {
        log.info("/loan-products/admin/options/batch-delete api success called to delete options");
        loanProductService.batchDeleteLoanOptionsByIds(request.getIds());
        return ResponseEntity.ok(ApiResult.success("Batch delete specific loan options success"));
    }

    // 批量删除产品
    @PostMapping(value = "/admin/products/batch-delete", produces = "application/json")
    @Operation(summary = "批量删除贷款产品", description = "管理员批量删除指定的贷款产品")
    public ResponseEntity<ApiResult<String>> batchDeleteProducts(@Parameter(description = "批量删除请求") @RequestBody BatchDeleteRequest request) {
        log.info("/loan-products/admin/products/batch-delete api success called to delete products");
        loanProductService.batchDeleteLoanProducts(request.getIds());
        return ResponseEntity.ok(ApiResult.success("Batch delete loan products success"));
    }

    // 根据更新时间和创建时间范围搜索产品
    @GetMapping(produces = "application/json")
    @Operation(summary = "按日期范围搜索产品", description = "根据创建时间和更新时间范围搜索贷款产品")
    public ResponseEntity<ApiResult<List<ListProductResponse>>> listProducts(
            @Parameter(description = "创建开始日期") @RequestParam(required = false) 
            @DateTimeFormat(iso = ISO.DATE) LocalDate createStartDate,

            @Parameter(description = "创建结束日期") @RequestParam(required = false) 
            @DateTimeFormat(iso = ISO.DATE) LocalDate createEndDate,

            @Parameter(description = "更新开始日期") @RequestParam(required = false) 
            @DateTimeFormat(iso = ISO.DATE) LocalDate updateStartDate,

            @Parameter(description = "更新结束日期") @RequestParam(required = false) 
            @DateTimeFormat(iso = ISO.DATE) LocalDate updateEndDate) {

        log.info("/api/loan-products success called to search products by date range");
        SearchByDateRequest request = new SearchByDateRequest();
        request.setCreateStartDate(createStartDate);
        request.setCreateEndDate(createEndDate);
        request.setUpdateStartDate(updateStartDate);
        request.setUpdateEndDate(updateEndDate);

        List<ListProductResponse> result = loanProductService.searchByDate(request);

        return ResponseEntity.ok(ApiResult.success(result));
    }
}
