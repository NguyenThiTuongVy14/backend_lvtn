package com.example.test.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
public class WebSocketMessage {
    private String type; // POINT_STATUS, COLLECTOR_STATUS_CHANGED
    private Integer id;
    private String status;
    private String collectorStatus;
    private String message;
    private LocalDateTime timestamp;
    // Constructor mới
    public WebSocketMessage(String type, Integer id, String status, String collectorStatus, String message, LocalDateTime timestamp) {
        this.type = type;
        this.id = id;
        this.status = status;
        this.collectorStatus = collectorStatus;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Constructor mặc định (giữ lại để tương thích)
    public WebSocketMessage() {
    }
}