package com.example.test.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class JobDetailResponse {
    private Integer jobRotationId;
    private String status;
    private String role;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Staff info
    private Integer staffId;
    private String staffName;

    // Position info
    private Integer positionId;
    private String jobPositionName;
    private String address;
    private Double lat;
    private Double lng;

    // Vehicle info
    private Integer vehicleId;
    private String licensePlate;
    private String capacity;
}