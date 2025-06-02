package com.example.test.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_collection_point_status")
@Data
public class CollectionPointStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "job_position_id")
    private Integer jobPositionId;

    @Column(name = "staff_id")
    private Integer staffId; // ID của COLLECTOR

    private String collectorStatus; // PENDING, COMPLETED

    private String completionNotes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}