package com.example.test.service;

import com.example.test.dto.*;
import com.example.test.entity.*;
import com.example.test.repository.JobPositionRepository;
import com.example.test.repository.JobRotationRepository;
import com.example.test.repository.StaffRepository;
import com.example.test.repository.VehicleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobRotationService {
    private final JobRotationRepository jobRotationRepository;

    public List<JobRotationDetailDTO> getMyJobRotationsByDate(String userName, LocalDate date) {
        System.out.println("Getting rotations for user: " + userName + " on date: " + date);
        return jobRotationRepository.findByUserNameAndDate(userName, date);
    }
    /**
     * Lấy danh sách công việc của driver với thông tin collector status theo ngày
     */
    public List<DriverJobWithCollectorStatusDTO> getDriverJobsWithCollectorStatusByDate(
            String username, LocalDate date) {

        return jobRotationRepository.findDriverJobsWithCollectorStatusByDate(username, date);
    }
    @Scheduled(cron = "0 0 * * * *") // mỗi giờ đầu tiên: 0:00, 1:00, 2:00,...
    public void autoFailExpiredJobs() {
        int affected = jobRotationRepository.updateLateJobRotations();
        System.out.println("Updated " + affected + " job rotations to LATE status.");
    }


    public MarkCompletionResponse markJobCompleted(MarkCompletionRequest request, String expectedRole) {
        MarkCompletionResponse response = new MarkCompletionResponse();
        try {
            if (request.getJobRotationId() == null) {
                MarkCompletionResponse errorResponse = new MarkCompletionResponse();
                errorResponse.setSuccess(false);
                errorResponse.setMessage("Job rotation ID không được để trống");
                return response;
            }
            Optional<JobRotation> jobRotationOpt = jobRotationRepository.findById(request.getJobRotationId());

            if (!jobRotationOpt.isPresent()) {
                response.setSuccess(false);
                response.setMessage("Không tìm thấy công việc với ID: " + request.getJobRotationId());
                return response;
            }
            JobRotation jobRotation = jobRotationOpt.get();
            if (!expectedRole.equals(jobRotation.getRole())) {
                response.setSuccess(false);
                response.setMessage("Chỉ " + expectedRole.toLowerCase() + " mới có thể thực hiện hành động này");
                return response;
            }

            if (!("ASSIGNED".equals(jobRotation.getStatus()))) {
                response.setSuccess(false);
                response.setMessage("Không đủ điều kiện");
                return response;
            }

            jobRotation.setStatus("COMPLETED");
            jobRotation.setUpdatedAt(LocalDateTime.now());
            jobRotationRepository.save(jobRotation);
            response.setSuccess(true);
            response.setMessage("Đánh dấu hoàn thành công việc thành công");
            findDriverAndAssigned(request.getTonnage());
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Lỗi hệ thống: " + e.getMessage());
        }

        return response;
    }
    private void findDriverAndAssigned(Integer tonnage){
        Optional<JobRotation> jobRotationOpt = jobRotationRepository.findDriver(tonnage);
        JobRotation jobRotation = jobRotationOpt.get();
        jobRotation.setStatus("ASSIGNED");
        jobRotation.setUpdatedAt(LocalDateTime.now());
        jobRotationRepository.save(jobRotation);

        //Socket
    }
    private void updateJobStatus(JobRotation jobRotation) {
        try {
            // Tạo data update
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("jobRotationId", jobRotation.getId());
            statusUpdate.put("status", jobRotation.getStatus());
            statusUpdate.put("role", jobRotation.getRole());
            statusUpdate.put("rotationDate", jobRotation.getRotationDate());
            statusUpdate.put("updatedAt", jobRotation.getUpdatedAt());


        } catch (Exception e) {
            // Log error nhưng không ảnh hưởng đến business logic
            System.err.println("Error updating job status: " + e.getMessage());
        }
    }

}