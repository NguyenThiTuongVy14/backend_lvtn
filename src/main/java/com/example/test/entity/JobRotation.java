package com.example.test.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Column(name = "job_position_id")
    private Integer jobPositionId; // ID vị trí công việc

    @Column(name = "vehicle_id")
    private Integer vehicleId; // ID xe

    @Column(name = "created_by")
    private Integer createdBy; // ID nhân viên tạo lịch phân công

    private String status; // Trạng thái: ROTATION_ACTIVE, ROTATION_INACTIVE

    @Column(name = "start_date")
    private LocalDateTime startDate; // Ngày bắt đầu

    @Column(name = "end_date")
    private LocalDateTime endDate; // Ngày kết thúc

    @Column(name = "created_at")
    private LocalDateTime createdAt; // Thời gian tạo

    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // Thời gian cập nhật
}