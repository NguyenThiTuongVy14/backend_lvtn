package com.example.test.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Gửi tin nhắn tới một topic
     * @param destination ví dụ "/topic/notification"
     * @param message nội dung tin nhắn
     */
    public void sendToTopic(String destination, Object message) {
        messagingTemplate.convertAndSend(destination, message);
    }

    /**
     * Gửi tin nhắn riêng cho một user
     * @param username tên người dùng
     * @param destination ví dụ "/queue/private"
     * @param message nội dung tin nhắn
     */
    public void sendToUser(String username, String destination, Object message) {
        messagingTemplate.convertAndSendToUser(username, destination, message);
    }
}
