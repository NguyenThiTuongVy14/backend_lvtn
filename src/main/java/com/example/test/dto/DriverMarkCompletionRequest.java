package com.example.test.dto;

import lombok.Data;

@Data
public class DriverMarkCompletionRequest {
    private Integer jobRotationId;
    private Integer jobPositionId;
    private Integer shiftId;
}
