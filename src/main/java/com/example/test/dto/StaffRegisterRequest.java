package com.example.test.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StaffRegisterRequest {
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String personalId;
    private LocalDate dayOfBirth;
    private Integer gender;
    private Integer authorityId;
    private String avatar;
    private String cccdFront;
    private String cccdBack;
    private String licenseFront;
    private String licenseBack;
}
