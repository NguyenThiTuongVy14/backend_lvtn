package com.example.test.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;


@Entity
@Data
@Table(name = "t_rotation_storeId")
public class RotationStoreId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer rotationIdCollector;
    private Integer rotationIdDriver;
    private Boolean  isCompleted =false ;
    private LocalDateTime createdAt ;
    private LocalDateTime updatedAt ;
}
