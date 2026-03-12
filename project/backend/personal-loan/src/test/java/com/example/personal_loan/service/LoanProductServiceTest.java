package com.example.personal_loan.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.personal_loan.dto.AdminGetProDetailResponse;
import com.example.personal_loan.dto.ListProductResponse;
import com.example.personal_loan.dto.ProductDto;
import com.example.personal_loan.dto.SearchByDateRequest;
import com.example.personal_loan.dto.UserGetProductResponse;
import com.example.personal_loan.entity.LoanOption;
import com.example.personal_loan.entity.LoanProduct;
import com.example.personal_loan.enums.ProductStatus;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.mapper.LoanOptionMapper;
import com.example.personal_loan.mapper.LoanProductMapper;
import com.example.personal_loan.service.impl.LoanProductServiceImpl;

@ExtendWith(MockitoExtension.class)
class LoanProductServiceTest {

    @Mock
    private LoanProductMapper loanProductMapper;

    @Mock
    private LoanOptionMapper loanOptionMapper;

    @InjectMocks
    private LoanProductServiceImpl loanProductService;

    private LoanProduct loanProduct;
    private LoanOption loanOption;
    private ProductDto productDto;

    @BeforeEach
    void setUp() {
        loanProduct = new LoanProduct();
        loanProduct.setId(1L);
        loanProduct.setProductName("个人消费贷");
        loanProduct.setDescription("个人消费贷款产品");
        loanProduct.setLoanUsage("日常消费");
        loanProduct.setMinAmount(new BigDecimal("1000"));
        loanProduct.setMaxAmount(new BigDecimal("50000"));
        loanProduct.setMinTerm(3);
        loanProduct.setMaxTerm(24);
        loanProduct.setTermStep(3);
        loanProduct.setStatus(ProductStatus.已下架);
        loanProduct.setCreateTime(LocalDateTime.now());
        loanProduct.setUpdateTime(LocalDateTime.now());

        loanOption = new LoanOption();
        loanOption.setOptionId(1L);
        loanOption.setProductId(1L);
        loanOption.setInterestRate(new BigDecimal("0.05"));
        loanOption.setLoanPeriod(12);
        loanOption.setRepaidType(com.example.personal_loan.enums.RepaidType.等额本息);
        loanOption.setCreateTime(LocalDateTime.now());
        loanOption.setUpdateTime(LocalDateTime.now());

        productDto = new ProductDto();
        productDto.setProductName("个人消费贷");
        productDto.setDescription("个人消费贷款产品");
        productDto.setLoanUsage("日常消费");
        productDto.setMinAmount(new BigDecimal("1000"));
        productDto.setMaxAmount(new BigDecimal("50000"));
        productDto.setMinTerm(3);
        productDto.setMaxTerm(24);
        productDto.setTermStep(3);
        productDto.setOptions(Arrays.asList(loanOption));
    }

