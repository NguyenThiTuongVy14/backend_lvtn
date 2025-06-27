package com.example.test.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class RoutePoint {
    private Integer positionId;
    private String pointName;
    private String address;
    private Integer smallTrucksCount; // Số xe rác nhỏ
    private BigDecimal wasteWeight;   // Tổng tấn rác (smallTrucksCount * 1 tấn)
}
