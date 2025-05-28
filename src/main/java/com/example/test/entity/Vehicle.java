package com.example.test.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "t_vehicle")
@Data
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "license_plate")
    private String licensePlate; // Biển số xe

    @Column(name = "tonnage")
    private BigDecimal tonnage; // Tải trọng xe (tấn)

    @Column(name = "status")
    private String status; // Trạng thái: AVAILABLE, IN_USE
}
