package com.example.test.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Setter
@Getter
public class OptimizedRouteResponse {
    private List<RoutePoint> points;
    private Double totalDistance;
    private Integer totalEstimatedTime;

    public OptimizedRouteResponse(List<RoutePoint> points, Double totalDistance, Integer totalEstimatedTime) {
        this.points = points;
        this.totalDistance = totalDistance;
        this.totalEstimatedTime = totalEstimatedTime;
    }

}

