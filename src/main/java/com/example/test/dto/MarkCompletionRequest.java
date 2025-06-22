package com.example.test.dto;

import lombok.Data;

@Data
public class MarkCompletionRequest {
    private Integer jobRotationId;
    private Integer tonnage;
}
