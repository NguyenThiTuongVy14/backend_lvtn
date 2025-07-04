package com.example.test.dto;

import lombok.Data;

@Data
public class NotificationRequest {
    private String fcm_token;
    private String title;
    private String body;

    // Constructors
    public NotificationRequest() {}

    public NotificationRequest(String token, String title, String body) {
        this.fcm_token = token;
        this.title = title;
        this.body = body;
    }

}
