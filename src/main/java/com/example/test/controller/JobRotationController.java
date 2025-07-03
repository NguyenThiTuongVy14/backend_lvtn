package com.example.test.controller;

import com.example.test.dto.*;
import com.example.test.entity.*;
import com.example.test.repository.*;
import com.example.test.service.JobRotationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


@RestController
@RequestMapping("/api/job-rotations")
public class JobRotationController {

    private final JobRotationRepository jobRotationRepository;
    private final JobRotationService jobRotationService;
    private final StaffRepository staffRepository;
    private final ShiftRepository shiftRepository;
    private final VehicleRepository  vehicleRepository;
    private final RotationLogRepository  rotationLogRepository;
    private final JobPositionRepository jobPositionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public JobRotationController(JobRotationRepository jobRotationRepository,
                                 JobRotationService jobRotationService,
                                 StaffRepository staffRepository, ShiftRepository shiftRepository, VehicleRepository vehicleRepository, RotationLogRepository rotationLogRepository, DriverRatingRepository driverRatingRepository, JobPositionRepository jobPositionRepository, SimpMessagingTemplate messagingTemplate) {
        this.jobRotationRepository = jobRotationRepository;
        this.jobRotationService = jobRotationService;
        this.staffRepository = staffRepository;
        this.shiftRepository = shiftRepository;
        this.vehicleRepository = vehicleRepository;
        this.rotationLogRepository = rotationLogRepository;
        this.jobPositionRepository = jobPositionRepository;
        this.messagingTemplate = messagingTemplate;
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

    @Transactional
    @PostMapping("/driver/register-shift")
    public ResponseEntity<?> registerDriverShift(@RequestBody List<DriverShiftRegistrationRequest> requests) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Staff driver = staffRepository.findByUserName(username);
            Integer staffId = driver.getId();

            for (DriverShiftRegistrationRequest request : requests) {
                for (Integer shiftId : request.getShiftId()) {
                    if (rotationLogRepository.existsByStaffIdAndShiftIdAndRotationDate(
                            staffId, shiftId, request.getRotationDate())) {
                        throw new IllegalStateException("Đã đăng ký ca " + shiftId +
                                " vào ngày " + request.getRotationDate());
                    }

                    RotationLog log = new RotationLog();
                    log.setStaffId(staffId);
                    log.setShiftId(shiftId);
                    log.setStatus("REQUEST");
                    log.setRequestedAt(LocalDateTime.now());
                    log.setRotationDate(request.getRotationDate());

                    rotationLogRepository.save(log);
                }
            }

            return ResponseEntity.ok(new ResponseMessage("Đăng ký ca làm việc thành công. Đang chờ xác nhận"));

        } catch (IllegalStateException e) {
            // rollback tự động do @Transactional
            return ResponseEntity.badRequest().body(new ErrorMessage(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi hệ thống: " + e.getMessage()));
        }
    }


    @PostMapping("/collector/completed")
    public ResponseEntity<?> collectorMarkCompleted(@RequestBody CollectorCompletionRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Staff collector = staffRepository.findByUserName(username);

        Optional<JobRotation> jobOpt = jobRotationRepository.findById(request.getJobRotationId());
        if (jobOpt.isEmpty() || !jobOpt.get().getStaffId().equals(collector.getId())) {
            return ResponseEntity.badRequest().body(new ErrorMessage("Công việc không hợp lệ hoặc không thuộc về bạn"));
        }

        JobRotation job = jobOpt.get();
        job.setSmallTrucksCount(request.getSmallTrucksCount());
        job.setStatus("COMPLETED");
        job.setUpdatedAt(LocalDateTime.now());
        jobRotationRepository.save(job);

        // Quy đổi số xe nhỏ ra tấn
        BigDecimal tonnagePerSmallTruck = new BigDecimal("0.5"); // 500kg
        BigDecimal totalCollectedTonnage = tonnagePerSmallTruck.multiply(new BigDecimal(request.getSmallTrucksCount()));

        // Điều xe lớn
        jobRotationService.assignVehiclesForCollection(job.getJobPositionId(), totalCollectedTonnage, job.getRotationDate(), job.getShiftId());

        return ResponseEntity.ok(new ResponseMessage("Đã hoàn thành " + request.getSmallTrucksCount() + " xe đẩy nhỏ (~" + totalCollectedTonnage + " tấn)"));
    }


    @PostMapping("/driver/completed")
    public ResponseEntity<?> driverMarkCompleted(@RequestBody MarkCompletionRequest request) {
        if (request.getJobRotationId() == null || request.getTonnage() == null) {
            return ResponseEntity.badRequest().body(new ErrorMessage("Thiếu thông tin jobRotationId hoặc tonnage"));
        }

        try {
            // Lấy thông tin user hiện tại
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Staff currentDriver = staffRepository.findByUserName(username);

            if (currentDriver == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorMessage("Không tìm thấy thông tin tài xế"));
            }

            MarkCompletionResponse response = jobRotationService.markDriverJobCompleted(request, currentDriver.getId());
            return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @GetMapping("/driver/optimized-routes")
    public ResponseEntity<?> getOptimizedRoutesForDriver() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Staff driver = staffRepository.findByUserName(username);

        List<JobRotation> jobs = jobRotationRepository.findCurrentDriverJobs(driver.getId(), LocalDate.now(), "PENDING");
        if (jobs.isEmpty()) {
            return ResponseEntity.ok(new ResponseMessage("Không có công việc cần thực hiện hôm nay"));
        }

        Vehicle vehicle = vehicleRepository.findById(jobs.get(0).getVehicleId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin xe"));

        Set<JobPosition> assignedPositions = new HashSet<>();
        for (JobRotation job : jobs) {
            JobPosition position = jobPositionRepository.findById(job.getJobPositionId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy điểm thu gom"));
            assignedPositions.add(position);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tối ưu hóa lộ trình thành công");
        response.put("vehicle", vehicle);
        response.put("positions", assignedPositions);

        return ResponseEntity.ok(response);
    }
    // Các record và class hỗ trợ
    private record ResponseMessage(String message, Object data) {
        ResponseMessage(String message) {
            this(message, null);
        }
    }

    private record ErrorMessage(String error) {}
}