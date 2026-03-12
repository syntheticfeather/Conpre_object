package com.example.personal_loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToBlackListRequest {

    @Schema(description = "用户ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @Schema(
        description = "黑名单等级（1: 轻度, 2: 中度, 3: 重度）",
        example = "2",
        allowableValues = {"1", "2", "3"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer blackLevel;
}
