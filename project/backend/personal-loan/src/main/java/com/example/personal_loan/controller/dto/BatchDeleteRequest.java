package com.example.personal_loan.controller.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchDeleteRequest {
    private List<Long> ids;
    // getter/setter
}