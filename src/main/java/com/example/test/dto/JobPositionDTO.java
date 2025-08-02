package com.example.test.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobPositionDTO {
    private Integer id;
    private String name;
    private String address;
    private String status;
    private BigDecimal lat;
    private BigDecimal lng;
    private LocalDateTime createdAt;
    private Integer index;
    private Integer arrival;
    private String type;


}
