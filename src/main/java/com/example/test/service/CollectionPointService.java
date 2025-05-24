package com.example.test.service;

import com.example.test.dto.UpdateStatusRequest;
import com.example.test.dto.WebSocketMessage;
import com.example.test.entity.JobPosition;
import com.example.test.entity.Staff;
import com.example.test.repository.JobPositionRepository;
import com.example.test.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CollectionPointService {

    private final JobPositionRepository jobPositionRepository;
    private final StaffRepository staffRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public CollectionPointService(JobPositionRepository jobPositionRepository,
                                  StaffRepository staffRepository,
                                  SimpMessagingTemplate messagingTemplate) {
        this.jobPositionRepository = jobPositionRepository;
        this.staffRepository = staffRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public JobPosition updateStatus(Integer id, UpdateStatusRequest request, String username) {
        // Tìm điểm thu gom
        JobPosition position = jobPositionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Collection point not found"));

        // Kiểm tra người dùng
        Staff staff = staffRepository.findByUserName(username);
        if (staff == null) {
            throw new IllegalArgumentException("Invalid user");
        }

        // Kiểm tra vai trò COLLECTOR
        if (!staff.getRole().equals("COLLECTOR")) {
            throw new IllegalArgumentException("Only COLLECTOR can update collection point status");
        }

        // Cập nhật trạng thái
        position.setStatus(request.getStatus());
        position.setUpdatedAt(LocalDateTime.now());
        position.setUpdatedBy(staff);
        JobPosition updatedPosition = jobPositionRepository.save(position);

        // Gửi thông báo WebSocket
        WebSocketMessage wsMessage = new WebSocketMessage();
        wsMessage.setType("POINT_STATUS");
        wsMessage.setId(id);
        wsMessage.setStatus(request.getStatus());
        wsMessage.setMessage("Collection point #" + id + " updated to " + request.getStatus() + " by " + username);

        // Gửi đến tất cả người dùng quan tâm
        messagingTemplate.convertAndSend("/topic/collection-point-updates", wsMessage);
        // Gửi đến admin
        messagingTemplate.convertAndSend("/topic/admin", wsMessage);
        // Gửi đến collector (nếu cần)
        messagingTemplate.convertAndSend("/topic/collector/" + staff.getId(), wsMessage);

        return updatedPosition;
    }
}