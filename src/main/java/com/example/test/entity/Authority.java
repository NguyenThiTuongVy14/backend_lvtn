package com.example.test.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "t_authority")
@Data
public class Authority {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private String description;
}