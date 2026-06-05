package com.example.personal_loan.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.aop.RedisLocked;
import com.example.personal_loan.dto.AdminGetProDetailResponse;
import com.example.personal_loan.dto.ListProductResponse;
import com.example.personal_loan.dto.LoanOptionResponse;
import com.example.personal_loan.dto.ProductDto;
import com.example.personal_loan.dto.SearchByDateRequest;
import com.example.personal_loan.dto.UserGetProductResponse;
import com.example.personal_loan.entity.LoanOption;
import com.example.personal_loan.entity.LoanProduct;
import com.example.personal_loan.enums.ProductStatus;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.mapper.LoanOptionMapper;
import com.example.personal_loan.mapper.LoanProductMapper;
import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.service.LoanProductService;

@Service
public class LoanProductServiceImpl implements LoanProductService{

    @Autowired
    private LoanProductMapper loanProductMapper;

    @Autowired
    private LoanOptionMapper loanOptionMapper;

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private OrderMapper orderMapper;

    /*
     * 管理员使用
     */

    // 新增产品
    @Override
    @Transactional
    @RedisLocked(key = "'lock:loan-product:create:' + #p0.productName")
    public ProductDto createLoanProduct(ProductDto dto) {

        LoanProduct product = new LoanProduct();

        dto.setStatus(ProductStatus.已下架); // 默认下架
        dto.setCreateTime(LocalDateTime.now());
        dto.setUpdateTime(LocalDateTime.now());
        BeanUtils.copyProperties(dto, product);

        loanProductMapper.create(product);
        dto.setId(product.getId());

        // 期数判空?
        if (dto.getMinTerm() != null && dto.getMaxTerm() != null && dto.getMinTerm() > dto.getMaxTerm()) {
            throw new BusinessException(400, "最短期数不能大于最长期数");
        }
        // 基础非负校验
        if (dto.getMinTerm() <= 0 || dto.getMaxTerm() <= 0 || dto.getTermStep().compareTo(dto.getMinTerm()) <= 0 || dto.getTermStep() <= 0) {
            throw new BusinessException(400, "期数和步长必须大于0");
        }
        // 期数，步长合法性校验，满足等差关系
        if ((dto.getMaxTerm() - dto.getMinTerm()) % dto.getTermStep() != 0) {
            throw new BusinessException(400, "不合法的期数或步长");
        }
        if (dto.getMinAmount() == null || dto.getMaxAmount() == null) {
            throw new BusinessException(400, "最小贷款金额和最大贷款金额不能为空");
        }
        if (dto.getMinAmount().compareTo(dto.getMaxAmount()) >= 0) {
            throw new BusinessException(400, "最小贷款金额必须小于最大贷款金额");
        }
        if (dto.getMinAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "最小贷款金额必须大于0");
        }

        // 批量插入选项
        if (dto.getOptions() != null && !dto.getOptions().isEmpty()) {
            for (LoanOption opt : dto.getOptions()) {
                opt.setProductId(product.getId());
                opt.setCreateTime(LocalDateTime.now());
                opt.setUpdateTime(LocalDateTime.now());
                loanOptionMapper.insert(opt); 
            }
        }else{
            throw new BusinessException(400, "贷款选项不能为空");
        }

