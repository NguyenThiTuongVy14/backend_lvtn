package com.example.test.dto;

import com.example.test.entity.JobPosition;
import com.example.test.entity.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DriverRouteResponse {
    private Integer jobRotationId;
//    private Vehicle vehicle;
    private JobPositionDTO position;
    private String status;

}
