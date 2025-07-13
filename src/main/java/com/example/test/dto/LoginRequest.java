package com.example.test.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginRequest {
    private String username;
    private String password;
    private String fcmToken;
    private Integer role;

}