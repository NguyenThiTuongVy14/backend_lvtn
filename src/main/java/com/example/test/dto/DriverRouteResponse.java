package com.example.test.dto;

import com.example.test.entity.JobPosition;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DriverRouteResponse {
    private String message;
    private String licensePlate;
    private BigDecimal vehicleCapacity;
    private BigDecimal remainingCapacity;
    private List<JobPosition> optimizedPoints;
    public DriverRouteResponse(String message, String licensePlate, BigDecimal vehicleCapacity,
                               BigDecimal remainingCapacity, List<JobPosition> optimizedPoints) {
        this.message = message;
        this.licensePlate = licensePlate;
        this.vehicleCapacity = vehicleCapacity;
        this.remainingCapacity = remainingCapacity;
        this.optimizedPoints = optimizedPoints;
    }
}
