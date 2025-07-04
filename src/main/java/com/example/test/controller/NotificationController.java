package com.example.test.controller;

import com.example.test.config.JwtTokenProvider;
import com.example.test.dto.NotificationRequest;
import com.example.test.entity.Notification;
import com.example.test.entity.Staff;
import com.example.test.repository.StaffRepository;
import com.example.test.service.FirebaseMessagingService;
import com.example.test.service.NotificationService;
    import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private FirebaseMessagingService fcmService;
    private final NotificationService notificationService;
    private JwtTokenProvider jwtTokenProvider;
    private final StaffRepository staffRepository;

    public NotificationController(NotificationService notificationService, StaffRepository staffRepository) {
        this.notificationService = notificationService;
        this.staffRepository = staffRepository;
    }

    // ✅ API gửi thông báo đến thiết bị thông qua FCM
    @PostMapping("/notifications")
    public String sendToToken(@RequestBody NotificationRequest notificationRequest) {
        fcmService.sendNotificationToToken(
                notificationRequest.getFcm_token(),
                notificationRequest.getTitle(),
                notificationRequest.getBody()
        );
        return "✅ Đã gửi thông báo tới thiết bị. " + notificationRequest.getFcm_token();
    }

    // ✅ API lấy danh sách thông báo của user từ JWT token
    @GetMapping
    public List<Notification> getUserNotifications() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Staff staff = staffRepository.findByUserName(username);
        System.out.println("id: " + staff.getId());
        return notificationService.getNotificationsByUserId(staff.getId());
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable Integer id) {
        boolean updated = notificationService.markAsRead(id);
        if (updated) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Đã đánh dấu là đã đọc.");
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Không tìm thấy thông báo có ID = " + id);
            return ResponseEntity.status(400).body(error);
        }
    }



}
