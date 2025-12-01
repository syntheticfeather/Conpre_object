package com.example.personal_loan.dto;

import java.time.LocalDateTime;

import com.example.personal_loan.enums.ProductStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListProductResponse {
    private Long productId;

    private String productName;
    private String description;
    private String usage;
    private ProductStatus status;
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
