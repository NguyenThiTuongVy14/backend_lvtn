package com.example.test.service;

import com.example.test.entity.Notification;
import com.example.test.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    private NotificationRepository notificationRepository;

    public void sendAdminAlert(String message) {
        simpMessagingTemplate.convertAndSend("/topic/admin-alerts", message);
        System.out.println("Đã gửi cảnh báo thiếu tài xế cho admin: " + message);
    }

    public List<Notification> getNotificationsByUserId(Integer userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public boolean markAsRead(Integer id) {
        Optional<Notification> optional = notificationRepository.findById(id);
        if (optional.isPresent()) {
            Notification notification = optional.get();
            notification.setIsRead(true);
            notificationRepository.save(notification);
            return true;
        }
        return false;
    }

}
