package com.example.test.dto;

import lombok.Data;

@Data
public class StaffRegisterRequest {
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String personalId;
    private String dayOfBirth;
    private int gender;
    private int authorityId;
}
