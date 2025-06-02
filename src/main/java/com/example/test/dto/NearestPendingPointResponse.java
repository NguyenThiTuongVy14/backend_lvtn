package com.example.test.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Setter
@Getter
public class NearestPendingPointResponse {
    private Integer id;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Double distance;
    private Integer estimatedTravelTime;
    private String status;
    private String collectorStatus;

    public NearestPendingPointResponse(Integer id, String name, String address, BigDecimal latitude,
                                       BigDecimal longitude, Double distance, Integer estimatedTravelTime,
                                       String status, String collectorStatus) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = distance;
        this.estimatedTravelTime = estimatedTravelTime;
        this.status = status;
        this.collectorStatus = collectorStatus;
    }

}