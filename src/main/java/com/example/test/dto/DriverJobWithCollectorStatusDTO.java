package com.example.test.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class DriverJobWithCollectorStatusDTO {
    private Integer id;
    private String status;
    private LocalDate rotationDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Thông tin staff
    private Integer staffId;
    private String staffName;
    private String phone;
    private String email;

    // Thông tin vị trí công việc
    private Integer positionId;
    private String jobPositionName;
    private BigDecimal lat;
    private BigDecimal lng;
    private String address;

    // Thông tin xe
    private Integer vehicleId;
    private String licensePlate;

    // Thông tin ca làm việc
    private Integer shiftId;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;

    // ===== THÔNG TIN COLLECTOR STATUS =====
    private Boolean collectorCompleted;      // Collector đã hoàn thành chưa

    // Constructor nhận Object[] từ native query
    public DriverJobWithCollectorStatusDTO(Object[] result) {
        this.id = (Integer) result[0];
        this.status = (String) result[1];
        this.rotationDate = ((java.sql.Date) result[2]).toLocalDate();
        this.createdAt = ((java.sql.Timestamp) result[3]).toLocalDateTime();
        this.updatedAt = result[4] != null ? ((java.sql.Timestamp) result[4]).toLocalDateTime() : null;

        this.staffId = (Integer) result[5];
        this.staffName = (String) result[6];

        this.positionId = (Integer) result[7];
        this.jobPositionName = (String) result[8];

        this.vehicleId = (Integer) result[9];
        this.licensePlate = (String) result[10];

        this.lat = (BigDecimal) result[11];
        this.lng = (BigDecimal) result[12];
        this.address = (String) result[13];

        this.phone = (String) result[14];
        this.email = (String) result[15];

        this.shiftId = (Integer) result[16];
        this.shiftName = (String) result[17];
        this.startTime = result[18] != null ? ((java.sql.Time) result[18]).toLocalTime() : null;
        this.endTime = result[19] != null ? ((java.sql.Time) result[19]).toLocalTime() : null;

        // Chỉ trạng thái collector đã hoàn thành
        this.collectorCompleted = result[20] != null ? (Boolean) result[20] : false;
    }

    // Getter methods để kiểm tra trạng thái
    public boolean isCollectorCompleted() {
        return collectorCompleted != null && collectorCompleted;
    }

    public String getCollectorStatusDisplay() {
        return isCollectorCompleted() ? "Sẵn sàng thu gom" : "Chờ collector";
    }
}
