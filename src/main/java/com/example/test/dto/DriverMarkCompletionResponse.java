package com.example.test.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DriverMarkCompletionResponse {
    private boolean success;
    private String message;
    private Integer jobRotationId;
    private String status;
    private LocalDateTime updatedAt;
}
