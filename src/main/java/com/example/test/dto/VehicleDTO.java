package com.example.test.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDTO {
    private Integer id;
    private String licensePlate;
    private BigDecimal tonnage;
    private String status;

    public VehicleDTO(Integer vehicleId, String licensePlate) {
        this.id = vehicleId;
        this.licensePlate = licensePlate;
    }
}