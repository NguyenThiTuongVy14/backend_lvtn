package com.example.test.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "t_job_rotation")
public class JobRotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "position_id")
    private JobPosition jobPosition;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Staff staff;

    private String status;

    private LocalDate fromDay;
    private LocalDate toDay;

    public LocalDate getToDay() {
        return toDay;
    }

    public void setToDay(LocalDate toDay) {
        this.toDay = toDay;
    }

    public LocalDate getFromDay() {
        return fromDay;
    }

    public void setFromDay(LocalDate fromDay) {
        this.fromDay = fromDay;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public JobPosition getJobPosition() {
        return jobPosition;
    }

    public void setJobPosition(JobPosition jobPosition) {
        this.jobPosition = jobPosition;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
