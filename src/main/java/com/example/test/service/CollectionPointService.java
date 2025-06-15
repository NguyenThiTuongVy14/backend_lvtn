package com.example.test.service;

import com.example.test.dto.MarkCompletionRequest;
import com.example.test.dto.MarkCompletionResponse;
import com.example.test.entity.JobRotation;
import com.example.test.repository.JobRotationRepository;
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

    @Autowired
    public CollectionPointService(JobRotationRepository jobRotationRepository,
                                  SimpMessagingTemplate messagingTemplate) {
        this.jobRotationRepository = jobRotationRepository;
        this.messagingTemplate = messagingTemplate;
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
     * Đánh dấu hoàn thành công việc (chung cho cả collector và driver)
     */
    public MarkCompletionResponse markJobCompleted(MarkCompletionRequest request, String expectedRole) {
        MarkCompletionResponse response = new MarkCompletionResponse();

        try {
            // Tìm job rotation theo ID
            Optional<JobRotation> jobRotationOpt = jobRotationRepository.findById(request.getJobRotationId());

            if (!jobRotationOpt.isPresent()) {
                response.setSuccess(false);
                response.setMessage("Không tìm thấy công việc với ID: " + request.getJobRotationId());
                return response;
            }

            JobRotation jobRotation = jobRotationOpt.get();

            // Kiểm tra role
            if (!expectedRole.equals(jobRotation.getRole())) {
                response.setSuccess(false);
                response.setMessage("Chỉ " + expectedRole.toLowerCase() + " mới có thể thực hiện hành động này");
                return response;
            }

            // Kiểm tra trạng thái hiện tại
            if (!("ASSIGNED".equals(jobRotation.getStatus()))) {
                response.setSuccess(false);
                response.setMessage("Không đủ điều kiện");
                return response;
            }

            // Cập nhật trạng thái thành COMPLETED
            jobRotation.setStatus("COMPLETED");
            jobRotation.setUpdatedAt(LocalDateTime.now());

            // Lưu vào database
            JobRotation savedJobRotation = jobRotationRepository.save(jobRotation);

            // Gửi socket update
            updateJobStatus(savedJobRotation);

            // Tạo response thành công
            response.setSuccess(true);
            response.setMessage("Đánh dấu hoàn thành công việc thành công");
            response.setJobRotationId(savedJobRotation.getId());
            response.setStatus(savedJobRotation.getStatus());
            response.setUpdatedAt(savedJobRotation.getUpdatedAt());

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Lỗi hệ thống: " + e.getMessage());
        }

        return response;
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

            // Gửi update đến các app khác
            messagingTemplate.convertAndSend("/topic/job-status", statusUpdate);

        } catch (Exception e) {
            // Log error nhưng không ảnh hưởng đến business logic
            System.err.println("Error updating job status: " + e.getMessage());
        }
    }
}