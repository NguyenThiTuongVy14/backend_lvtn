package com.example.test.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_collection_request")
@Data
public class CollectionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "address")
    private String address;

    @Column(name = "waste_type")
    private String wasteType; // ORGANIC, RECYCLABLE, HAZARDOUS

    @Column(name = "requested_time")
    private LocalDateTime requestedTime;

    @Column(name = "status")
    private String status; // PENDING, ASSIGNED, COMPLETED, CANCELLED

    @ManyToOne
    @JoinColumn(name = "collector_id")
    private Staff collector; // Người thu gom được phân công

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "updated_by")
    private Staff updatedBy; // Người cập nhật trạng thái
}