package com.example.test.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class JobRotationDetailDTO {
    private Integer id;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Date rotationDate;
    private StaffDTO staff;
    private JobPositionDTO position;
    private ShiftDTO shift;
    private VehicleDTO vehicle;

    // Constructor với kiểu dữ liệu chính xác
    public JobRotationDetailDTO(
            Integer id,                // jr.id
            String status,             // jr.status
            Date rotationDate,       // jr.rotation_date
            Timestamp createdAt,       // jr.created_at
            Timestamp updatedAt,       // jr.updated_at (có thể NULL)
            Integer staffId,           // s.id AS staff_id
            String staffName,          // s.full_name AS staff_name
            Integer positionId,        // jp.id AS position_id
            String jobPositionName,    // jp.name AS job_position_name
            Integer vehicleId,         // v.id AS vehicle_id (có thể NULL)
            String licensePlate,       // v.license_plate (có thể NULL)
            BigDecimal lat,            // jp.lat
            BigDecimal lng,            // jp.lng
            String address,            // jp.address
            String phone,              // s.phone
            String email,              // s.email
            Integer shiftId,           // sh.id AS shift_id
            String shiftName,          // sh.name AS shift_name
            Time startTime,          // sh.start_time (String hoặc Time?)
            Time endTime             // sh.end_time (String hoặc Time?)
    ) {
        this.id = id;
        this.status = status;
        this.rotationDate = rotationDate;
        this.createdAt = createdAt != null ? createdAt.toLocalDateTime() : null;
        this.updatedAt = updatedAt != null ? updatedAt.toLocalDateTime() : null;

        // Handle staff - không được null
        if (staffId != null) {
            this.staff = new StaffDTO();
            this.staff.setId(staffId);
            this.staff.setFullName(staffName);
            this.staff.setPhone(phone);
            this.staff.setEmail(email);
        }

        // Handle position - không được null
        if (positionId != null) {
            this.position = new JobPositionDTO();
            this.position.setId(positionId);
            this.position.setName(jobPositionName);
            this.position.setLat(lat);
            this.position.setLng(lng);
            this.position.setAddress(address);
        }

        // Handle shift - không được null
        if (shiftId != null) {
            this.shift = new ShiftDTO();
            this.shift.setId(shiftId);
            this.shift.setName(shiftName);
            this.shift.setStartTime(startTime);
            this.shift.setEndTime(endTime);
        }

        // Handle vehicle - có thể null
        if (vehicleId != null) {
            this.vehicle = new VehicleDTO();
            this.vehicle.setId(vehicleId);
            this.vehicle.setLicensePlate(licensePlate);
        }
    }
}