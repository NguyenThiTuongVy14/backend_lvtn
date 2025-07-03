package com.example.test.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MarkCompletionRequest {
    private Integer jobRotationId;
    private Integer tonnage;
}
