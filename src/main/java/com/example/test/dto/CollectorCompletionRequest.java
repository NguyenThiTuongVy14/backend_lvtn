package com.example.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CollectorCompletionRequest {

    @JsonProperty("jobRotationId")
    private Integer jobRotationId;

    @JsonProperty("smallTrucksCount")
    private Integer smallTrucksCount;
}
