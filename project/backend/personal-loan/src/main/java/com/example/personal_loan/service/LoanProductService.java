package com.example.personal_loan.service;

import java.util.List;

import com.example.personal_loan.dto.AdminGetProDetailResponse;
import com.example.personal_loan.dto.ListProductResponse;
import com.example.personal_loan.dto.ProductDto;
import com.example.personal_loan.dto.SearchByDateRequest;
import com.example.personal_loan.dto.UserGetProductResponse;
import com.example.personal_loan.entity.LoanOption;

public interface LoanProductService {

    /*
     * 管理员使用
     */

    // 创建单个产品，多个选项（至少一种）
    ProductDto createLoanProduct(ProductDto loanProductDto);

    // 上架产品
    void activeProduct(Long productId);

    // 下架产品
    void deactiveProduct(Long productId);

    // 给指定产品批量增加选项
    void batchCreateLoanOptions(Long productId, List<LoanOption> options);

    // 删除产品的某个选项（贷款方案详情）
    int deleteLoanOption(Long optionId);

    // 删除产品
    int deleteLoanProduct(Long productId);

    // 批量删除产品（连带其所有选项）
    void batchDeleteLoanProducts(List<Long> productIds);

    // 批量删除选项（不删产品本身）
    void batchDeleteLoanOptionsByIds(List<Long> optionIds);

    // 修改
    ProductDto updateLoanProduct(Long id,ProductDto loanProductDto);

    // 获取单个产品详情
    AdminGetProDetailResponse adminGetProductById(Long id);

    // 获取产品列表
    List<ListProductResponse> adminGetAllProducts ();

    // 搜索
    List<ListProductResponse> searchByDate(SearchByDateRequest request);

    /*
     * 用户使用
     */

    // 根据产品名称搜索产品
    List<UserGetProductResponse> searchProductsByName(String name);
    // 查询所有产品
    List<UserGetProductResponse> getAllLoanProducts();
}
