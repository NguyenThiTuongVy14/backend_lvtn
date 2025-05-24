package com.example.test.dto;

import lombok.Data;

@Data
public class WebSocketMessage {
    private String type;
    private Integer id;
    private String status;
    private String message;
}