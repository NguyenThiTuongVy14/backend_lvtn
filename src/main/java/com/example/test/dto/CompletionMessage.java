package com.example.test.dto;

import com.example.test.entity.JobPosition;
import lombok.Data;

@Data
public class CompletionMessage {
    private JobPosition jobPosition;
    private String status;

    public CompletionMessage(JobPosition jobPosition, String status) {
        this.jobPosition = jobPosition;
        this.status = status;
    }
}
