package com.example.test.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_job_rotation")
@Data
public class JobRotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "vehicle_id")
    private Integer vehicleId;

    @Column(name = "job_position_id")
    private Integer jobPositionId;

    @Column(name = "shift_id")
    private Integer shiftId;

    @Column(name = "collection_request_id")
    private Integer collectionRequestId;

    @Column(name = "rotation_date")
    private LocalDate rotationDate;

    @Column(name = "role")
    private String role;

    @Column(name = "status")
    private String status;

    @Column(name = "small_trucks_count")
    private Integer smallTrucksCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "tonnage")
    private BigDecimal tonnage;

    @JoinColumn(name = "vehicle_id")
    private Integer vehicle;
}