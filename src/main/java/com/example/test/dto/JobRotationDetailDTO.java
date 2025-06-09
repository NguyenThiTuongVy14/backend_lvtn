package com.example.test.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class JobRotationDetailDTO {
    private Integer id;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private StaffDTO staff;
    private JobPositionDTO position;
    private VehicleDTO vehicle;

    JobRotationDetailDTO(
            Integer id,
            String status,
            Timestamp createdAt,
            Timestamp updatedAt,
            Integer staffId,
            String staffName,
            Integer positionId,
            String jobPositionName,
            Integer vehicleId,
            String licensePlate,
            BigDecimal lat,
            BigDecimal lng,
            String address,
            String phone,
            String email
    ) {
        this.id = id;
        this.status = status;
        this.createdAt = createdAt != null ? createdAt.toLocalDateTime() : null;
        this.updatedAt = updatedAt != null ? updatedAt.toLocalDateTime() : null;
        this.staff = new StaffDTO(staffId, staffName,phone,email);
        this.position = new JobPositionDTO(positionId, jobPositionName,lat,lng,address);
        this.vehicle = vehicleId != null ? new VehicleDTO(vehicleId, licensePlate) : null;
    }
}