    @Test
    void testCreateLoanProduct_Success() {
        when(loanProductMapper.create(any(LoanProduct.class))).thenAnswer(invocation -> {
            LoanProduct p = invocation.getArgument(0);
            p.setId(1L);
            return null;
        });
        when(loanOptionMapper.insert(any(LoanOption.class))).thenAnswer(invocation -> {
            LoanOption o = invocation.getArgument(0);
            o.setOptionId(1L);
            return null;
        });

        ProductDto result = loanProductService.createLoanProduct(productDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(loanProductMapper).create(any(LoanProduct.class));
        verify(loanOptionMapper).insert(any(LoanOption.class));
    }

    @Test
    void testCreateLoanProduct_InvalidTerm() {
        productDto.setMinTerm(24);
        productDto.setMaxTerm(3);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            loanProductService.createLoanProduct(productDto)
        );
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("最短期数不能大于最长期数"));
    }

    @Test
    void testCreateLoanProduct_InvalidAmount() {
        productDto.setMinAmount(new BigDecimal("50000"));
        productDto.setMaxAmount(new BigDecimal("1000"));

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            loanProductService.createLoanProduct(productDto)
        );
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("最小贷款金额必须小于最大贷款金额"));
    }

    @Test
    void testActiveProduct_Success() {
        loanProduct.setStatus(ProductStatus.已下架);
        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        assertDoesNotThrow(() -> loanProductService.activeProduct(1L));

        verify(loanProductMapper).update(any(LoanProduct.class));
    }

    @Test
    void testActiveProduct_AlreadyActive() {
        loanProduct.setStatus(ProductStatus.上架中);
        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            loanProductService.activeProduct(1L)
        );
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("产品已经上架"));
    }

    @Test
    void testDeactiveProduct_Success() {
        loanProduct.setStatus(ProductStatus.上架中);
        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        assertDoesNotThrow(() -> loanProductService.deactiveProduct(1L));

        verify(loanProductMapper).update(any(LoanProduct.class));
    }

    @Test
    void testDeactiveProduct_AlreadyInactive() {
        loanProduct.setStatus(ProductStatus.已下架);
        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            loanProductService.deactiveProduct(1L)
        );
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("产品已经下架"));
    }

    @Test
    void testBatchCreateLoanOptions_Success() {
        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        assertDoesNotThrow(() -> loanProductService.batchCreateLoanOptions(1L, Arrays.asList(loanOption)));

        verify(loanOptionMapper).insertBatch(anyList());
    }

    @Test
    void testBatchCreateLoanOptions_ProductNotFound() {
        when(loanProductMapper.findById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            loanProductService.batchCreateLoanOptions(1L, Arrays.asList(loanOption))
        );
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("该产品不存在"));
    }

    @Test
    void testDeleteLoanOption_Success() {
        when(loanOptionMapper.deleteById(1L)).thenReturn(1);

        int result = loanProductService.deleteLoanOption(1L);

        assertEquals(1, result);
    }

    @Test
    void testDeleteLoanProduct_Success() {
        when(loanProductMapper.delete(1L)).thenReturn(1);

        int result = loanProductService.deleteLoanProduct(1L);

        assertEquals(1, result);
        verify(loanOptionMapper).deleteByProductId(1L);
    }

    @Test
    void testBatchDeleteLoanProducts_Success() {
        List<Long> productIds = Arrays.asList(1L, 2L);

        assertDoesNotThrow(() -> loanProductService.batchDeleteLoanProducts(productIds));

        verify(loanOptionMapper).batchDeleteByProductIds(productIds);
        verify(loanProductMapper).batchDelete(productIds);
    }

    @Test
    void testBatchDeleteLoanOptionsByIds_Success() {
        List<Long> optionIds = Arrays.asList(1L, 2L);

        assertDoesNotThrow(() -> loanProductService.batchDeleteLoanOptionsByIds(optionIds));

        verify(loanOptionMapper).batchDeleteByIds(optionIds);
    }

    @Test
    void testBatchDeleteLoanOptionsByIds_EmptyList() {
        assertDoesNotThrow(() -> loanProductService.batchDeleteLoanOptionsByIds(Collections.emptyList()));

        verify(loanOptionMapper, never()).batchDeleteByIds(anyList());
    }

    @Test
    void testUpdateLoanProduct_Success() {
        ProductDto updateDto = new ProductDto();
        updateDto.setProductName("更新后的产品");
        updateDto.setDescription("更新后的描述");
        updateDto.setMinAmount(new BigDecimal("2000"));
        updateDto.setMaxAmount(new BigDecimal("60000"));
        updateDto.setMinTerm(6);
        updateDto.setMaxTerm(36);
        updateDto.setOptions(Arrays.asList(loanOption));

        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);
        when(loanOptionMapper.selectById(1L)).thenReturn(loanOption);
        when(loanOptionMapper.selectByProductId(1L)).thenReturn(Arrays.asList(loanOption));

        ProductDto result = loanProductService.updateLoanProduct(1L, updateDto);

        assertNotNull(result);
        assertEquals("更新后的产品", result.getProductName());
        assertEquals("更新后的描述", result.getDescription());
        verify(loanProductMapper).update(any(LoanProduct.class));
    }

    @Test
    void testUpdateLoanProduct_ProductNotFound() {
        when(loanProductMapper.findById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            loanProductService.updateLoanProduct(1L, productDto)
        );
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("该产品不存在"));
    }

    @Test
    void testUpdateLoanProduct_InvalidTerm() {
        ProductDto updateDto = new ProductDto();
        updateDto.setMinTerm(36);
        updateDto.setMaxTerm(6);

        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            loanProductService.updateLoanProduct(1L, updateDto)
        );
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("最短期数不能大于最长期数"));
    }

    @Test
    void testUpdateLoanProduct_InvalidAmount() {
        ProductDto updateDto = new ProductDto();
        updateDto.setMinAmount(new BigDecimal("60000"));
        updateDto.setMaxAmount(new BigDecimal("2000"));

        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            loanProductService.updateLoanProduct(1L, updateDto)
        );
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("最小贷款金额必须小于最大贷款金额"));
    }

    @Test
    void testUpdateLoanProduct_PartialAmount() {
        ProductDto updateDto = new ProductDto();
        updateDto.setMinAmount(new BigDecimal("2000"));
        updateDto.setMaxAmount(null);

        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            loanProductService.updateLoanProduct(1L, updateDto)
        );
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("最小贷款金额和最大贷款金额必须同时提供"));
    }

    @Test
    void testUpdateLoanProduct_OptionWithoutId() {
        LoanOption optionWithoutId = new LoanOption();
        optionWithoutId.setInterestRate(new BigDecimal("0.06"));
        optionWithoutId.setLoanPeriod(24);

        ProductDto updateDto = new ProductDto();
        updateDto.setOptions(Arrays.asList(optionWithoutId));

        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            loanProductService.updateLoanProduct(1L, updateDto)
        );
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("选项不存在"));
    }

    @Test
    void testAdminGetProductById_Success() {
        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);
        when(loanOptionMapper.selectByProductId(1L)).thenReturn(Arrays.asList(loanOption));

        AdminGetProDetailResponse response = loanProductService.adminGetProductById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getProductId());
        assertEquals("个人消费贷", response.getProductName());
        assertEquals(8, response.getTerms().size());
        assertEquals(3, response.getTerms().get(0));
        assertEquals(24, response.getTerms().get(7));
        assertEquals(1, response.getOptions().size());
    }

    @Test
    void testAdminGetProductById_ProductNotFound() {
        when(loanProductMapper.findById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            loanProductService.adminGetProductById(1L)
        );
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("产品不存在"));
    }

    @Test
    void testAdminGetAllProducts_Success() {
        when(loanProductMapper.findAll()).thenReturn(Arrays.asList(loanProduct));

        List<ListProductResponse> responses = loanProductService.adminGetAllProducts();

        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void testSearchByDate_Success() {
        SearchByDateRequest request = new SearchByDateRequest();
        request.setCreateStartDate(LocalDate.of(2024, 1, 1));
        request.setCreateEndDate(LocalDate.of(2024, 12, 31));

        ListProductResponse listProductResponse = new ListProductResponse();
        listProductResponse.setProductId(1L);
        listProductResponse.setProductName("个人消费贷");
        listProductResponse.setDescription("个人消费贷款产品");
        listProductResponse.setLoanUsage("日常消费");
        listProductResponse.setMinAmount(new BigDecimal("1000"));
        listProductResponse.setMaxAmount(new BigDecimal("50000"));
        listProductResponse.setStatus(ProductStatus.已下架);
        listProductResponse.setCreateTime(LocalDateTime.now());
        listProductResponse.setUpdateTime(LocalDateTime.now());

        when(loanProductMapper.searchByDate(any(), any(), any(), any())).thenReturn(Arrays.asList(listProductResponse));

        List<ListProductResponse> responses = loanProductService.searchByDate(request);

        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void testSearchProductsByName_Success() {
        when(loanProductMapper.findByProductNameLike("消费")).thenReturn(Arrays.asList(loanProduct));
        when(loanOptionMapper.selectByProductId(1L)).thenReturn(Arrays.asList(loanOption));

        List<UserGetProductResponse> responses = loanProductService.searchProductsByName("消费");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getProductId());
        assertEquals("个人消费贷", responses.get(0).getProductName());
        assertEquals(8, responses.get(0).getTerms().size());
        assertEquals(1, responses.get(0).getOptions().size());
    }

    @Test
    void testSearchProductsByName_EmptyKeyword() {
        List<UserGetProductResponse> responses = loanProductService.searchProductsByName("");

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void testSearchProductsByName_NullKeyword() {
        List<UserGetProductResponse> responses = loanProductService.searchProductsByName(null);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void testGetAllLoanProducts_Success() {
        loanProduct.setStatus(ProductStatus.上架中);
        when(loanProductMapper.findAllActive()).thenReturn(Arrays.asList(loanProduct));
        when(loanOptionMapper.selectByProductId(1L)).thenReturn(Arrays.asList(loanOption));

        List<UserGetProductResponse> responses = loanProductService.getAllLoanProducts();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getProductId());
        assertEquals("个人消费贷", responses.get(0).getProductName());
        assertEquals(8, responses.get(0).getTerms().size());
        assertEquals(1, responses.get(0).getOptions().size());
    }

    @Test
    void testGetAllLoanProducts_EmptyList() {
        when(loanProductMapper.findAllActive()).thenReturn(Collections.emptyList());

        List<UserGetProductResponse> responses = loanProductService.getAllLoanProducts();

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }
}
