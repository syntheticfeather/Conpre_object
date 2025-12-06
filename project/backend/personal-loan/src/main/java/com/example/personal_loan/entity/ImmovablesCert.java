package com.example.personal_loan.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImmovablesCert {

    private Integer immovableCertId;

    // 房产证明
    private String propertyCertPath;

    //车产证明
    private String carCertPath;

    //总资产
    private Integer totalValue;
}
