package com.example.test.controller;


import com.example.test.dto.NotificationRequest;
import com.example.test.service.FirebaseMessagingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fcm")
public class NotificationController {

    @Autowired
    private FirebaseMessagingService fcmService;

    @PostMapping("/send")
    public String sendToToken(@RequestBody NotificationRequest notificationRequest) {
        fcmService.sendNotificationToToken(
                notificationRequest.getFcm_token(),
                notificationRequest.getTitle(),
                notificationRequest.getBody()
        );
        return "✅ Đã gửi thông báo tới thiết bị. "+notificationRequest.getFcm_token();
    }
}
