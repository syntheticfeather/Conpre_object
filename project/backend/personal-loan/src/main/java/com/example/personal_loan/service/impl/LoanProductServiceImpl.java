package com.example.personal_loan.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.dto.ListProductResponse;
import com.example.personal_loan.dto.LoanOptionResponse;
import com.example.personal_loan.dto.ProductDto;
import com.example.personal_loan.dto.UserGetProductResponse;
import com.example.personal_loan.entity.LoanOption;
import com.example.personal_loan.entity.LoanProduct;
import com.example.personal_loan.enums.ProductStatus;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.mapper.LoanOptionMapper;
import com.example.personal_loan.mapper.LoanProductMapper;
import com.example.personal_loan.service.LoanProductService;

@Service
public class LoanProductServiceImpl implements LoanProductService{

    @Autowired
    private LoanProductMapper loanProductMapper;

    @Autowired
    private LoanOptionMapper loanOptionMapper;

    /*
     * 管理员使用
     */

    // 新增产品

    /*
     * 创建产品
     */
    @Override
    @Transactional
    public ProductDto createLoanProduct(ProductDto dto) {

        LoanProduct product = new LoanProduct();

        dto.setStatus(ProductStatus.INACTIVE); // 默认下架
        dto.setCreateTime(LocalDateTime.now());
        dto.setUpdateTime(LocalDateTime.now());
        BeanUtils.copyProperties(dto, product);

        loanProductMapper.create(product); // MyBatis 会回填 id
        dto.setId(product.getId());

        if (dto.getMinTerm() != null && dto.getMaxTerm() != null && dto.getMinTerm() > dto.getMaxTerm()) {
            throw new BusinessException(400, "最短期数不能大于最长期数");
        }

        // 批量插入选项
        if (dto.getOptions() != null && !dto.getOptions().isEmpty()) {
            for (LoanOption opt : dto.getOptions()) {
                opt.setProductId(product.getId());
                loanOptionMapper.insert(opt); 
                opt.setCreateTime(LocalDateTime.now());
                opt.setUpdateTime(LocalDateTime.now());
            }
        }else{
            throw new BusinessException(400, "贷款选项不能为空");
        }

        return dto;
    }

    // 上架产品
    @Override
    @Transactional
    public void activeProduct(Long productId){
        LoanProduct product = loanProductMapper.findById(productId);
        if(product.getStatus().equals(ProductStatus.INACTIVE)){
            product.setStatus(ProductStatus.ACTIVE);
            loanProductMapper.update(product);
        }else{
            throw new BusinessException(404,"产品已经上架");
        }

    }

    // 下架产品
    @Override
    @Transactional
    public void deactiveProduct(Long productId){
        LoanProduct product = loanProductMapper.findById(productId);
        if(product.getStatus().equals(ProductStatus.ACTIVE)){
            product.setStatus(ProductStatus.INACTIVE);
            loanProductMapper.update(product);
        }else{
            throw new BusinessException(404,"产品已经下架");
        }
    }

    /*
     * 批量创建产品选项
     */
    @Override
    @Transactional
    public void batchCreateLoanOptions(Long productId, List<LoanOption> options){
        if (loanProductMapper.findById(productId)==null) {
            throw new BusinessException(404,"该产品不存在，无法添加选项");
        }
    
        for (LoanOption option : options) {
            if (option == null) continue;
            
            option.setProductId(productId);
        }

        loanOptionMapper.insertBatch(options);
        
    }

    // 删除产品某个选项
    @Override
    @Transactional
    public int deleteLoanOption(Long optionId) {
        return loanOptionMapper.deleteById(optionId);
    }

    // 删除产品
    @Override
    @Transactional
    public int deleteLoanProduct(Long productId){
        loanOptionMapper.deleteByProductId(productId);  // 先删除该产品所有选项
        return loanProductMapper.delete(productId);
    }

    //批量删除产品
    @Override
    @Transactional
    public void batchDeleteLoanProducts(List<Long> productIds){
        loanOptionMapper.batchDeleteByProductIds(productIds);
        // 再删除产品
        loanProductMapper.batchDelete(productIds);
    }

    //批量删除选项
    @Override
    @Transactional
    public void batchDeleteLoanOptionsByIds(List<Long> optionIds){
        if (optionIds == null || optionIds.isEmpty()) {
            return;
        }
        loanOptionMapper.batchDeleteByIds(optionIds);
    }

    /*
     * 更新产品
     */
    @Override
    public ProductDto updateLoanProduct(Long productId,ProductDto dto){

        LoanProduct existing = loanProductMapper.findById(productId);
        if (existing == null) {
            throw new BusinessException(404,"该产品不存在");
        }

        // 只更新非空字段（避免覆盖为 null）
        if (dto.getProductName() != null) {
            existing.setProductName(dto.getProductName());
        }
        if (dto.getDescription() != null) {
            existing.setDescription(dto.getDescription());
        }
        if (dto.getLoanUsage() != null) {
            existing.setLoanUsage(dto.getLoanUsage());
        }
        if (dto.getMinTerm() != null) {
            existing.setMinTerm(dto.getMinTerm());
        }
        if (dto.getMaxTerm() != null) {
            existing.setMaxTerm(dto.getMaxTerm());
        }
        if (dto.getTermStep() != null) {
            existing.setTermStep(dto.getTermStep());
        }
        if (dto.getPromotionDetails() != null) {
            existing.setPromotionDetails(dto.getPromotionDetails());
        }
        if (dto.getMinTerm() != null && dto.getMaxTerm() != null 
            && dto.getMinTerm() > dto.getMaxTerm()) {
            throw new BusinessException(400, "最短期数不能大于最长期数");
        }
        existing.setUpdateTime(LocalDateTime.now());

        loanProductMapper.update(existing);

        // 处理 options：仅更新已存在的选项（带 id）
        if (dto.getOptions() != null) {
            for (LoanOption opt : dto.getOptions()) {
                if (opt.getId() == null) {
                    throw new BusinessException(400, "选项不存在");
                }

                // 校验选项归属
                LoanOption dbOpt = loanOptionMapper.selectById(opt.getId());
                if (dbOpt == null) {
                    throw new BusinessException(400, "贷款选项不存在: " + opt.getId());
                }
                if (!productId.equals(dbOpt.getProductId())) {
                    throw new BusinessException(400, "贷款选项不属于当前产品: " + opt.getId());
                }

                // 执行选择性更新（只更新非空字段）
                opt.setProductId(productId); 
                loanOptionMapper.update(opt);
                opt.setUpdateTime(LocalDateTime.now());
            }
        }

        // 构建并返回更新后的完整 ProductDto
        ProductDto result = new ProductDto();
        BeanUtils.copyProperties(existing, result);
        result.setOptions(loanOptionMapper.selectByProductId(productId));
        return result;
    }

