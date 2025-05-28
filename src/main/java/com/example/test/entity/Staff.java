package com.example.test.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Setter
@Getter
@Entity
@Table(name = "t_user")
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "staff_code")
    private String staffCode;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "day_of_birth")
    private LocalDate dayOfBirth;

    private String phone;

    private String address;

    @Column(name = "personal_id")
    private String personalId;

    private Integer gender;

    @Column(name = "start_day")
    private LocalDate startDay;

    @Column(name = "end_day")
    private LocalDate endDay;

    @Column(name = "user_name")
    private String userName;

    private String email;

    private String password;

    private String status;

    @Column(name = "authority_id")
    private Integer authorityId;
}