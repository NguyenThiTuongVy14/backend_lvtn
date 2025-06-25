package com.example.test.controller;

import com.example.test.dto.*;
import com.example.test.entity.*;
import com.example.test.repository.*;
import com.example.test.service.JobRotationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/job-rotations")
public class JobRotationController {

    private final JobRotationRepository jobRotationRepository;
    private final JobRotationService jobRotationService;
    private final StaffRepository staffRepository;
    private final ShiftRepository shiftRepository;
    private final VehicleRepository  vehicleRepository;
    private final RotationLogRepository  rotationLogRepository;
    private final DriverRatingRepository driverRatingRepository;
    private final JobPositionRepository jobPositionRepository;
    @Autowired
    public JobRotationController(JobRotationRepository jobRotationRepository,
                                 JobRotationService jobRotationService,
                                 StaffRepository staffRepository, ShiftRepository shiftRepository, VehicleRepository vehicleRepository, RotationLogRepository rotationLogRepository, DriverRatingRepository driverRatingRepository, JobPositionRepository jobPositionRepository) {
        this.jobRotationRepository = jobRotationRepository;
        this.jobRotationService = jobRotationService;
        this.staffRepository = staffRepository;
        this.shiftRepository = shiftRepository;
        this.vehicleRepository = vehicleRepository;
        this.rotationLogRepository = rotationLogRepository;
        this.driverRatingRepository = driverRatingRepository;
        this.jobPositionRepository = jobPositionRepository;
    }


