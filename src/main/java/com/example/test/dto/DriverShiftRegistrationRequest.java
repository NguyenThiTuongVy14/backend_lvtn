package com.example.test.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DriverShiftRegistrationRequest {
    // Getters và Setters
    private List<Integer> shiftId;
//    private Integer vehicleId;
    private LocalDate rotationDate;

}
