package com.example.test.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_rotation_log")
@Data
public class RotationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "shift_id")
    private Integer shiftId;

    @Column(name = "vehicle_id")
    private Integer vehicleId;

    private String status; // REQUEST, ASSIGNED, REJECTED

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "rotation_date")
    private LocalDate rotationDate;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}