package com.example.test.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Setter
@Getter
public class UpdateStatusRequest {
    private String status; // PENDING, ASSIGNED, COMPLETED, CANCEL, FAIL (cho JobRotation)
    private String collectorStatus; // PENDING, COMPLETED
    private BigDecimal currentLatitude;
    private BigDecimal currentLongitude;
    private String completionNotes;
    private List<String> images;

}