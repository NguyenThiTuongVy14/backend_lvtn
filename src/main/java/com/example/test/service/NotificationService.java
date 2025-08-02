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
    private NotificationRepository notificationRepository;


    public List<Notification> getNotificationsByUserId(Integer userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    public void createNotification(Integer userId, String title, String content, String type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(false);
        notification.setCreatedAt(java.time.LocalDateTime.now());

        notificationRepository.save(notification);
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
