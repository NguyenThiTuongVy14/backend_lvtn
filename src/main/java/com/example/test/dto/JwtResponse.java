package com.example.test.dto;

import lombok.Data;
import lombok.Getter;

@Data
public class JwtResponse {
    private  String token;
    private Integer role;

    public JwtResponse() {

    }

}