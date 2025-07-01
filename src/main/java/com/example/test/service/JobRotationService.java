package com.example.test.service;

import com.example.test.dto.*;
import com.example.test.entity.*;
import com.example.test.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final NotificationService notificationService;
    private final ShiftRepository shiftRepository;

    public List<JobRotationDetailDTO> getMyJobRotationsByDate(String userName, LocalDate date) {
        return jobRotationRepository.findByUserNameAndDate(userName, date);
    }

    public List<DriverJobWithCollectorStatusDTO> getDriverJobsWithCollectorStatusByDate(String username, LocalDate date) {
        return jobRotationRepository.findDriverJobsWithCollectorStatusByDate(username, date);
    }

    @Scheduled(cron = "0 0 * * * *")
    public void autoFailExpiredJobs() {
        int affected = jobRotationRepository.updateLateJobRotations();
        System.out.println("Updated " + affected + " job rotations to LATE status.");
    }

    @Transactional
    public MarkCompletionResponse markJobCompleted(MarkCompletionRequest request, String expectedRole) {
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

        if (!expectedRole.equals(jobRotation.getRole())) {
            response.setSuccess(false);
            response.setMessage("Chỉ " + expectedRole.toLowerCase() + " mới có thể thực hiện hành động này");
            return response;
        }

        if (!"ASSIGNED".equals(jobRotation.getStatus())) {
            response.setSuccess(false);
            response.setMessage("Công việc không ở trạng thái ASSIGNED");
            return response;
        }

        jobRotation.setStatus("COMPLETED");
        jobRotation.setUpdatedAt(LocalDateTime.now());
        jobRotationRepository.save(jobRotation);

        findDriverAndAssigned(request.getTonnage());

        response.setSuccess(true);
        response.setMessage("Đánh dấu hoàn thành công việc thành công");

        return response;
    }

    private void findDriverAndAssigned(Integer tonnage) {
        Optional<JobRotation> driverJobOpt = jobRotationRepository.findDriver(tonnage);

        if (driverJobOpt.isPresent()) {
            JobRotation driverJob = driverJobOpt.get();
            driverJob.setStatus("ASSIGNED");
            driverJob.setUpdatedAt(LocalDateTime.now());
            jobRotationRepository.save(driverJob);
        } else {
            System.out.println("Không tìm thấy tài xế phù hợp để phân công");
        }
    }
    private Optional<JobRotation> findAvailableDriverWithVehicle(LocalDate date, Integer shiftId, BigDecimal requiredTonnage) {
        // Tìm tài xế đã có nhiệm vụ cùng ca, ngày, xe chưa đầy
        List<JobRotation> assignedDrivers = jobRotationRepository.findByRotationDateAndShiftIdAndRole(date, shiftId, "DRIVER");

        for (JobRotation job : assignedDrivers) {
            Vehicle vehicle = vehicleRepository.findById(job.getVehicleId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin xe"));

            if (vehicle.getRemainingTonnage().compareTo(requiredTonnage) >= 0) {
                return Optional.of(job); // Ưu tiên tài xế này
            }
        }

        return Optional.empty(); // Không có tài xế phù hợp, chuyển sang tìm tài xế mới
    }
    @Transactional
    public void assignVehiclesForCollection(Integer locationId, BigDecimal totalTonnage, LocalDate rotationDate, Integer shiftId) {
        BigDecimal remaining = totalTonnage;

        // Ưu tiên tài xế cũ còn xe trống
        Optional<JobRotation> existingJob = findDriverWithAvailableVehicle(rotationDate, shiftId);
        if (existingJob.isPresent()) {
            JobRotation job = existingJob.get();
            Vehicle vehicle = vehicleRepository.findById(job.getVehicleId()).orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin xe"));

            BigDecimal load = vehicle.getRemainingTonnage().min(remaining);
            assignAdditionalJob(job.getStaffId(), vehicle, locationId, load, rotationDate, shiftId);

            vehicle.setRemainingTonnage(vehicle.getRemainingTonnage().subtract(load));
            vehicleRepository.save(vehicle);

            remaining = remaining.subtract(load);
        }

        // Lặp tiếp cho đến khi hết rác hoặc hết tài nguyên
        while (remaining.compareTo(BigDecimal.ZERO) > 0) {
            List<Vehicle> vehicles = vehicleRepository.findByStatusIn(Arrays.asList("AVAILABLE", "IN_USE"));
            vehicles.sort(Comparator.comparing(Vehicle::getRemainingTonnage).reversed());

            boolean assigned = false;
            for (Vehicle vehicle : vehicles) {
                if (vehicle.getRemainingTonnage().compareTo(BigDecimal.ZERO) <= 0) continue;

                Optional<RotationLog> driverOpt = findNewAvailableDriver(rotationDate, shiftId);
                if (driverOpt.isEmpty()) {
                    notificationService.sendAdminAlert("Thiếu tài xế điều xe lớn, còn lại " + remaining + " tấn chưa gom đủ");
                    return;
                }

                BigDecimal load = vehicle.getRemainingTonnage().min(remaining);
                assignJobToDriver(driverOpt.get(), vehicle, locationId, load, rotationDate, shiftId);

                vehicle.setRemainingTonnage(vehicle.getRemainingTonnage().subtract(load));
                vehicleRepository.save(vehicle);

                remaining = remaining.subtract(load);
                assigned = true;
                break;
            }

            if (!assigned) {
                notificationService.sendAdminAlert("Hết xe hoặc không đủ sức chứa, còn lại " + remaining + " tấn chưa gom đủ");
                break;
            }
        }
    }

    private Optional<RotationLog> findNewAvailableDriver(LocalDate date, Integer shiftId) {
        List<RotationLog> driverRequests = rotationLogRepository.findByStatusAndRotationDate("REQUEST", date);

        return driverRequests.stream()
                .filter(request -> !jobRotationRepository.existsByStaffIdAndRotationDateAndShiftId(request.getStaffId(), date, shiftId))
                .findFirst();
    }
    private Optional<JobRotation> findDriverWithAvailableVehicle(LocalDate date, Integer shiftId) {
        List<JobRotation> jobs = jobRotationRepository.findByRotationDateAndShiftIdAndRole(date, shiftId, "DRIVER");

        return jobs.stream()
                .filter(job -> {
                    Vehicle vehicle = vehicleRepository.findById(job.getVehicleId()).orElse(null);
                    return vehicle != null && vehicle.getRemainingTonnage().compareTo(BigDecimal.ZERO) > 0;
                })
                .findFirst();
    }


    public List<RoutePoint> calculateOptimizedRoute(Integer vehicleId, LocalDate rotationDate, Integer shiftId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe"));

        BigDecimal remainingCapacity = vehicle.getTonnage();
        List<RoutePoint> optimizedPoints = new ArrayList<>();

        List<Object[]> completedPoints = jobRotationRepository.findCompletedCollectionsWithTruckCounts(rotationDate, shiftId);

        for (Object[] pointData : completedPoints) {
            Integer jobRotationId = (Integer) pointData[0];
            Integer positionId = (Integer) pointData[1];
            Integer smallTrucksCount = (Integer) pointData[2];
            BigDecimal totalWaste = new BigDecimal(smallTrucksCount);

            JobPosition position = jobPositionRepository.findById(positionId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy điểm thu gom"));

            while (totalWaste.compareTo(BigDecimal.ZERO) > 0 && remainingCapacity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal load = totalWaste.min(remainingCapacity);

                optimizedPoints.add(new RoutePoint(positionId, position.getName(), position.getAddress(), load.intValue(), load));

                remainingCapacity = remainingCapacity.subtract(load);
                totalWaste = totalWaste.subtract(load);

                if (remainingCapacity.compareTo(BigDecimal.ZERO) <= 0) break;
            }

            if (remainingCapacity.compareTo(BigDecimal.ZERO) <= 0) break;
        }

        return optimizedPoints;
    }

    private Optional<RotationLog> findAvailableDriver(LocalDate date, Integer currentShiftId) {
        int nextShiftId = getNextShiftId(currentShiftId);

        List<RotationLog> driverRequests = rotationLogRepository.findByStatusAndRotationDateAndShiftId(
                "REQUEST", date, nextShiftId
        );

        Map<Integer, Double> driverRatings = driverRatingRepository.findAll().stream()
                .collect(Collectors.toMap(DriverRating::getDriverId, DriverRating::getAverageRating, (a, b) -> b));

        return driverRequests.stream()
                .filter(request -> !jobRotationRepository.existsByStaffIdAndRotationDateAndShiftId(
                        request.getStaffId(), date, nextShiftId
                ))
                .sorted(Comparator.comparingDouble((RotationLog r) ->
                        driverRatings.getOrDefault(r.getStaffId(), 0.0)
                ).reversed())
                .findFirst();
    }
    private int getNextShiftId(int currentShiftId) {
        List<Shift> shifts = shiftRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));

        if (shifts.isEmpty()) {
            throw new RuntimeException("Chưa cấu hình ca làm việc");
        }

        for (int i = 0; i < shifts.size(); i++) {
            if (shifts.get(i).getId().equals(currentShiftId)) {
                return (i == shifts.size() - 1) ? shifts.get(0).getId() : shifts.get(i + 1).getId();
            }
        }

        throw new RuntimeException("Không tìm thấy ca hiện tại trong cấu hình ca");
    }

    private void assignJobToDriver(RotationLog driverLog, Vehicle vehicle, Integer locationId,
                                   BigDecimal assignedTonnage, LocalDate rotationDate, Integer shiftId) {

        JobRotation job = new JobRotation();
        job.setStaffId(driverLog.getStaffId());
        job.setVehicleId(vehicle.getId());
        job.setRole("DRIVER");
        job.setStatus("PENDING");
        job.setRotationDate(rotationDate);
        job.setShiftId(shiftId);
        job.setJobPositionId(locationId);
        job.setTonnage(assignedTonnage);
        job.setCreatedAt(LocalDateTime.now());

        jobRotationRepository.save(job);

        rotationLogRepository.updateStatusByStaffIdAndUpdatedAt("ASSIGNED", driverLog.getStaffId(), LocalDateTime.now());
    }
    private void assignAdditionalJob(Integer staffId, Vehicle vehicle, Integer locationId,
                                     BigDecimal assignedTonnage, LocalDate rotationDate, Integer shiftId) {

        JobRotation job = new JobRotation();
        job.setStaffId(staffId);
        job.setVehicleId(vehicle.getId());
        job.setJobPositionId(locationId);
        job.setRole("DRIVER");
        job.setStatus("PENDING");
        job.setRotationDate(rotationDate);
        job.setShiftId(shiftId);
        job.setTonnage(assignedTonnage);
        job.setCreatedAt(LocalDateTime.now());

        jobRotationRepository.save(job);
    }
}