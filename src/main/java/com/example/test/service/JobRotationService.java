package com.example.test.service;

import com.example.test.dto.*;
import com.example.test.entity.*;
import com.example.test.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class JobRotationService {

    private final JobRotationRepository jobRotationRepository;
    private final JobRotationTempRepository jobRotationTempRepository;

    private final VehicleRepository vehicleRepository;
    private final RotationLogRepository rotationLogRepository;
    private final JobPositionRepository jobPositionRepository;
    private final ShiftRepository shiftRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final StaffRepository staffRepository;
    private final FirebaseMessagingService firebaseMessagingService;
    private final PriorityAllocatorService priorityAllocatorService;
    private final RouteOptimizationService routeOptimizationService;
    private final RouteRepository routeRepository;

    public List<JobRotationDetailDTO> getMyJobRotationsByDate(String userName, LocalDate date) {
        return jobRotationRepository.findByUserNameAndDate(userName, date);
    }

    /**
     * Cron 12:00 trưa hằng ngày: push WAITLIST (ưu tiên carry_points) lên ASSIGNED nếu còn slot.
     * Phân công cho ngày mai (tuỳ bạn chỉnh).
     */
    @Scheduled(cron = "0 0 12 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void autoPromoteAtNoon() {
        LocalDate targetDate = LocalDate.now().plusDays(1);
        priorityAllocatorService.promoteWaitlistIfAny(targetDate);
    }

    @Scheduled(cron = "0 0 * * * *")
    public void autoFailExpiredJobs() {
        int affected = jobRotationRepository.updateLateJobRotations();
        System.out.println("Updated " + affected + " job rotations to LATE status.");
    }

    @Scheduled(cron = "0 25 16 * * *", zone = "Asia/Ho_Chi_Minh")
    public void assignCollector() {
        List<JobPosition> positions = jobPositionRepository.findByStatusAndUserIdNotNull("ACTIVE");
        for (JobPosition jobPosition : positions) {
            JobRotation jobRotation = new JobRotation();
            jobRotation.setJobPositionId(jobPosition.getId());
            jobRotation.setRole("COLLECTOR");
            jobRotation.setRotationDate(LocalDate.now());
            jobRotation.setShiftId(2);
            jobRotation.setStaffId(jobPosition.getUserId());
            jobRotation.setStatus("PENDING");
            jobRotation.setCreatedAt(LocalDateTime.now());
            jobRotation.setUpdatedAt(LocalDateTime.now());
            jobRotationRepository.save(jobRotation);
        }
        System.out.println("Assigned " + positions.size() + " job rotations.");

    }

    @Transactional
    public MarkCompletionResponse markDriverJobCompleted(MarkCompletionRequest request, Staff driver) {
        MarkCompletionResponse response = new MarkCompletionResponse();

        if (request.getJobRotationId() == null) {
            response.setSuccess(false);
            response.setMessage("Job rotation ID không được để trống");
            return response;
        }

        Optional<JobRotationTemp> jobRotationOpt = jobRotationTempRepository.findById(request.getJobRotationId());

        if (jobRotationOpt.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Không tìm thấy công việc với ID: " + request.getJobRotationId());
            return response;
        }

        JobRotationTemp jobRotation = jobRotationOpt.get();

        if (!jobRotation.getStaffId().equals(driver.getId())) {
            response.setSuccess(false);
            response.setMessage("Công việc không thuộc về tài xế hiện tại");
            return response;
        }

        if (!"PENDING".equals(jobRotation.getStatus()) && !"PROCESSING".equals(jobRotation.getStatus())) {
            response.setSuccess(false);
            response.setMessage("Công việc không ở trạng thái có thể hoàn thành");
            return response;
        }
        jobRotation.setStatus("COMPLETED");
        jobRotation.setUpdatedAt(LocalDateTime.now());
        jobRotationTempRepository.save(jobRotation);

        Vehicle vehicle = vehicleRepository.findById(jobRotation.getVehicleId()).orElse(null);
        if (vehicle != null) {
            BigDecimal current = vehicle.getCurrentTonnage() != null
                    ? vehicle.getCurrentTonnage()
                    : BigDecimal.ZERO;
            BigDecimal added = jobRotation.getTonnage() != null
                    ? jobRotation.getTonnage()
                    : BigDecimal.ZERO;

            vehicle.setCurrentTonnage(current.add(added));
            vehicleRepository.save(vehicle);
            messagingTemplate.convertAndSend("/topic/vehicle-updates", vehicle);
        }


        JobUpdateDTO message = new JobUpdateDTO();
        message.setJobRotationId(jobRotation.getId());
        message.setFullName(driver.getFullName());
        Shift shift = shiftRepository.findById(jobRotation.getShiftId())
                .orElse(null);
        message.setShift(shift.getName());
        message.setTonnage(jobRotation.getTonnage());
        message.setStatus(jobRotation.getStatus());
        messagingTemplate.convertAndSend("/topic/job-driver-updates", message);

        Optional<Route> route = routeRepository.findRouteByRotationId(request.getJobRotationId());
        List<Route> routes = routeRepository.findByVehicleId(vehicle.getId());
        for (Route routeItem : routes) {
            if(routeItem.getIndex() - 1 == route.get().getIndex()) {
                if(routeItem.getRotationId() == null){
                    continue;
                }
                Optional<JobRotationTemp> job = jobRotationTempRepository.findById(routeItem.getRotationId());
                if(job.isPresent()) {
                    JobRotationTemp jobTemp = job.get();
                    jobTemp.setStatus("PROCESSING");
                    jobTemp.setUpdatedAt(LocalDateTime.now());
                    jobRotationTempRepository.save(jobTemp);
                }
//                else{
//                    response.setMessage("Đã hoàn thành tất cả công việc");
//                    response.setSuccess(true);
//                    return response;
//                }
            }
        }

        response.setMessage("Job rotation completed");
        response.setSuccess(true);
        return response;
    }
    private record JobDriverCompletedMessage(Integer jobId, String name,Integer driverId, String status,Integer vehicleId,Integer shiftId, BigDecimal remainingTonnage,LocalDate rotationDate) {}



    private record VehicleStatusUpdatedMessage(Integer vehicleId, String status, BigDecimal remainingTonnage) {}

    /**
     * Gán xe cho những driver ASSIGNED trong ngày.
     * Nếu dư driver → chuyển UNASSIGNED & tăng carry_points.
     */
    @Transactional
    public int assignAllVehicles(LocalDate rotationDate) {
        // 1. Lấy danh sách driver ASSIGNED (đã được chọn từ carry_points)
        List<RotationLog> assignedDrivers = rotationLogRepository.findAssignedOrderByCarryPoints(rotationDate);
        if (assignedDrivers.isEmpty()) return -1;

        // 2. Lấy danh sách xe, sắp xếp theo remainingTonnage giảm dần
        List<Vehicle> vehicles = vehicleRepository.findByStatus("AVAILABLE");
        vehicles.sort((a, b) -> b.getRemainingTonnage().compareTo(a.getRemainingTonnage()));
        if (vehicles.isEmpty()) return -2;

        // 3. Lấy danh sách các điểm tập kết (JobRotationTemp, role = COLLECTOR)
        List<JobRotationTemp> jobRotations = jobRotationTempRepository.findByVehicleIdNullAndRole("COLLECTOR");
        jobRotations.sort((a, b) -> b.getTonnage().compareTo(a.getTonnage()));

        // 4. Gán xe cho từng driver ASSIGNED
        for (int i = 0; i < assignedDrivers.size(); i++) {
            if (i >= vehicles.size()) break; // (Thừa ra thì thoát)
            RotationLog driver = assignedDrivers.get(i);
            Vehicle vehicle = vehicles.get(i);

            List<JobRotationTemp> bestSubset = assignRotationForVehicle(jobRotations, vehicle, driver.getStaffId());
            jobRotations.removeAll(bestSubset);


            if (bestSubset.isEmpty()) {
                driver.setStatus("UNASSIGNED");
                rotationLogRepository.save(driver);
                staffRepository.changeCarryPoints(driver.getStaffId(), 1);
//                firebaseMessagingService.sendToAllTokensByStaffId(
//                        driver.getStaffId(),
//                        "Thông báo công việc",
//                        "Hôm nay không còn điểm thu gom nào cả.\nBạn sẽ được cộng điểm ưu tiên vào lần sau.",
//                        "info"
//                );
            }
            else {
                resetCarryPointsAfterCompleted(driver.getId());
//                firebaseMessagingService.sendToAllTokensByStaffId(
//                    driver.getStaffId(),
//                    "Đã đến giờ làm việc",
//                    "Bạn được phân công " + bestSubset.size() + " điểm thu gom.\nXe: " + vehicle.getLicensePlate(),
//                    "info"
//            );
            }
        }

        if (!jobRotations.isEmpty()) {
            return -2;
        }
        return 1;
    }

    public List<JobRotationTemp> assignRotationForVehicle(List<JobRotationTemp> jobRotations, Vehicle vehicle, Integer idDriver) {
        BigDecimal capacity = vehicle.getRemainingTonnage();
        int n = jobRotations.size();
        BigDecimal bestSum = BigDecimal.ZERO;
        List<JobRotationTemp> bestSubset = new ArrayList<>();

        // Tìm tập con tối ưu
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

        if (bestSubset.isEmpty()) {
            return bestSubset;
        }
        List<JobRotationTemp> temps = new ArrayList<>();
        for (JobRotationTemp job : bestSubset) {
            job.setVehicleId(vehicle.getId());
            jobRotationTempRepository.save(job);
            JobRotationTemp jobDriver = new JobRotationTemp();
            jobDriver.setVehicleId(vehicle.getId());
            jobDriver.setJobPositionId(job.getJobPositionId());
            jobDriver.setRole("DRIVER");
            jobDriver.setShiftId(job.getShiftId());
            jobDriver.setTonnage(job.getTonnage());
            jobDriver.setSmallTrucksCount(job.getSmallTrucksCount());
            jobDriver.setUpdatedAt(LocalDateTime.now());
            jobDriver.setRotationDate(job.getRotationDate());
            jobDriver.setStatus("PENDING");
            jobDriver.setStaffId(idDriver);
            jobRotationTempRepository.save(jobDriver);


            temps.add(jobDriver);
        }

        // Trừ tải trọng còn lại
        vehicle.setRemainingTonnage(vehicle.getRemainingTonnage().subtract(bestSum));
        vehicle.setStatus("IN_USE");
        vehicleRepository.save(vehicle);


        Map<String, Object> result = routeOptimizationService.optimizeRoute(temps, vehicle);
        routeOptimizationService.convertAndSaveRoute(result);
        return bestSubset;
    }





    /**
     * Khi tài xế hoàn thành công việc -> reset carry_points = 0
     * (Bạn gọi hàm này ở chỗ mark completed)
     */
    @Transactional
    public void resetCarryPointsAfterCompleted(Integer staffId) {
        staffRepository.resetCarryPoints(staffId);
    }

    @Transactional
    public int finalizeDailyAssignments(LocalDate rotationDate) {
        List<JobRotationTemp> tempJobs =
                jobRotationTempRepository.findByRotationDate(rotationDate);

        if (tempJobs.isEmpty()) return 0;

        for (JobRotationTemp temp : tempJobs) {
            JobRotation job = new JobRotation();
            job.setStaffId(temp.getStaffId());
            job.setJobPositionId(temp.getJobPositionId());
            job.setVehicleId(temp.getVehicleId());
            job.setShiftId(temp.getShiftId());
            job.setRotationDate(rotationDate);
            job.setTonnage(temp.getTonnage());
            job.setRole(temp.getRole());
            job.setStatus(temp.getStatus());
            job.setCreatedAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());

            jobRotationRepository.save(job);
        }

        jobRotationTempRepository.deleteAllByRotationDate(rotationDate);
        return tempJobs.size();
    }

}