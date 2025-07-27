package com.example.test.dto;

public record ResponseMessage(String message, Object data) {
    ResponseMessage(String message) {
        this(message, null);
    }
}