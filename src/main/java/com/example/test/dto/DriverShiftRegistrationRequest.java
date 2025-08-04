package com.example.test.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DriverShiftRegistrationRequest {
    // Getters và Setters
    private List<LocalDate> dates;
//    private Integer vehicleId;

}
