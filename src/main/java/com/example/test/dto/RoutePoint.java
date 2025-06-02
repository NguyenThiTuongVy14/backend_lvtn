package com.example.test.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class RoutePoint {
    private Integer id;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String status;
    private String collectorStatus;
    private Double distanceToNext;
    private Integer estimatedTimeToNext;

    public RoutePoint(Integer id, String name, String address, BigDecimal latitude, BigDecimal longitude,
                      String status, String collectorStatus, Double distanceToNext, Integer estimatedTimeToNext) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.collectorStatus = collectorStatus;
        this.distanceToNext = distanceToNext;
        this.estimatedTimeToNext = estimatedTimeToNext;
    }

}
