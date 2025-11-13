package com.example.personal_loan.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.dao.LoanOptionMapper;
import com.example.personal_loan.dao.LoanProductMapper;
import com.example.personal_loan.dto.ProductDto;
import com.example.personal_loan.entity.LoanOption;
import com.example.personal_loan.entity.LoanProduct;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.service.LoanProductService;
import com.example.personal_loan.vo.LoanProductVO;

@Service
public class LoanProductServiceImpl implements LoanProductService{

    @Autowired
    private LoanProductMapper loanProductMapper;

    @Autowired
    private LoanOptionMapper loanOptionMapper;

    /*
     * 管理员使用
     */
    
    @Override
    @Transactional
    public ProductDto createLoanProduct(ProductDto dto) {
        LoanProduct product = new LoanProduct();
        dto.setCreateTime(LocalDateTime.now());
        dto.setUpdateTime(LocalDateTime.now());
        BeanUtils.copyProperties(dto, product);

        loanProductMapper.create(product); // MyBatis 会回填 id
        dto.setId(product.getId());

        // 批量插入选项
        if (dto.getOptions() != null && !dto.getOptions().isEmpty()) {
            for (LoanOption opt : dto.getOptions()) {
                opt.setProductId(product.getId());
                loanOptionMapper.insert(opt); 
            }
        }

        return dto;
    }

    @Override
    @Transactional
    public int deleteLoanOption(Long optionId) {
        return loanOptionMapper.deleteById(optionId);
    }

    @Override
    @Transactional
    public int deleteLoanProduct(Long productId){
        loanOptionMapper.deleteByProductId(productId);  // 先删除该产品所有选项
        return loanProductMapper.delete(productId);
    }

    @Override
    public ProductDto updateLoanProduct(Long productId,ProductDto dto){

        LoanProduct existing = loanProductMapper.findById(productId);
        if (existing == null) {
            throw new BusinessException("404","该产品不存在");
        }

        // 只更新非空字段（避免覆盖为 null）
        if (dto.getProductName() != null) {
            existing.setProductName(dto.getProductName());
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
        existing.setUpdateTime(LocalDateTime.now());

        loanProductMapper.update(existing);

        // 如果 options 字段存在（即前端明确提供了选项列表），才做替换
        // 注意：这里假设传了 options 就是要完全替换；如果要支持选项的部分更新，需更复杂逻辑
        if (dto.getOptions() != null) {
            loanOptionMapper.deleteByProductId(productId);
            if (!dto.getOptions().isEmpty()) {
                for (LoanOption opt : dto.getOptions()) {
                    opt.setProductId(productId);
                }
                loanOptionMapper.insertBatch(dto.getOptions());
            }
        }
        ProductDto updatedDto = new ProductDto();
        BeanUtils.copyProperties(existing, updatedDto);
        updatedDto.setOptions(loanOptionMapper.selectByProductId(productId));
        return updatedDto;
    }


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

    @Override
    @Transactional
    public List<ProductDto> adminGetAllProducts (){
        List<LoanProduct> products = loanProductMapper.findAll();
        return products.stream().map(product -> {
            ProductDto dto = new ProductDto();
            BeanUtils.copyProperties(product, dto);
            dto.setOptions(loanOptionMapper.selectByProductId(product.getId()));
            return dto;
        }).collect(Collectors.toList());
    }


    /*
     * 用户使用
     */

    @Override
    public LoanProductVO getLoanProductById(Long id){
        LoanProduct product = loanProductMapper.findById(id);
        if (product == null) return null;

        List<LoanOption> options = loanOptionMapper.selectByProductId(id);
        List<LoanOption> optionVOs = options.stream().map(opt -> {
            LoanOption vo = new LoanOption();
            BeanUtils.copyProperties(opt, vo);
            return vo;
        }).collect(Collectors.toList());

        LoanProductVO vo = new LoanProductVO();
        vo.setId(product.getId());
        vo.setProductName(product.getProductName());
        vo.setPromotionDetails(product.getPromotionDetails());
        vo.setCreateTime(product.getCreateTime());
        vo.setOptions(optionVOs);
        return vo;
    }

    @Override
    public List<LoanProductVO> getAllLoanProducts(){
        List<LoanProduct> products = loanProductMapper.findAll();
        return products.stream().map(product -> {
            List<LoanOption> options = loanOptionMapper.selectByProductId(product.getId());
            List<LoanOption> optionVOs = options.stream()
                .map(opt -> {
                    LoanOption vo = new LoanOption();
                    BeanUtils.copyProperties(opt, vo);
                    return vo;
                })
                .collect(Collectors.toList());

            LoanProductVO vo = new LoanProductVO();
            vo.setId(product.getId());
            vo.setProductName(product.getProductName());
            vo.setPromotionDetails(product.getPromotionDetails());
            vo.setCreateTime(product.getCreateTime());
            vo.setOptions(optionVOs);
            return vo;
        }).collect(Collectors.toList());
    }
}
