package com.example.test.service;


import com.example.test.entity.FCMToken;
import com.example.test.repository.FcmRepository;
import com.google.firebase.messaging.*;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FirebaseMessagingService {
    @Autowired
    private final FcmRepository fcmRepository;
    private final NotificationService notificationService;
    public FirebaseMessagingService(FcmRepository fcmRepository, NotificationService notificationService) {
        this.fcmRepository = fcmRepository;
        this.notificationService = notificationService;
    }

    public void sendNotificationToToken(String fcmToken, String title, String body) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification)
                    .putData("click_action", "FLUTTER_NOTIFICATION_CLICK") // cho Flutter/Expo xử lý khi nhấn
                    .putData("custom_key", "value")
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("✅ Notification sent successfully: " + response);

        } catch (FirebaseMessagingException e) {
            System.err.println("❌ Error sending FCM: " + e.getMessage());
        }
    }
    public void sendToAllTokensByStaffId(Integer staffId, String title, String body, String type) {
        notificationService.createNotification(staffId, title, body, type);
        List<FCMToken> tokens = fcmRepository.findByStaffId(staffId);
        for (FCMToken token : tokens) {
            sendNotificationToToken(token.getToken(), title, body);
        }
    }
}