        return dto;
    }

    // 上架产品
    @Override
    @Transactional
    @RedisLocked(key = "'lock:loan-product:active:' + #p0")
    public void activeProduct(Long productId){
        LoanProduct product = loanProductMapper.findById(productId);
        if(product.getStatus().equals(ProductStatus.已下架)){
            product.setStatus(ProductStatus.上架中);
            product.setUpdateTime(LocalDateTime.now());
            loanProductMapper.update(product);
        }else{
            throw new BusinessException(404,"产品已经上架");
        }

    }

    // 下架产品
    @Override
    @Transactional
    @RedisLocked(key = "'lock:loan-product:deactive:' + #p0")
    public void deactiveProduct(Long productId){
        LoanProduct product = loanProductMapper.findById(productId);
        if(product.getStatus().equals(ProductStatus.上架中)){
            product.setStatus(ProductStatus.已下架);
            product.setUpdateTime(LocalDateTime.now());
            loanProductMapper.update(product);
        }else{
            throw new BusinessException(404,"产品已经下架");
        }
    }

    // 批量创建产品选项
    @Override
    @Transactional
    @RedisLocked(key = "'lock:loan-product:options:create:' + #p0")
    public void batchCreateLoanOptions(Long productId, List<LoanOption> options){
        // 校验产品是否存在
        LoanProduct product = loanProductMapper.findById(productId);
        if (product == null) {
            throw new BusinessException(404,"该产品不存在，无法添加选项");
        }
       
        for (LoanOption option : options) {
            if (option == null) continue;
            option.setCreateTime(LocalDateTime.now());
            option.setUpdateTime(LocalDateTime.now());
            option.setProductId(productId);
        }

        loanOptionMapper.insertBatch(options);
        
    }

    // 删除产品某个选项
    @Override
    @Transactional
    @RedisLocked(key = "'lock:loan-option:delete:' + #p0")
    public int deleteLoanOption(Long optionId) {
        // 检查选项是否存在
        LoanOption option = loanOptionMapper.selectById(optionId);
        if (option == null) {
            throw new BusinessException(404, "该选项不存在");
        }
        
        // 检查选项所属的产品是否被贷款申请引用
        if (applicationMapper.countByProductId(option.getProductId()) > 0) {
            throw new BusinessException(400, "该产品已被贷款申请引用，无法删除选项");
        }
        
        // 检查选项所属的产品是否被订单引用
        if (orderMapper.countByProductId(option.getProductId()) > 0) {
            throw new BusinessException(400, "该产品已被订单引用，无法删除选项");
        }
        
        return loanOptionMapper.deleteById(optionId);
    }

    // 删除产品
    @Override
    @Transactional
    @RedisLocked(key = "'lock:loan-product:delete:' + #p0")
    public int deleteLoanProduct(Long productId){
        // 检查产品是否存在
        if (loanProductMapper.findById(productId) == null) {
            throw new BusinessException(404, "该产品不存在");
        }
        
        // 检查产品是否被贷款申请引用
        if (applicationMapper.countByProductId(productId) > 0) {
            throw new BusinessException(400, "删除失败，产品已被使用");
        }
        
        // 检查产品是否被订单引用
        if (orderMapper.countByProductId(productId) > 0) {
            throw new BusinessException(400, "删除失败，产品已被使用");
        }
        
        loanOptionMapper.deleteByProductId(productId);  // 先删除该产品所有选项
        return loanProductMapper.delete(productId);
    }

    //批量删除产品
    @Override
    @Transactional
    @RedisLocked(key = "'lock:loan-product:batch-delete'")
    public void batchDeleteLoanProducts(List<Long> productIds){
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        
        // 检查产品是否被贷款申请引用
        if (applicationMapper.countByProductIds(productIds) > 0) {
            throw new BusinessException(400, "删除失败，部分产品已被使用");
        }
        
        // 检查产品是否被订单引用
        if (orderMapper.countByProductIds(productIds) > 0) {
            throw new BusinessException(400, "删除失败，部分产品已被使用");
        }
        
        loanOptionMapper.batchDeleteByProductIds(productIds);
        // 再删除产品
        loanProductMapper.batchDelete(productIds);
    }

    //批量删除选项
    @Override
    @Transactional
    @RedisLocked(key = "'lock:loan-option:batch-delete'")
    public void batchDeleteLoanOptionsByIds(List<Long> optionIds){
        if (optionIds == null || optionIds.isEmpty()) {
            return;
        }
        
        // 检查每个选项所属的产品是否被引用
        for (Long optionId : optionIds) {
            LoanOption option = loanOptionMapper.selectById(optionId);
            if (option != null) {
                // 检查产品是否被贷款申请引用
                if (applicationMapper.countByProductId(option.getProductId()) > 0) {
                    throw new BusinessException(400, "删除失败，部分选项已被使用");
                }
                
                // 检查产品是否被订单引用
                if (orderMapper.countByProductId(option.getProductId()) > 0) {
                    throw new BusinessException(400, "删除失败，部分选项已被使用");
                }
            }
        }
        
        loanOptionMapper.batchDeleteByIds(optionIds);
    }

    // 更新产品
    @Override
    @RedisLocked(key = "'lock:loan-product:update:' + #p0")
    public ProductDto updateLoanProduct(Long productId,ProductDto dto){

        LoanProduct existing = loanProductMapper.findById(productId);
        if (existing == null) {
            throw new BusinessException(404,"该产品不存在");
        }
        // minTerm,maxTerm,termStep必须同时提供或同时不提供
        if (!((dto.getMinTerm() != null && dto.getMaxTerm() != null && dto.getTermStep() != null)
            || (dto.getMinTerm() == null && dto.getMaxTerm() == null && dto.getTermStep() == null))) {
            throw new BusinessException(400, "期数和步长必须同时提供");
        }
        //校验期数和步长
        if (dto.getMinTerm() != null && dto.getMaxTerm() != null && dto.getTermStep() != null) {
            // 非0校验
            if (dto.getMinTerm() <= 0 || dto.getMaxTerm() <= 0 || dto.getTermStep() <= 0) {
                throw new BusinessException(400, "期数和步长必须大于0");
            }
            // 最短期数不能大于最长期数
            if (dto.getMinTerm() > dto.getMaxTerm()) {
                throw new BusinessException(400, "最短期数不能大于最长期数");
            }
            // 期数，步长合法性校验，满足等差关系
            if ((dto.getMaxTerm() - dto.getMinTerm()) % dto.getTermStep() != 0) {
                throw new BusinessException(400, "不合法的期数或步长");
            }
        }
        // 校验金额
        if (dto.getMinAmount() != null && dto.getMaxAmount() != null) {
            if (dto.getMinAmount().compareTo(dto.getMaxAmount()) >= 0) {
                throw new BusinessException(400, "最小贷款金额必须小于最大贷款金额");
            }
            if (dto.getMinAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "最小贷款金额必须大于0");
            }
        }
        if ((dto.getMinAmount() != null && dto.getMaxAmount() == null) || 
            (dto.getMinAmount() == null && dto.getMaxAmount() != null)) {
            throw new BusinessException(400, "最小贷款金额和最大贷款金额必须同时提供");
        }

        // 更新
        BeanUtils.copyProperties(dto, existing);
        existing.setUpdateTime(LocalDateTime.now());
        loanProductMapper.update(existing);
        
        // 处理 options：仅更新已存在的选项（带 id）
        if (dto.getOptions() != null) {
            for (LoanOption opt : dto.getOptions()) {
                if (opt.getOptionId() == null) {
                    throw new BusinessException(400, "选项不存在");
                }

                // 校验选项归属
                LoanOption dbOpt = loanOptionMapper.selectById(opt.getOptionId());
                if (dbOpt == null) {
                    throw new BusinessException(400, "贷款选项不存在: " + opt.getOptionId());
                }
                if (!productId.equals(dbOpt.getProductId())) {
                    throw new BusinessException(400, "贷款选项不属于当前产品: " + opt.getOptionId());
                }
               
                // 执行选择性更新（只更新非空字段）
                opt.setProductId(productId); 
                opt.setUpdateTime(LocalDateTime.now());
                loanOptionMapper.update(opt);
            }
        }

        // 重新查询更新后的产品信息
        LoanProduct updatedProduct = loanProductMapper.findById(productId);
        
        // 构建并返回更新后的完整 ProductDto
        ProductDto result = new ProductDto();
        BeanUtils.copyProperties(updatedProduct, result);
        result.setOptions(loanOptionMapper.selectByProductId(productId));
        return result;
    }

    // 管理员获取单个产品详情
    @Override
    @Transactional
    public AdminGetProDetailResponse adminGetProductById(Long id){
        LoanProduct product = loanProductMapper.findById(id);
        if (product == null) {
            throw new BusinessException(404,"产品不存在");
        }

        List<LoanOption> options = loanOptionMapper.selectByProductId(id);

        List<Integer> terms = generateTerms(product);
        
        AdminGetProDetailResponse response = new AdminGetProDetailResponse();
        response.setProductId(product.getId());
        response.setProductName(product.getProductName());
        response.setDescription(product.getDescription());
        response.setLoanUsage(product.getLoanUsage());
        response.setTerms(terms);
        response.setMinAmount(product.getMinAmount());
        response.setMaxAmount(product.getMaxAmount());
        response.setPromotionDetails(product.getPromotionDetails());
        response.setStatus(product.getStatus());
        response.setCreateTime(product.getCreateTime());
        response.setUpdateTime(product.getUpdateTime());
        response.setOptions(options);

        return response;
    }

    // 管理员获取所有产品列表
    @Override
    @Transactional
    public List<ListProductResponse> adminGetAllProducts (){
        List<LoanProduct> products = loanProductMapper.findAll();
        return products.stream().map(product -> {
            ListProductResponse response = new ListProductResponse();
            response.setProductId(product.getId());
            response.setProductName(product.getProductName());
            response.setDescription(product.getDescription());
            response.setLoanUsage(product.getLoanUsage());
            response.setStatus(product.getStatus());
            response.setMinAmount(product.getMinAmount());
            response.setMaxAmount(product.getMaxAmount());
            response.setCreateTime(product.getCreateTime());
            response.setUpdateTime(product.getUpdateTime());

            return response;
        }).collect(Collectors.toList());
    }

    // 管理员 根据更新时间和创建时间范围搜索产品
    @Override
    public List<ListProductResponse> searchByDate(SearchByDateRequest request) {
        return loanProductMapper.searchByDate(
            request.getCreateStartDate(),
            request.getCreateEndDate(),
            request.getUpdateStartDate(),
            request.getUpdateEndDate()
        );
    }

    /*
     * 用户根据产品名搜索贷款产品
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
            List<Integer> terms = generateTerms(product);

            // 查询并转换 options
            List<LoanOption> options = loanOptionMapper.selectByProductId(product.getId());
            List<LoanOptionResponse> optionResponses = options.stream()
                .map(opt -> {
                    LoanOptionResponse resp = new LoanOptionResponse();
                    resp.setOptionId(opt.getOptionId());
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
            response.setMinAmount(product.getMinAmount());
            response.setMaxAmount(product.getMaxAmount());
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
            List<Integer> terms = generateTerms(product);

            // 查询并转换贷款选项
            List<LoanOption> options = loanOptionMapper.selectByProductId(product.getId());
            List<LoanOptionResponse> optionResponses = options.stream()
                .map(opt -> {
                    LoanOptionResponse resp = new LoanOptionResponse();
                    resp.setOptionId(opt.getOptionId());
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
            response.setMinAmount(product.getMinAmount());
            response.setMaxAmount(product.getMaxAmount());
            response.setTerms(terms);
            response.setOptions(optionResponses);

            return response;
        }).collect(Collectors.toList());
    }

    @Override
    public List<UserGetProductResponse> getTopLoanProducts(int limit) {
        List<LoanProduct> products = loanProductMapper.findTopActive(limit);
        return products.stream().map(product -> {
            List<Integer> terms = generateTerms(product);
            List<LoanOption> options = loanOptionMapper.selectByProductId(product.getId());
            List<LoanOptionResponse> optionResponses = options.stream()
                .map(opt -> {
                    LoanOptionResponse resp = new LoanOptionResponse();
                    resp.setOptionId(opt.getOptionId());
                    resp.setInterestRate(opt.getInterestRate());
                    resp.setLoanPeriod(opt.getLoanPeriod());
                    resp.setRepaidType(opt.getRepaidType());
                    return resp;
                })
                .collect(Collectors.toList());

            UserGetProductResponse response = new UserGetProductResponse();
            response.setProductId(product.getId());
            response.setProductName(product.getProductName());
            response.setDescription(product.getDescription());
            response.setLoanUsage(product.getLoanUsage());
            response.setPromotionDetails(product.getPromotionDetails());
            response.setMinAmount(product.getMinAmount());
            response.setMaxAmount(product.getMaxAmount());
            response.setTerms(terms);
            response.setOptions(optionResponses);
            return response;
        }).collect(Collectors.toList());
    }

    // 内部工具方法，生成terms列表
    private List<Integer> generateTerms(LoanProduct product) {
        List<Integer> terms = new ArrayList<>();
        if (product.getMinTerm() != null && product.getMaxTerm() != null && product.getTermStep() != null) {
            for (int t = product.getMinTerm(); t <= product.getMaxTerm(); t += product.getTermStep()) {
                terms.add(t);
            }
        }
        return terms;
    }
}
