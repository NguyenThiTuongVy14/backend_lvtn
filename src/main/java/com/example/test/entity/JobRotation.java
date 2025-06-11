package com.example.test.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Setter
@Getter
@Entity
@Table(name = "t_job_rotation")
@Data
public class JobRotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "staff_id")
    private Integer staffId; // ID nhân viên được phân công

    @Column(name = "position_id")
    private Integer jobPositionId; // ID địa điểm công việc

    @Column(name = "vehicle_id")
    private Integer vehicleId; // ID xe

//    @Column(name = "shift_id")
//    private int shiftId; //Ca làm

//    @Column(name = "rotation_date")
//    private Date rotationDate;
    private String role; //COLLECTOR, DRIVER

    private String status; //COMPLETED, PENDING, FAIL

    @Column(name = "created_by")
    private Integer createdBy; // ID admin tạo lịch phân công

    @Column(name = "created_at")
    private LocalDateTime createdAt; // Thời gian tạo

    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // Thời gian cập nhật
}