    /*
     * 管理员获取产品详情
     */
    @Override
    @Transactional
    public ProductDto adminGetProductById(Long id){
        LoanProduct product = loanProductMapper.findById(id);
        if (product == null) return null;

        List<LoanOption> options = loanOptionMapper.selectByProductId(id);

        ProductDto dto = new ProductDto();
        BeanUtils.copyProperties(product, dto);
        dto.setOptions(options);
        return dto;
    }

    /*
     * 管理员获取所有产品列表
     */
    @Override
    @Transactional
    public List<ListProductResponse> adminGetAllProducts (){
        List<LoanProduct> products = loanProductMapper.findAll();
        return products.stream().map(product -> {
            ListProductResponse response = new ListProductResponse();
            response.setProductId(product.getId());
            response.setProductName(product.getProductName());
            response.setDescription(product.getDescription());
            response.setUsage(product.getLoanUsage());
            response.setStatus(product.getStatus());
            response.setCreateTime(product.getCreateTime());
            response.setUpdateTime(product.getUpdateTime());

            return response;
        }).collect(Collectors.toList());
    }


    /*
     * 用户使用
     */
    /*
     * 用户获取贷款产品
     */
    @Override
    public List<UserGetProductResponse> searchProductsByName(String name){
        if (name == null || name.trim().isEmpty()) {
            return Collections.emptyList(); // 空关键词返回空列表
        }

        // 1. 调用 Mapper 模糊查询
        List<LoanProduct> products = loanProductMapper.findByProductNameLike(name.trim());

        // 2. 转换为 UserGetProductResponse 列表（复用之前的转换逻辑）
        return products.stream().map(product -> {
            // 生成 terms
            List<Integer> terms = new ArrayList<>();
            if (product.getMinTerm() != null && product.getMaxTerm() != null && product.getTermStep() != null) {
                for (int t = product.getMinTerm(); t <= product.getMaxTerm(); t += product.getTermStep()) {
                    terms.add(t);
                }
            }

            // 查询并转换 options
            List<LoanOption> options = loanOptionMapper.selectByProductId(product.getId());
            List<LoanOptionResponse> optionResponses = options.stream()
                .map(opt -> {
                    LoanOptionResponse resp = new LoanOptionResponse();
                    resp.setOptionId(opt.getId());
                    resp.setLoanAmount(opt.getLoanAmount());
                    resp.setInterestRate(opt.getInterestRate());
                    resp.setLoanPeriod(opt.getLoanPeriod());
                    resp.setRepaidType(opt.getRepaidType());
                    return resp;
                })
                .collect(Collectors.toList());

            // 构建响应对象
            UserGetProductResponse response = new UserGetProductResponse();
            response.setProductId(product.getId());
            response.setProductName(product.getProductName());
            response.setDescription(product.getDescription());
            response.setLoanUsage(product.getLoanUsage());
            response.setPromotionDetails(product.getPromotionDetails());
            response.setTerms(terms);
            response.setOptions(optionResponses);

            return response;
        }).collect(Collectors.toList());
    }

    /*
     * 用户获得所有贷款产品
     */
    @Override
    public List<UserGetProductResponse> getAllLoanProducts(){
        List<LoanProduct> products = loanProductMapper.findAllActive();
        return products.stream().map(product -> {
            // 生成 terms 列表
            List<Integer> terms = new ArrayList<>();
            if (product.getMinTerm() != null && product.getMaxTerm() != null && product.getTermStep() != null) {
                for (int t = product.getMinTerm(); t <= product.getMaxTerm(); t += product.getTermStep()) {
                    terms.add(t);
                }
            }

            // 查询并转换贷款选项
            List<LoanOption> options = loanOptionMapper.selectByProductId(product.getId());
            List<LoanOptionResponse> optionResponses = options.stream()
                .map(opt -> {
                    LoanOptionResponse resp = new LoanOptionResponse();
                    resp.setOptionId(opt.getId());
                    resp.setLoanAmount(opt.getLoanAmount());
                    resp.setInterestRate(opt.getInterestRate());
                    resp.setLoanPeriod(opt.getLoanPeriod());
                    resp.setRepaidType(opt.getRepaidType());
                    return resp;
                })
                .collect(Collectors.toList());

            // 构建最终响应对象
            UserGetProductResponse response = new UserGetProductResponse();
            response.setProductId(product.getId());
            response.setProductName(product.getProductName());
            response.setDescription(product.getDescription()); 
            response.setLoanUsage(product.getLoanUsage()); 
            response.setPromotionDetails(product.getPromotionDetails());
            response.setTerms(terms);
            response.setOptions(optionResponses);

            return response;
        }).collect(Collectors.toList());
    }
}
