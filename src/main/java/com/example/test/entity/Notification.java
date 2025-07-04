package com.example.test.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private String type;

    @Column(name = "is_read")
    private Boolean isRead;

    @Column(name = "user_id")
    private Integer userId;
}
