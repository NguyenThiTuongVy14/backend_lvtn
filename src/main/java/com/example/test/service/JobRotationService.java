package com.example.test.service;

import com.example.test.controller.JobRotationController;
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
    private final NotificationService notificationService;
    private final ShiftRepository shiftRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final StaffRepository staffRepository;
    private final FirebaseMessagingService firebaseMessagingService;
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

        List<Vehicle> vehicles = vehicleRepository.findByStatusIn(Arrays.asList("AVAILABLE", "IN_USE"));
        vehicles.sort(Comparator.comparing(Vehicle::getRemainingTonnage).reversed());

        // Bước 1: Tìm xe duy nhất đủ tải
        Optional<Vehicle> optimalVehicleOpt = vehicles.stream()
                .filter(v -> v.getRemainingTonnage().compareTo(totalTonnage) >= 0)
                .sorted(Comparator.comparing(v -> v.getRemainingTonnage().subtract(totalTonnage).abs()))
                .findFirst();

        if (optimalVehicleOpt.isPresent()) {
            Vehicle vehicle = optimalVehicleOpt.get();
            Optional<JobRotation> existingJob = findDriverWithVehicle(vehicle.getId(), rotationDate, shiftId);

            if (existingJob.isPresent()) {
                JobRotation jobRotation = existingJob.get();
                JobRotation newJob = assignAdditionalJob(jobRotation.getStaffId(), vehicle, locationId, totalTonnage, rotationDate, shiftId);
                firebaseMessagingService.sendToAllTokensByStaffId(jobRotation.getStaffId(), "Công việc mới", "Bạn được giao tại điểm #" + locationId);

                Shift shift = shiftRepository.findById(shiftId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc id = " + shiftId));

                String name = staffRepository.findFullNameById(jobRotation.getStaffId());
                if (name == null) {
                    notificationService.sendAdminAlert("Không tìm thấy tên nhân viên id = " + jobRotation.getStaffId());
                    return;
                }

                JobPosition position = jobPositionRepository.findById(jobRotation.getJobPositionId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy vị trí công việc id = " + jobRotation.getJobPositionId()));

                messagingTemplate.convertAndSend("/topic/assigned-driver",
                        new AssignedDriver(newJob.getId(), "PENDING", totalTonnage, shift, name, position,newJob.getVehicleId()));
            } else {
                Optional<RotationLog> driverOpt = findNewAvailableDriver(rotationDate, shiftId);
                if (driverOpt.isEmpty()) {
                    notificationService.sendAdminAlert("Thiếu tài xế điều xe lớn, còn lại " + totalTonnage + " tấn chưa gom đủ");
                    return;
                }

                RotationLog driver = driverOpt.get();
                JobRotation newJobRotation = assignJobToDriver(driver, vehicle, locationId, totalTonnage, rotationDate, shiftId);
                firebaseMessagingService.sendToAllTokensByStaffId(driver.getStaffId(), "Công việc mới", "Bạn được giao tại điểm #" + locationId);

                Shift shift = shiftRepository.findById(shiftId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc id = " + shiftId));

                String name = staffRepository.findFullNameById(driver.getStaffId());
                if (name == null) {
                    notificationService.sendAdminAlert("Không tìm thấy tên nhân viên id = " + driver.getStaffId());
                    return;
                }

                JobPosition position = jobPositionRepository.findById(newJobRotation.getJobPositionId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy vị trí công việc id = " + newJobRotation.getJobPositionId()));

                messagingTemplate.convertAndSend("/topic/assigned-driver",
                        new AssignedDriver(newJobRotation.getId(), "PENDING", totalTonnage, shift, name, position,newJobRotation.getVehicleId()));
            }

            vehicle.setRemainingTonnage(vehicle.getRemainingTonnage().subtract(totalTonnage));
            vehicleRepository.save(vehicle);
            messagingTemplate.convertAndSend("/topic/vehicle-updates",
                    new VehicleStatusUpdatedMessage(vehicle.getId(), vehicle.getStatus(), vehicle.getRemainingTonnage()));
            return;
        }

        // Bước 2: Không có xe nào đủ tải → phải chia nhỏ
        BigDecimal remaining = totalTonnage;

        // 2.1 Ưu tiên chia cho xe đã có tài xế trước
        List<JobRotation> existingJobs = jobRotationRepository.findByRotationDateAndShiftIdAndRole(rotationDate, shiftId, "DRIVER");
        for (JobRotation job : existingJobs) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            Vehicle vehicle = vehicleRepository.findById(job.getVehicleId()).orElse(null);
            if (vehicle == null || vehicle.getRemainingTonnage().compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal load = vehicle.getRemainingTonnage().min(remaining);
            JobRotation newJob = assignAdditionalJob(job.getStaffId(), vehicle, locationId, load, rotationDate, shiftId);

            // Send Firebase notification
            firebaseMessagingService.sendToAllTokensByStaffId(job.getStaffId(), "Công việc mới", "Bạn được giao tại điểm #" + locationId);

            // Send WebSocket notification
            Shift shift = shiftRepository.findById(shiftId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc id = " + shiftId));

            String name = staffRepository.findFullNameById(job.getStaffId());
            if (name != null) {
                JobPosition position = jobPositionRepository.findById(job.getJobPositionId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy vị trí công việc id = " + job.getJobPositionId()));

                messagingTemplate.convertAndSend("/topic/assigned-driver",
                        new AssignedDriver(newJob.getId(), "PENDING", load, shift, name, position,newJob.getVehicleId()));
            }

            vehicle.setRemainingTonnage(vehicle.getRemainingTonnage().subtract(load));
            vehicleRepository.save(vehicle);

            messagingTemplate.convertAndSend("/topic/vehicle-updates",
                    new VehicleStatusUpdatedMessage(vehicle.getId(), vehicle.getStatus(), vehicle.getRemainingTonnage()));

            remaining = remaining.subtract(load);
        }

        // 2.2 Tiếp tục chia cho xe chưa được phân công nếu còn dư
        for (Vehicle vehicle : vehicles) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            if (vehicle.getRemainingTonnage().compareTo(BigDecimal.ZERO) <= 0) continue;

            // Bỏ qua xe đã có tài xế
            boolean alreadyAssigned = existingJobs.stream().anyMatch(j -> j.getVehicleId().equals(vehicle.getId()));
            if (alreadyAssigned) continue;

            Optional<RotationLog> driverOpt = findNewAvailableDriver(rotationDate, shiftId);
            if (driverOpt.isEmpty()) {
                notificationService.sendAdminAlert("Thiếu tài xế điều xe lớn, còn lại " + remaining + " tấn chưa gom đủ");
                return;
            }

            BigDecimal load = vehicle.getRemainingTonnage().min(remaining);
            JobRotation newJob = assignJobToDriver(driverOpt.get(), vehicle, locationId, load, rotationDate, shiftId);

            // Send Firebase notification
            firebaseMessagingService.sendToAllTokensByStaffId(driverOpt.get().getStaffId(), "Công việc mới", "Bạn được giao tại điểm #" + locationId);

            // Send WebSocket notification
            Shift shift = shiftRepository.findById(shiftId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc id = " + shiftId));

            String name = staffRepository.findFullNameById(driverOpt.get().getStaffId());
            if (name != null) {
                JobPosition position = jobPositionRepository.findById(newJob.getJobPositionId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy vị trí công việc id = " + newJob.getJobPositionId()));

                messagingTemplate.convertAndSend("/topic/assigned-driver",
                        new AssignedDriver(newJob.getId(), "PENDING", load, shift, name, position,newJob.getVehicleId()));
            }

            vehicle.setRemainingTonnage(vehicle.getRemainingTonnage().subtract(load));
            vehicleRepository.save(vehicle);

            messagingTemplate.convertAndSend("/topic/vehicle-updates",
                    new VehicleStatusUpdatedMessage(vehicle.getId(), vehicle.getStatus(), vehicle.getRemainingTonnage()));

            remaining = remaining.subtract(load);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            notificationService.sendAdminAlert("Hết xe hoặc không đủ sức chứa, còn lại " + remaining + " tấn chưa gom đủ");
        }
    }

    private record VehicleStatusUpdatedMessage(Integer vehicleId, String status, BigDecimal remainingTonnage) {}
    private record AssignedDriver(Integer jobId,String status, BigDecimal totalTonnage,Shift shift,String name,JobPosition jobPosition,Integer vehicleId) {}

    private Optional<JobRotation> findDriverWithVehicle(Integer vehicleId, LocalDate date, Integer shiftId) {
        List<JobRotation> results = jobRotationRepository.findByVehicleIdAndRotationDateAndShiftId(vehicleId, date, shiftId);
        if (results.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(results.get(0));
    }

    private Optional<RotationLog> findNewAvailableDriver(LocalDate date, Integer shiftId) {
// Khai báo ngày chính xác

// In ra kiểm tra giá trị truyền vào
        System.out.println("Truy vấn với status = REQUEST và ngày = " + date);

// Gọi repository
        List<RotationLog> driverRequests = rotationLogRepository.findByStatusAndRotationDate("REQUEST", date);

// Kiểm tra kết quả
        System.out.println("Số lượng bản ghi tìm được: " + driverRequests.size());
        driverRequests.forEach(r -> {
            System.out.println("ID: " + r.getId() + " | StaffId: " + r.getStaffId() + " | Ngày: " + r.getRotationDate() + " | Trạng thái: " + r.getStatus());
        });

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

    private JobRotation assignJobToDriver(RotationLog driverLog, Vehicle vehicle, Integer locationId,
                                          BigDecimal assignedTonnage, LocalDate rotationDate, Integer shiftId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Staff collector = staffRepository.findByUserName(username);
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
        job.setCollectionRequestId(collector.getId());
        JobRotation savedJob = jobRotationRepository.save(job);

        rotationLogRepository.updateStatusByStaffIdAndUpdatedAt("ASSIGNED", driverLog.getStaffId(), LocalDateTime.now());

        return savedJob; // Return the saved job rotation
    }

    private JobRotation assignAdditionalJob(Integer staffId, Vehicle vehicle, Integer locationId,
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

        return jobRotationRepository.save(job); // Return the saved job rotation
    }
}