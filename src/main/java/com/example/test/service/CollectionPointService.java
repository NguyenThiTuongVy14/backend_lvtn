package com.example.test.service;

import com.example.test.dto.*;
import com.example.test.entity.*;
import com.example.test.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    // ===========================================
    // COLLECTOR METHODS
    // ===========================================

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

            // Gửi socket update
            updateJobStatus(savedJobRotation);

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

    public List<JobRotation> getPendingCollectorJobs(Integer staffId) {
        return jobRotationRepository.findByStaffIdAndRoleAndStatusOrderByStartDateAsc(
                staffId, "COLLECTOR", "PENDING");
    }

    public List<JobDetailResponse> getCollectorJobsWithDetails(Integer staffId, String status) {
        List<Object[]> results = jobRotationRepository.findCollectorJobsWithDetails(staffId, status);

        return results.stream().map(this::mapToJobDetailResponse).collect(Collectors.toList());
    }

    public List<JobRotation> getCollectorJobsByDate(Integer staffId, Date workDate) {
        return jobRotationRepository.findCollectorJobsByDate(staffId, workDate);
    }

    public List<JobRotation> getAllCollectorJobs(Integer staffId) {
        return jobRotationRepository.findAllCollectorJobs(staffId);
    }

    // ===========================================
    // DRIVER METHODS
    // ===========================================

    public DriverMarkCompletionResponse driverMarkCompleted(DriverMarkCompletionRequest request) {
        DriverMarkCompletionResponse response = new DriverMarkCompletionResponse();

        try {
            // Tìm job rotation theo ID
            Optional<JobRotation> jobRotationOpt = jobRotationRepository.findById(request.getJobRotationId());

            if (!jobRotationOpt.isPresent()) {
                response.setSuccess(false);
                response.setMessage("Không tìm thấy công việc với ID: " + request.getJobRotationId());
                return response;
            }

            JobRotation jobRotation = jobRotationOpt.get();

            // Kiểm tra role phải là DRIVER
            if (!"DRIVER".equals(jobRotation.getRole())) {
                response.setSuccess(false);
                response.setMessage("Chỉ driver mới có thể thực hiện hành động này");
                return response;
            }

            // Kiểm tra trạng thái hiện tại
            if ("COMPLETED".equals(jobRotation.getStatus())) {
                response.setSuccess(false);
                response.setMessage("Công việc đã được đánh dấu hoàn thành trước đó");
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

    public List<JobRotation> getPendingDriverJobs(Integer staffId) {
        return jobRotationRepository.findByStaffIdAndRoleAndStatusOrderByStartDateAsc(
                staffId, "DRIVER", "PENDING");
    }

    public List<JobDetailResponse> getDriverJobsWithDetails(Integer staffId, String status) {
        List<Object[]> results = jobRotationRepository.findDriverJobsWithDetails(staffId, status);

        return results.stream().map(this::mapToJobDetailResponse).collect(Collectors.toList());
    }

    public List<JobRotation> getDriverJobsByDate(Integer staffId, Date workDate) {
        return jobRotationRepository.findDriverJobsByDate(staffId, workDate);
    }

    public List<JobRotation> getAllDriverJobs(Integer staffId) {
        return jobRotationRepository.findAllDriverJobs(staffId);
    }

    // ===========================================
    // COMMON METHODS
    // ===========================================

    public List<JobRotation> getActiveJobs(Integer staffId, String role) {
        return jobRotationRepository.findActiveJobs(staffId, role);
    }

    public List<JobRotation> getUpcomingJobs(Integer staffId, String role) {
        return jobRotationRepository.findUpcomingJobs(staffId, role);
    }

    public List<JobRotation> getOverdueJobs(Integer staffId, String role) {
        return jobRotationRepository.findOverdueJobs(staffId, role);
    }

    public List<JobRotation> getJobsByDateRange(Integer staffId, String role, Date startDate, Date endDate) {
        return jobRotationRepository.findJobsByDateRange(staffId, role, startDate, endDate);
    }

    public List<Object[]> getJobStatistics(Integer staffId) {
        return jobRotationRepository.countJobsByStatusAndRole(staffId);
    }

    // ===========================================
    // PRIVATE HELPER METHODS
    // ===========================================

    private void updateJobStatus(JobRotation jobRotation) {
        try {
            // Tạo data update
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("jobRotationId", jobRotation.getId());
            statusUpdate.put("status", jobRotation.getStatus());
            statusUpdate.put("role", jobRotation.getRole());
            statusUpdate.put("updatedAt", jobRotation.getUpdatedAt());

            // Gửi update đến các app khác
            messagingTemplate.convertAndSend("/topic/job-status", statusUpdate);

        } catch (Exception e) {
            // Log error nhưng không ảnh hưởng đến business logic
            System.err.println("Error updating job status: " + e.getMessage());
        }
    }

    private JobDetailResponse mapToJobDetailResponse(Object[] result) {
        JobDetailResponse response = new JobDetailResponse();

        response.setJobRotationId((Integer) result[0]);
        response.setStatus((String) result[1]);
        response.setRole((String) result[2]);
        response.setStartDate((LocalDateTime) result[3]);
        response.setEndDate((LocalDateTime) result[4]);
        response.setCreatedAt((LocalDateTime) result[5]);
        response.setUpdatedAt((LocalDateTime) result[6]);

        response.setStaffId((Integer) result[7]);
        response.setStaffName((String) result[8]);

        response.setPositionId((Integer) result[9]);
        response.setJobPositionName((String) result[10]);
        response.setAddress((String) result[11]);
        response.setLat((Double) result[12]);
        response.setLng((Double) result[13]);

        response.setVehicleId((Integer) result[14]);
        response.setLicensePlate((String) result[15]);
        response.setCapacity((String) result[16]);

        return response;
    }
}