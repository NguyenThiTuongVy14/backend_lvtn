package com.example.test.controller;

import com.example.test.cache.TruckCountCache;
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
import java.sql.Driver;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

            currentJob.setSmallTrucksCount(request.getSmallTrucksCount());
            currentJob.setStatus("COMPLETED");
            currentJob.setUpdatedAt(LocalDateTime.now());
            jobRotationRepository.save(currentJob);

            // 4. Tự động phân công tài xế
            assignDriverForCollectionPoint(
                    currentJob.getJobPositionId(),
                    request.getSmallTrucksCount(),
                    currentJob.getRotationDate(),
                    currentJob.getShiftId()
            );

            return ResponseEntity.ok(new ResponseMessage("Đã hoàn thành công việc gồm có "+request.getSmallTrucksCount()+"xe rác nhỏ và yêu cầu vận chuyển rác"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi khi đánh dấu hoàn thành: " + e.getMessage()));
        }
    }

    // Phương thức phân công tài xế
    private void assignDriverForCollectionPoint(
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

    @GetMapping("/driver/optimized-routes")
    public ResponseEntity<?> getOptimizedRoutesForDriver() {
        try {
            // 1. Xác thực tài xế
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Staff driver = staffRepository.findByUserName(username);

            // 2. Lấy công việc hiện tại của tài xế
            List<JobRotation> driverJobs = jobRotationRepository.findCurrentDriverJobs(
                    driver.getId(),
                    LocalDate.now(),
                    "PENDING"
            );

            if (driverJobs.isEmpty()) {
                return ResponseEntity.ok(new ResponseMessage("Không có công việc nào cần thực hiện hôm nay"));
            }

            // 3. Lấy thông tin xe
            Vehicle vehicle = vehicleRepository.findById(driverJobs.get(0).getVehicleId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin xe"));
            JobRotation currentDriverJob = driverJobs.get(0);

            Integer currentDriverShiftId = currentDriverJob.getShiftId();
            if (currentDriverShiftId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorMessage("Công việc của tài xế không có thông tin ca làm việc."));
            }
            // 4. Lấy các điểm thu gom đã hoàn thành với số xe rác nhỏ (small_trucks_count)
            List<Object[]> collectionPoints = jobRotationRepository.findCompletedCollectionsWithTruckCounts(LocalDate.now(),currentDriverShiftId );

            // 5. Tính toán lộ trình tối ưu
            List<RoutePoint> optimizedPoints = new ArrayList<>();
            BigDecimal remainingCapacity = vehicle.getTonnage();

            for (Object[] pointData : collectionPoints) {
                Integer jobRotationId = (Integer) pointData[0];
                Integer positionId = (Integer) pointData[1];
                Integer smallTrucksCount = (Integer) pointData[2]; // Số xe rác nhỏ
                BigDecimal wasteWeight = new BigDecimal(smallTrucksCount); // 1 xe = 1 tấn

                if (remainingCapacity.compareTo(wasteWeight) >= 0) {
                    JobPosition position = jobPositionRepository.findById(positionId)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy điểm thu gom"));

                    optimizedPoints.add(new RoutePoint(
                            positionId,
                            position.getName(),
                            position.getAddress(),
                            smallTrucksCount,
                            wasteWeight
                    ));

                    remainingCapacity = remainingCapacity.subtract(wasteWeight);

                    // Cập nhật trạng thái
                    jobRotationRepository.updateJobStatus(jobRotationId, "IN_PROGRESS");
                }

                if (remainingCapacity.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
            }

            return ResponseEntity.ok(new DriverRouteResponse(
                    "Tối ưu hóa lộ trình thành công",
                    vehicle.getLicensePlate(),
                    vehicle.getTonnage(),
                    remainingCapacity,
                    optimizedPoints
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi hệ thống: " + e.getMessage()));
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