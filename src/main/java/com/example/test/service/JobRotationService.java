package com.example.test.service;

import com.example.test.dto.*;
import com.example.test.entity.*;
import com.example.test.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobRotationService {
    private final JobRotationRepository jobRotationRepository;
    private final VehicleRepository vehicleRepository;
    private final RotationLogRepository rotationLogRepository;
    private final DriverRatingRepository driverRatingRepository;

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
    public void updateJobStatus(JobRotation jobRotation) {
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
    public void assignDriverForCollectionPoint(
            Integer jobRotationId,
            Integer smallTrucksCount, // Số xe từ request
            LocalDate rotationDate,
            Integer shiftId
    ) {
        // 1. Tính tải trọng cần thiết (1 xe = 1 tấn)
        BigDecimal requiredTonnage = new BigDecimal(smallTrucksCount);

        // 2. Tìm xe phù hợp
        List<Vehicle> suitableVehicles = vehicleRepository
                .findByStatusAndTonnageGreaterThanEqual("AVAILABLE", requiredTonnage);

        // 3. Tìm tài xế đã đăng ký
        Optional<RotationLog> driverRequest = findAvailableDriver(rotationDate,shiftId);

        // 4. Tạo phân công nếu tìm thấy
        if (driverRequest.isPresent() && !suitableVehicles.isEmpty()) {
            createJobRotation(
                    driverRequest.get().getStaffId(),
                    suitableVehicles.get(0).getId(),
                    jobRotationId
            );
        }
    }
    private Optional<RotationLog> findAvailableDriver(LocalDate date,Integer shiftId) {
        // 1. Lấy tất cả tài xế đã đăng ký ca trong ngày
        List<RotationLog> driverRequests = rotationLogRepository
                .findByStatusAndRotationDate("REQUEST", date);

        // 2. Sắp xếp theo rating giảm dần
        Map<Integer, Double> driverRatings = driverRatingRepository.findAll().stream()
                .collect(Collectors.toMap(
                        DriverRating::getDriverId,
                        DriverRating::getAverageRating
                ));

        driverRequests.sort((a, b) -> {
            Double ratingA = driverRatings.getOrDefault(a.getStaffId(), 0.0);
            Double ratingB = driverRatings.getOrDefault(b.getStaffId(), 0.0);
            return ratingB.compareTo(ratingA);
        });

        // 3. Lọc tài xế chưa có phân công trong ngày
        return driverRequests.stream()
                .filter(request -> !isDriverAssigned(request.getStaffId(), date,shiftId))
                .findFirst();
    }

    // Kiểm tra tài xế đã có phân công chưa
    private boolean isDriverAssigned(Integer driverId, LocalDate date, Integer shiftId // Thêm tham số shiftId
    ) {
        return jobRotationRepository.existsByStaffIdAndRotationDate(driverId, date,shiftId);
    }
    private void createJobRotation(
            Integer driverId,
            Integer vehicleId,
            Integer collectionJobId
    ) {
        // 1. Lấy thông tin điểm thu gom từ job của collector
        JobRotation collectionJob = jobRotationRepository.findById(collectionJobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc thu gom"));

        // 2. Tạo phân công cho tài xế
        JobRotation driverJob = new JobRotation();
        driverJob.setStaffId(driverId);
        driverJob.setVehicleId(vehicleId);
        driverJob.setJobPositionId(collectionJob.getJobPositionId());
        driverJob.setRole("DRIVER");
        driverJob.setStatus("PENDING");
        driverJob.setRotationDate(LocalDate.now());
        driverJob.setShiftId(collectionJob.getShiftId());
        driverJob.setCreatedAt(LocalDateTime.now());

        // 3. Lưu vào DB
        jobRotationRepository.save(driverJob);

        // 4. Cập nhật trạng thái log
        rotationLogRepository.updateStatusByStaffIdAndUpdatedAt(
                "ASSIGNED",
                driverId,
                LocalDateTime.now()
        );

        // 5. Đánh dấu xe đã được sử dụng
        vehicleRepository.updateStatus(vehicleId, "IN_USE");
    }

}