package com.example.test.service;

import com.example.test.dto.*;
import com.example.test.entity.*;
import com.example.test.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final JobPositionRepository jobPositionRepository;
    private final ShiftRepository shiftRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final StaffRepository staffRepository;
    private final FirebaseMessagingService firebaseMessagingService;
    private final JobRotationTempRepository jobRotationTempRepository;

    public List<JobRotationDetailDTO> getMyJobRotationsByDate(String userName, LocalDate date) {
        return jobRotationRepository.findByUserNameAndDate(userName, date);
    }


    @Scheduled(cron = "0 0 * * * *")
    public void autoFailExpiredJobs() {
        int affected = jobRotationRepository.updateLateJobRotations();
        System.out.println("Updated " + affected + " job rotations to LATE status.");
    }
    @Transactional
    public MarkCompletionResponse markDriverJobCompleted(MarkCompletionRequest request, Integer driverId) {
        MarkCompletionResponse response = new MarkCompletionResponse();

        if (request.getJobRotationId() == null) {
            response.setSuccess(false);
            response.setMessage("Job rotation ID không được để trống");
            return response;
        }

        Optional<JobRotation> jobRotationOpt = jobRotationRepository.findById(request.getJobRotationId());

        if (jobRotationOpt.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Không tìm thấy công việc với ID: " + request.getJobRotationId());
            return response;
        }

        JobRotation jobRotation = jobRotationOpt.get();

        // Kiểm tra quyền sở hữu công việc
        if (!jobRotation.getStaffId().equals(driverId)) {
            response.setSuccess(false);
            response.setMessage("Công việc không thuộc về tài xế hiện tại");
            return response;
        }

        if (!"DRIVER".equals(jobRotation.getRole())) {
            response.setSuccess(false);
            response.setMessage("Chỉ tài xế mới có thể thực hiện hành động này");
            return response;
        }

        if (!"PENDING".equals(jobRotation.getStatus()) && !"ASSIGNED".equals(jobRotation.getStatus())) {
            response.setSuccess(false);
            response.setMessage("Công việc không ở trạng thái có thể hoàn thành");
            return response;
        }

        // Đánh dấu công việc hiện tại hoàn thành
        jobRotation.setStatus("COMPLETED");
        jobRotation.setUpdatedAt(LocalDateTime.now());
        jobRotationRepository.save(jobRotation);

        // Kiểm tra xem có còn công việc nào khác của tài xế trong cùng ca và ngày không
        List<JobRotation> remainingJobs = jobRotationRepository.findByStaffIdAndRotationDateAndShiftIdAndStatusNot(
                driverId,
                jobRotation.getRotationDate(),
                jobRotation.getShiftId(),
                "COMPLETED"
        );
        messagingTemplate.convertAndSend("/topic/assigned-driver",
                new JobDriverCompletedMessage(jobRotation.getId(), staffRepository.findFullNameById(driverId),driverId, "COMPLETED",jobRotation.getVehicleId(),jobRotation.getShiftId(),jobRotation.getTonnage(),jobRotation.getRotationDate()));
        // Nếu không còn công việc nào khác, reset trạng thái xe
        if (remainingJobs.isEmpty()) {
            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(jobRotation.getVehicleId());
            if (vehicleOpt.isPresent()) {
                Vehicle vehicle = vehicleOpt.get();
                // Reset lại tải trọng còn lại về tải trọng ban đầu
                vehicle.setRemainingTonnage(vehicle.getTonnage());
                vehicle.setStatus("AVAILABLE");
                vehicleRepository.save(vehicle);
                messagingTemplate.convertAndSend("/topic/vehicle-updates",
                        new VehicleStatusUpdatedMessage(vehicle.getId(), vehicle.getStatus(), vehicle.getRemainingTonnage()));
                response.setSuccess(true);
                response.setMessage("Đã hoàn thành tất cả công việc trong ca. Xe đã được reset về trạng thái ban đầu.");
                response.setUpdatedAt(LocalDateTime.now());
            } else {
                response.setSuccess(true);
                response.setMessage("Đã hoàn thành công việc nhưng không tìm thấy thông tin xe để reset");
                response.setUpdatedAt(LocalDateTime.now());
            }
        } else {
            response.setSuccess(true);
            response.setMessage("Đã hoàn thành công việc tại điểm này. Còn " + remainingJobs.size() + " điểm khác cần hoàn thành.");
            response.setUpdatedAt(LocalDateTime.now());
        }

        return response;
    }
    private record JobDriverCompletedMessage(Integer jobId, String name,Integer driverId, String status,Integer vehicleId,Integer shiftId, BigDecimal remainingTonnage,LocalDate rotationDate) {}



    private record VehicleStatusUpdatedMessage(Integer vehicleId, String status, BigDecimal remainingTonnage) {}

    public int assignAllVehicles() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        vehicles.sort((a, b) -> b.getRemainingTonnage().compareTo(a.getRemainingTonnage()));

        //Lay danh sach cac dien chua co xe va sap xep so luong rac giam dan
        List<JobRotationTemp> jobRotations = jobRotationTempRepository.findByVehicleIdNullAndRole("COLLECTOR");
        jobRotations.sort((a, b) -> b.getTonnage().compareTo(a.getTonnage()));

        List<RotationLog> rotationLogs = rotationLogRepository.findAll();

        for (Vehicle vehicle : vehicles) {
            if(!rotationLogs.isEmpty()) {
                jobRotations.removeAll(assignRotationForVehicle(jobRotations, vehicle, rotationLogs.get(0).getId()));
                rotationLogs.remove(0);
            }
            else
                return -1; // Không đủ tài xế
        }

        if(!jobRotations.isEmpty()){
            return -2;
        }
        return 1;
    }

    public List<JobRotationTemp> assignRotationForVehicle(List<JobRotationTemp> jobRotations, Vehicle vehicle, Integer idDriver) {
        BigDecimal capacity = vehicle.getRemainingTonnage();


        int n = jobRotations.size();
        BigDecimal bestSum = BigDecimal.ZERO;
        List<JobRotationTemp> bestSubset = new ArrayList<>();

        // B2: Duyệt tất cả tập con (n nhỏ mới dùng được, ví dụ n <= 20)
        for (int mask = 0; mask < (1 << n); mask++) {
            BigDecimal currentSum = BigDecimal.ZERO;
            List<JobRotationTemp> currentSubset = new ArrayList<>();

            for (int i = 0; i < n; i++) {
                if (((mask >> i) & 1) == 1) {
                    JobRotationTemp job = jobRotations.get(i);
                    currentSum = currentSum.add(job.getTonnage());
                    if (currentSum.compareTo(capacity) > 0) break;
                    currentSubset.add(job);
                }
            }

            if (currentSum.compareTo(capacity) <= 0 && currentSum.compareTo(bestSum) > 0) {
                bestSum = currentSum;
                bestSubset = currentSubset;
            }
        }

        // B3: Gán các bãi vào xe
        for (JobRotationTemp job : bestSubset) {
            JobRotationTemp jobDriver = new JobRotationTemp();
            jobDriver.setVehicleId(vehicle.getId());
            jobDriver.setJobPositionId(job.getJobPositionId());
            jobDriver.setRole("DRIVER");
            jobDriver.setShiftId(job.getShiftId());
            jobDriver.setTonnage(job.getTonnage());
            jobDriver.setSmallTrucksCount(job.getSmallTrucksCount());
            jobDriver.setUpdatedAt(LocalDateTime.now());
            jobDriver.setStaffId(job.getStaffId());
            jobDriver.setStatus("PENDING");
            jobDriver.setStaffId(idDriver);
            jobRotationTempRepository.save(jobDriver);
        }

        // B4: Trừ tải trọng còn lại
        vehicle.setRemainingTonnage(vehicle.getRemainingTonnage().subtract(bestSum));
        vehicleRepository.save(vehicle);
        return bestSubset;

    }







}