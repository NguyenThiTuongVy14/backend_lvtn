package com.example.test.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "t_driver_rating")
@Data
public class DriverRating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "driver_id")
    private Integer driverId;

    @Column(name = "average_rating")
    private Double averageRating;

    @Column(name = "total_ratings")
    private Integer totalRatings;
}