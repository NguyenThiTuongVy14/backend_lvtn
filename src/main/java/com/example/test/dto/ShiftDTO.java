package com.example.test.dto;

import lombok.Data;

import java.sql.Time;
import java.time.LocalTime;


@Data
public class ShiftDTO {
    private Integer id;
    private String name;
    private Time startTime;
    private Time endTime;

    public ShiftDTO(Integer id, String name, Time start_time, Time end_time) {
        this.id = id;
        this.name = name;
        this.startTime = start_time;
        this.endTime = end_time;
    }

    public ShiftDTO() {

    }
}