    @GetMapping
    public ResponseEntity<?> getAllJobRotations() {
        try {
            List<JobRotation> rotations = jobRotationRepository.findAll();
            return ResponseEntity.ok(rotations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi khi lấy danh sách lịch phân công: " + e.getMessage()));
        }
    }

    // Lấy lịch phân công theo ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getJobRotationById(@PathVariable Integer id) {
        try {
            Optional<JobRotation> rotation = jobRotationRepository.findById(id);
            if (rotation.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorMessage("Lịch phân công không tìm thấy"));
            }

            // Kiểm tra quyền truy cập
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Staff currentUser = staffRepository.findByUserName(username);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorMessage("Người dùng không hợp lệ"));
            }

            // Nếu không phải ADMIN, chỉ cho phép xem lịch của chính mình
            Optional<String> authority = staffRepository.findAuthorityNameByStaffId(currentUser.getId());
            if (!authority.orElse("").equals("ADMIN") &&
                    !rotation.get().getStaffId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorMessage("Không có quyền truy cập"));
            }

            return ResponseEntity.ok(rotation.get());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi khi lấy lịch phân công: " + e.getMessage()));
        }
    }


    // Lấy lịch phân công của nhân viên theo ID (chỉ ADMIN)
    @GetMapping("/staff/{staffId}")
    public ResponseEntity<?> getJobRotationsByStaffId(@PathVariable Integer staffId) {
        try {
            List<JobRotation> rotations = jobRotationRepository.findByStaffId(staffId);
            return ResponseEntity.ok(rotations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi khi lấy lịch phân công theo nhân viên: " + e.getMessage()));
        }
    }

    // Lấy thống kê lịch phân công (chỉ ADMIN)
    @GetMapping("/statistics")
    public ResponseEntity<?> getJobRotationStatistics() {
        try {
            List<Object[]> statistics = jobRotationRepository.getRotationStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi khi lấy thống kê lịch phân công: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<List<JobRotationDetailDTO>> getMyRotations(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("Logged in username: " + username);
        List<JobRotationDetailDTO> rotations;
        if (date != null) {
            rotations = jobRotationService.getMyJobRotationsByDate(username, date);
        } else {
            rotations = jobRotationService.getMyJobRotationsByDate(username, LocalDate.now());
        }

        return ResponseEntity.ok(rotations);
    }
//    @PostMapping("/collector/completed")
//    public ResponseEntity<MarkCompletionResponse> collectorMarkCompleted(
//            @RequestBody MarkCompletionRequest request) {
//
//        MarkCompletionResponse response = jobRotationService.markJobCompleted(request, "COLLECTOR");
//        if (response.isSuccess()) {
//            return ResponseEntity.ok(response);
//        } else {
//            return ResponseEntity.badRequest().body(response);
//        }
//    }
    // API để tài xế đăng ký ca làm việc
    @PostMapping("/driver/register-shift")
    public ResponseEntity<?> registerDriverShift(@RequestBody DriverShiftRegistrationRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Staff driver = staffRepository.findByUserName(username);

            // Kiểm tra ca làm việc tồn tại
            Optional<Shift> shiftOpt = shiftRepository.findById(request.getShiftId());
            if (shiftOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorMessage("Ca làm việc không tồn tại"));
            }

            // Kiểm tra xe tồn tại và có sẵn sàng
//            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(request.getVehicleId());
//            if (vehicleOpt.isEmpty() || !"AVAILABLE".equals(vehicleOpt.get().getStatus())) {
//                return ResponseEntity.badRequest().body(new ErrorMessage("Xe không khả dụng"));
//            }

            // Lưu vào bảng rotation log với trạng thái REQUEST
            RotationLog log = new RotationLog();
            log.setStaffId(driver.getId());
            log.setShiftId(request.getShiftId());
//            log.setVehicleId(request.getVehicleId());
            log.setStatus("REQUEST");
            log.setRequestedAt(LocalDateTime.now());
            log.setRotationDate(request.getRotationDate());

            rotationLogRepository.save(log);

            return ResponseEntity.ok(new ResponseMessage("Đăng ký ca làm việc thành công. Đang chờ xác nhận"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi khi đăng ký ca làm việc: " + e.getMessage()));
        }
    }

    // API để người gom rác đánh dấu hoàn thành công việc
    @PostMapping("/collector/completed")
    public ResponseEntity<?> collectorMarkCompleted(@RequestBody CollectorCompletionRequest request) {
        try {
            // 1. Xác thực người gom rác
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Staff collector = staffRepository.findByUserName(username);

//            if (collector == null || !"COLLECTOR".equals(collector.getRole())) {
//                return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                        .body(new ErrorMessage("Chỉ người gom rác mới được sử dụng chức năng này"));
//            }

            // 2. Kiểm tra công việc tồn tại và thuộc về người này
            Optional<JobRotation> currentJobOpt = jobRotationRepository.findById(request.getJobRotationId());
            if (currentJobOpt.isEmpty() || !currentJobOpt.get().getStaffId().equals(collector.getId())) {
                return ResponseEntity.badRequest()
                        .body(new ErrorMessage("Công việc không tồn tại hoặc không thuộc về bạn"));
            }

            JobRotation currentJob = currentJobOpt.get();

            // 3. Cập nhật trạng thái công việc
            currentJob.setStatus("COMPLETED");
            currentJob.setUpdatedAt(LocalDateTime.now());
            jobRotationRepository.save(currentJob);

            // 4. Tự động phân công tài xế (phiên bản đã sửa)
            assignDriverForCollectionPoint(
                    currentJob.getJobPositionId(),
                    request.getSmallTrucksCount(),
                    currentJob.getRotationDate()
            );

            return ResponseEntity.ok(new ResponseMessage("Đã hoàn thành công việc và yêu cầu vận chuyển rác"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi khi đánh dấu hoàn thành: " + e.getMessage()));
        }
    }

    // Phương thức phân công tài xế - VERSION ĐÃ SỬA
    private void assignDriverForCollectionPoint(Integer collectionPointId, int smallTrucksCount, LocalDate rotationDate) {
        // 1. Tính toán tải trọng cần thiết
        BigDecimal requiredTonnage = BigDecimal.valueOf(smallTrucksCount);

        // 2. Tìm tất cả tài xế đã đăng ký ca làm việc trong ngày
        List<RotationLog> pendingDriverRequests = rotationLogRepository.findByStatusAndRotationDate("REQUEST", rotationDate);

        // 3. Sắp xếp tài xế theo rating cao nhất
        Map<Integer, Double> driverRatings = driverRatingRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        DriverRating::getDriverId,
                        DriverRating::getAverageRating
                ));

        pendingDriverRequests.sort((a, b) -> {
            Double ratingA = driverRatings.getOrDefault(a.getStaffId(), 0.0);
            Double ratingB = driverRatings.getOrDefault(b.getStaffId(), 0.0);
            return ratingB.compareTo(ratingA);
        });

        // 4. Tìm xe phù hợp
        for (RotationLog driverRequest : pendingDriverRequests) {
            // Tìm xe có sẵn, đủ tải trọng và chưa được phân công
            Optional<Vehicle> suitableVehicle = vehicleRepository
                    .findAvailableVehicles(requiredTonnage, rotationDate)
                    .stream()
                    .findFirst();

            if (suitableVehicle.isPresent()) {
                Vehicle vehicle = suitableVehicle.get();

                // 5. Tạo công việc cho tài xế
                JobRotation driverJob = new JobRotation();
                driverJob.setStaffId(driverRequest.getStaffId());
                driverJob.setJobPositionId(collectionPointId);
                driverJob.setVehicleId(vehicle.getId());
                driverJob.setRole("DRIVER");
                driverJob.setStatus("PENDING");
                driverJob.setRotationDate(rotationDate);
                driverJob.setShiftId(driverRequest.getShiftId());
                driverJob.setCreatedAt(LocalDateTime.now());
                driverJob.setUpdatedAt(LocalDateTime.now());

                jobRotationRepository.save(driverJob);

                // 6. Cập nhật trạng thái
                driverRequest.setStatus("ASSIGNED");
                rotationLogRepository.save(driverRequest);

                vehicle.setStatus("IN_USE");
                vehicleRepository.save(vehicle);

                break; // Chỉ phân công cho 1 tài xế
            }
        }
    }

    private boolean isVehicleAssigned(Integer vehicleId, LocalDate date) {
        return jobRotationRepository.existsByVehicleIdAndRotationDate(vehicleId, date);
    }

        // API để tài xế xem các điểm cần chạy dựa trên tải trọng còn lại
    @GetMapping("/driver/optimized-routes")
    public ResponseEntity<?> getOptimizedRoutesForDriver() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Staff driver = staffRepository.findByUserName(username);

//            if (driver == null || !"DRIVER".equals(driver.getRole())) {
//                return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                        .body(new ErrorMessage("Chỉ tài xế mới có thể sử dụng chức năng này"));
//            }

            // Lấy công việc hiện tại của tài xế
            List<JobRotation> driverJobs = jobRotationRepository.findByStaffIdAndRotationDateAndStatus(
                    driver.getId(), LocalDate.now(), "PENDING");

            if (driverJobs.isEmpty()) {
                return ResponseEntity.ok(new ResponseMessage("Không có công việc nào cần thực hiện hôm nay"));
            }

            // Lấy thông tin xe
            Integer vehicleId = driverJobs.get(0).getVehicleId();
            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
            if (vehicleOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorMessage("Không tìm thấy thông tin xe"));
            }

            Vehicle vehicle = vehicleOpt.get();
            BigDecimal remainingCapacity = vehicle.getTonnage();

            // Lấy các điểm thu gom cần vận chuyển (có trạng thái COMPLETED bởi người gom rác)
            List<JobRotation> collectionPoints = jobRotationRepository.findByRoleAndStatusAndRotationDate(
                    "COLLECTOR", "COMPLETED", LocalDate.now());

            // Sắp xếp các điểm theo khoảng cách gần nhất (giả định)
            List<JobPosition> optimizedPoints = new ArrayList<>();

            for (JobRotation point : collectionPoints) {
                Optional<JobPosition> positionOpt = jobPositionRepository.findById(point.getJobPositionId());
                if (positionOpt.isPresent()) {
                    JobPosition position = positionOpt.get();

                    // Giả định mỗi điểm có 1 tấn rác (1 small truck)
                    BigDecimal pointWeight = BigDecimal.ONE;

                    if (remainingCapacity.compareTo(pointWeight) >= 0) {
                        optimizedPoints.add(position);
                        remainingCapacity = remainingCapacity.subtract(pointWeight);

                        // Cập nhật trạng thái điểm này thành "IN_PROGRESS" để tránh phân công lại
                        point.setStatus("IN_PROGRESS");
                        jobRotationRepository.save(point);
                    }
                }

                if (remainingCapacity.compareTo(BigDecimal.ZERO) <= 0) {
                    break; // Đã hết tải trọng
                }
            }

            // Trả về danh sách các điểm tối ưu
            return ResponseEntity.ok(new DriverRouteResponse(
                    "Tối ưu hóa lộ trình thành công",
                    vehicle.getLicensePlate(),
                    vehicle.getTonnage(),
                    remainingCapacity,
                    optimizedPoints
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi khi tối ưu hóa lộ trình: " + e.getMessage()));
        }
    }

    // Các record và class hỗ trợ
    private record ResponseMessage(String message, Object data) {
        ResponseMessage(String message) {
            this(message, null);
        }
    }

    private record ErrorMessage(String error) {}
}