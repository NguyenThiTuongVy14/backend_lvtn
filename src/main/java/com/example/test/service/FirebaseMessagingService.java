package com.example.test.service;


import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;

@Service
public class FirebaseMessagingService {

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
}
