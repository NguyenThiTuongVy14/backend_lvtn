package com.example.test.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobUpdateDTO {
    private Integer jobRotationId;
    private String fullName;
//    private Vehicle vehicle;
    private String status;
    private String shift;
    private int smallTrucksCount;
    private BigDecimal tonnage;
}
