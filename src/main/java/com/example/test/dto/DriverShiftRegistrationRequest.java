package com.example.test.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DriverShiftRegistrationRequest {
    // Getters và Setters
    private Integer shiftId;
//    private Integer vehicleId;
    private LocalDate rotationDate;

}
