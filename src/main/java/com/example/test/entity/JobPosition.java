package com.example.test.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_job_position")
@Data
public class JobPosition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String address;

    private BigDecimal lat;

    private BigDecimal lng;

    private String name;

    private String image;

    private String status;//ACTIVE; INACTIVE

    @Column(name = "required_tonnage")
    private BigDecimal requiredTonnage; // Tải trọng yêu cầu (tấn)

    @Column(name = "created_by")
    private Integer createdBy; // ID của admin tạo điểm làm việc

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}