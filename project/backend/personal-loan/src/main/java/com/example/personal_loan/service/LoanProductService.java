package com.example.personal_loan.service;

import java.util.List;

import com.example.personal_loan.dto.ProductDto;
import com.example.personal_loan.vo.LoanProductVO;

public interface LoanProductService {

    /*
     * 管理员使用
     */

    ProductDto createLoanProduct(ProductDto loanProductDto);

    // 删除产品的某个选项（贷款方案详情）
    int deleteLoanOption(Long optionId);

    // 删除产品
    int deleteLoanProduct(Long productId);

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
