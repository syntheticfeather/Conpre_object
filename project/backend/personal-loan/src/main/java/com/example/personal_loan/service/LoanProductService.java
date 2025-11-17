package com.example.personal_loan.service;

import java.util.List;

import com.example.personal_loan.dto.ProductDto;
import com.example.personal_loan.entity.LoanOption;
import com.example.personal_loan.vo.LoanProductVO;

public interface LoanProductService {

    /*
     * 管理员使用
     */

    // 创建单个产品，多个选项（可选）
    ProductDto createLoanProduct(ProductDto loanProductDto);

    // 给指定产品批量增加选项
    void batchCreateLoanOptions(Long productId, List<LoanOption> options);

    // 批量增加产品
    // void batchCreateLoanProducts(List<ProductDto> dtos);

    // 删除产品的某个选项（贷款方案详情）
    int deleteLoanOption(Long optionId);

    // 删除产品
    int deleteLoanProduct(Long productId);

    // 批量删除产品（连带其所有选项）
    void batchDeleteLoanProducts(List<Long> productIds);

    // 批量删除选项（不删产品本身）
    void batchDeleteLoanOptionsByIds(List<Long> optionIds);

    ProductDto updateLoanProduct(Long id,ProductDto loanProductDto);

    // 根据productId查询产品及其选项
    ProductDto adminGetProductById(Long id);

    List<ProductDto> adminGetAllProducts ();



    /*
     * 用户使用
     */

    LoanProductVO getLoanProductById(Long id);
    List<LoanProductVO> getAllLoanProducts();
}
