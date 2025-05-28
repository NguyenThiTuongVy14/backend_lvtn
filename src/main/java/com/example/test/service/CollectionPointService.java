package com.example.test.service;

import com.example.test.dto.UpdateStatusRequest;
import com.example.test.dto.WebSocketMessage;
import com.example.test.entity.Authority;
import com.example.test.entity.JobPosition;
import com.example.test.entity.Staff;
import com.example.test.repository.AuthorityRepository;
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
    private final AuthorityRepository authorityRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public CollectionPointService(JobPositionRepository jobPositionRepository,
                                  StaffRepository staffRepository,
                                  AuthorityRepository authorityRepository,
                                  SimpMessagingTemplate messagingTemplate) {
        this.jobPositionRepository = jobPositionRepository;
        this.staffRepository = staffRepository;
        this.authorityRepository = authorityRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public JobPosition updateStatus(Integer id, UpdateStatusRequest request, String username) {
        // Tìm điểm thu gom
        JobPosition position = jobPositionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Collection point not found"));

        // Kiểm tra người dùng
        Staff staff = staffRepository.findByUserName(username);
        if (staff == null || staff.getAuthorityId() == null) {
            throw new IllegalArgumentException("Invalid user or authority");
        }

        // Lấy vai trò từ authority_id
        Authority authority = authorityRepository.findById(staff.getAuthorityId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid authority ID: " + staff.getAuthorityId()));
        if (!authority.getName().equals("COLLECTOR")) {
            throw new IllegalArgumentException("Only COLLECTOR can update collection point status");
        }

        // Kiểm tra trạng thái hợp lệ
        if (!isValidStatus(request.getStatus())) {
            throw new IllegalArgumentException("Invalid status. Must be PENDING or COMPLETED");
        }

        // Cập nhật trạng thái
        position.setStatus(request.getStatus());
        position.setUpdatedAt(LocalDateTime.now());
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
        // Gửi đến collector
        messagingTemplate.convertAndSend("/topic/collector/" + staff.getId(), wsMessage);

        return updatedPosition;
    }

    private boolean isValidStatus(String status) {
        return "PENDING".equals(status) || "COMPLETED".equals(status);
    }
}