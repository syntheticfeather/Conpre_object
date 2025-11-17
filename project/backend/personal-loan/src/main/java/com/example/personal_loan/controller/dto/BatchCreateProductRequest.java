package com.example.personal_loan.controller.dto;

import java.util.List;

import com.example.personal_loan.dto.ProductDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchCreateProductRequest {
    private List<ProductDto> products;
}
