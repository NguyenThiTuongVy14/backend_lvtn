package com.example.test.service;

import com.example.test.dto.MarkCompletionRequest;
import com.example.test.dto.MarkCompletionResponse;
import com.example.test.entity.JobRotation;
import com.example.test.entity.RotationStoreId;
import com.example.test.repository.JobRotationRepository;
import com.example.test.repository.RotationStoreIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CollectionPointService {

    private final JobRotationRepository jobRotationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RotationStoreIdRepository rotationStoreIdRepository;

    @Autowired
    public CollectionPointService(JobRotationRepository jobRotationRepository,
                                  SimpMessagingTemplate messagingTemplate,RotationStoreIdRepository rotationStoreIdRepository) {
        this.jobRotationRepository = jobRotationRepository;
        this.messagingTemplate = messagingTemplate;
        this.rotationStoreIdRepository=rotationStoreIdRepository;
    }

    /**
     * Lấy danh sách lịch phân công của collector
     */
    public List<JobRotation> getCollectorSchedules(Integer staffId) {
        return jobRotationRepository.findAllCollectorJobs(staffId);
    }

    /**
     * Lấy danh sách lịch phân công của driver
     */
    public List<JobRotation> getDriverSchedules(Integer staffId) {
        return jobRotationRepository.findAllDriverJobs(staffId);
    }
    /**
     * Gửi thông báo cập nhật trạng thái công việc qua WebSocket
     */
    private void updateJobStatus(JobRotation jobRotation) {
        try {
            // Tạo data update
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("jobRotationId", jobRotation.getId());
            statusUpdate.put("status", jobRotation.getStatus());
            statusUpdate.put("role", jobRotation.getRole());
            statusUpdate.put("rotationDate", jobRotation.getRotationDate());
            statusUpdate.put("updatedAt", jobRotation.getUpdatedAt());

            System.out.println("Sending to WebSocket: " + statusUpdate);
            messagingTemplate.convertAndSend("/topic/job-status-"+jobRotation.getJobPositionId(), statusUpdate);

        } catch (Exception e) {
            // Log error nhưng không ảnh hưởng đến business logic
            System.err.println("Error updating job status: " + e.getMessage());
        }
    }
}