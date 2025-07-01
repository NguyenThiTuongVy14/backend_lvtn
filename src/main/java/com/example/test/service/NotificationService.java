package com.example.test.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    public void sendAdminAlert(String message) {
        simpMessagingTemplate.convertAndSend("/topic/admin-alerts", message);
        System.out.println("Đã gửi cảnh báo thiếu tài xế cho admin: " + message);
    }

}
