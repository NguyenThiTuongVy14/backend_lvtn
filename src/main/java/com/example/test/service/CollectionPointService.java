package com.example.test.service;

import com.example.test.dto.MarkCompletionRequest;
import com.example.test.dto.MarkCompletionResponse;
import com.example.test.entity.*;
import com.example.test.repository.AuthorityRepository;
import com.example.test.repository.JobPositionRepository;
import com.example.test.repository.JobRotationRepository;
import com.example.test.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CollectionPointService {

    private final JobPositionRepository jobPositionRepository;
    private final StaffRepository staffRepository;
    private final AuthorityRepository authorityRepository;
    private final JobRotationRepository jobRotationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public CollectionPointService(JobPositionRepository jobPositionRepository,
                                  StaffRepository staffRepository,
                                  AuthorityRepository authorityRepository,
                                  JobRotationRepository jobRotationRepository,
                                  SimpMessagingTemplate messagingTemplate) {
        this.jobPositionRepository = jobPositionRepository;
        this.staffRepository = staffRepository;
        this.authorityRepository = authorityRepository;
        this.jobRotationRepository = jobRotationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public MarkCompletionResponse markCollectionPointCompleted(MarkCompletionRequest request) {
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

            // Kiểm tra role phải là COLLECTOR
            if (!"COLLECTOR".equals(jobRotation.getRole())) {
                response.setSuccess(false);
                response.setMessage("Chỉ collector mới có thể đánh dấu hoàn thành điểm thu gom");
                return response;
            }

            // Kiểm tra trạng thái hiện tại
            if ("COMPLETED".equals(jobRotation.getStatus())) {
                response.setSuccess(false);
                response.setMessage("Điểm thu gom đã được đánh dấu hoàn thành trước đó");
                return response;
            }

            // Cập nhật trạng thái thành COMPLETED
            jobRotation.setStatus("COMPLETED");
            jobRotation.setUpdatedAt(LocalDateTime.now());


            // Lưu vào database
            JobRotation savedJobRotation = jobRotationRepository.save(jobRotation);

            // **THÊM SOCKET UPDATE**
            // Cập nhật status cho app tài xế qua WebSocket
            updateCollectionPointStatus(savedJobRotation);

            // Tạo response thành công
            response.setSuccess(true);
            response.setMessage("Đánh dấu hoàn thành điểm thu gom thành công");
            response.setJobRotationId(savedJobRotation.getId());
            response.setStatus(savedJobRotation.getStatus());
            response.setUpdatedAt(savedJobRotation.getUpdatedAt());

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Lỗi hệ thống: " + e.getMessage());
        }

        return response;
    }

    // Phương thức lấy danh sách công việc của collector
    public List<JobRotation> getCollectorJobs(Integer staffId, Date rotationDate) {
        return jobRotationRepository.findByStaffIdAndRoleAndRotationDate(
                staffId, "COLLECTOR", rotationDate);
    }

    // Phương thức lấy công việc đang pending của collector
    public List<JobRotation> getPendingCollectorJobs(Integer staffId) {
        return jobRotationRepository.findByStaffIdAndRoleAndStatus(
                staffId, "COLLECTOR", "PENDING");
    }


    private void updateCollectionPointStatus(JobRotation jobRotation) {
        try {
            // Tạo data update
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("jobRotationId", jobRotation.getId());
            statusUpdate.put("status", jobRotation.getStatus());
            statusUpdate.put("updatedAt", jobRotation.getUpdatedAt());

            // Gửi update đến app tài xế
            messagingTemplate.convertAndSend("/topic/collection-status", statusUpdate);

        } catch (Exception e) {
            // Log error nhưng không ảnh hưởng đến business logic
            System.err.println("Error updating collection point status: " + e.getMessage());
        }
    }
}