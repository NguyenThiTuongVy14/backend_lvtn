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

    public JobPositionDTO(Integer positionId, String jobPositionName, BigDecimal lat, BigDecimal lng, String address) {
        this.id = positionId;
        this.name = jobPositionName;
        this.lat = lat;
        this.lng = lng;
        this.address = address;
    }
}
