package com.example.test.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "t_routes")
@Data
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "vehicle_id")
    private Integer vehicleId;

    @Column(name = "`index`")
    private Integer index;

    @Column(name = "rotation_id")
    private Integer rotationId;

    @Column(name = "arrival")
    private Integer arrival;


    @Column(name = "type")
    private String type;

}
