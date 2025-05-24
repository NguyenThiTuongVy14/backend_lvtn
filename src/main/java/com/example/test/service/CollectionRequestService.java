package com.example.test.service;

import com.example.test.dto.UpdateStatusRequest;
import com.example.test.dto.WebSocketMessage;
import com.example.test.entity.CollectionRequest;
import com.example.test.entity.Staff;
import com.example.test.repository.CollectionRequestRepository;
import com.example.test.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CollectionRequestService {

    private final CollectionRequestRepository collectionRequestRepository;
    private final StaffRepository staffRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public CollectionRequestService(CollectionRequestRepository collectionRequestRepository,
                                    StaffRepository staffRepository,
                                    SimpMessagingTemplate messagingTemplate) {
        this.collectionRequestRepository = collectionRequestRepository;
        this.staffRepository = staffRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public CollectionRequest createAndAssignCollectionRequest(CollectionRequest request) {
        // Kiểm tra dữ liệu đầu vào
        if (request.getAddress() == null || request.getWasteType() == null || request.getRequestedTime() == null) {
            throw new IllegalArgumentException("Address, waste type, and requested time are required");
        }
        if (!isValidWasteType(request.getWasteType())) {
            throw new IllegalArgumentException("Invalid waste type. Must be ORGANIC, RECYCLABLE, or HAZARDOUS");
        }
        if (request.getCollector() == null || !staffRepository.existsById(request.getCollector().getId())) {
            throw new IllegalArgumentException("Invalid collector");
        }

        // Kiểm tra vai trò COLLECTOR
        Staff collector = staffRepository.findById(request.getCollector().getId())
                .orElseThrow(() -> new IllegalArgumentException("Collector not found"));
        if (!collector.getRole().equals("COLLECTOR")) {
            throw new IllegalArgumentException("Selected staff is not a COLLECTOR");
        }

        // Thiết lập trạng thái và thời gian
        request.setStatus("ASSIGNED");
        request.setCreatedAt(LocalDateTime.now());
        CollectionRequest savedRequest = collectionRequestRepository.save(request);

        // Gửi thông báo WebSocket đến COLLECTOR và ADMIN
        WebSocketMessage wsMessage = new WebSocketMessage();
        wsMessage.setType("REQUEST_STATUS");
        wsMessage.setId(savedRequest.getId());
        wsMessage.setStatus(savedRequest.getStatus());
        wsMessage.setMessage("New collection request #" + savedRequest.getId() + " assigned to collector ID " + savedRequest.getCollector().getId());

        messagingTemplate.convertAndSend("/topic/collector/" + savedRequest.getCollector().getId(), wsMessage);
        messagingTemplate.convertAndSend("/topic/admin", wsMessage);

        return savedRequest;
    }

    @Transactional
    public CollectionRequest updateCollectionRequestStatus(Integer id, UpdateStatusRequest statusRequest, Staff updater) {
        // Tìm yêu cầu thu gom
        CollectionRequest collectionRequest = collectionRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Collection request not found"));

        // Kiểm tra quyền cập nhật
        if (!collectionRequest.getCollector().getId().equals(updater.getId())) {
            throw new IllegalArgumentException("You can only update your own assigned requests");
        }

        // Kiểm tra trạng thái hợp lệ
        if (!isValidStatus(statusRequest.getStatus())) {
            throw new IllegalArgumentException("Invalid status. Must be COMPLETED or CANCELLED");
        }

        // Cập nhật trạng thái
        collectionRequest.setStatus(statusRequest.getStatus());
        collectionRequest.setUpdatedAt(LocalDateTime.now());
        collectionRequest.setUpdatedBy(updater);
        CollectionRequest updatedRequest = collectionRequestRepository.save(collectionRequest);

        // Gửi thông báo WebSocket
        WebSocketMessage wsMessage = new WebSocketMessage();
        wsMessage.setType("REQUEST_STATUS");
        wsMessage.setId(updatedRequest.getId());
        wsMessage.setStatus(updatedRequest.getStatus());
        wsMessage.setMessage("Collection request #" + updatedRequest.getId() + " updated to " + updatedRequest.getStatus());

        // Gửi đến tất cả người dùng quan tâm
        messagingTemplate.convertAndSend("/topic/collection-request-updates", wsMessage);
        // Gửi đến admin
        messagingTemplate.convertAndSend("/topic/admin", wsMessage);
        // Gửi đến collector
        messagingTemplate.convertAndSend("/topic/collector/" + updatedRequest.getCollector().getId(), wsMessage);

        return updatedRequest;
    }

    private boolean isValidWasteType(String wasteType) {
        return wasteType != null && (wasteType.equals("ORGANIC") || wasteType.equals("RECYCLABLE") || wasteType.equals("HAZARDOUS"));
    }

    private boolean isValidStatus(String status) {
        return status != null && (status.equals("COMPLETED") || status.equals("CANCELLED"));
    }
}