package com.example.test.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "request_become_staff")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "full_name")
    private String fullName;
    private String email;
    private String phone;
    private String address;
    @Column(name = "personal_id")
    private String personalId;
    @Column(name = "day_of_birth")
    private LocalDate dayOfBirth;
    private Integer gender;
    private Integer authorityId;
    private String avatar;
    @Column(name = "cccd_front")
    private String cccdFront;
    @Column(name = "cccd_back")
    private String cccdBack;
    @Column(name = "license_front")
    private String licenseFront;
    @Column(name = "license_back")
    private String licenseBack;
    private String status;
    @Column(name = "admin_note")
    private String adminNote;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
