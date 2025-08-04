package com.example.test.controller;

import com.example.test.dto.*;
import com.example.test.entity.*;
import com.example.test.repository.*;
import com.example.test.service.JobRotationService;
import com.example.test.service.PriorityAllocatorService;
import com.example.test.service.RouteOptimizationService;
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
    private final JobRotationTempRepository jobRotationTempRepository;
    private final PriorityAllocatorService  priorityAllocatorService;
    private final RouteOptimizationService routeOptimizationService;
    private final RouteRepository routeRepository;
    @Autowired
    public JobRotationController(JobRotationRepository jobRotationRepository,
                                 JobRotationService jobRotationService,
                                 StaffRepository staffRepository, ShiftRepository shiftRepository, VehicleRepository vehicleRepository, RotationLogRepository rotationLogRepository, DriverRatingRepository driverRatingRepository, JobPositionRepository jobPositionRepository, SimpMessagingTemplate messagingTemplate, JobRotationTempRepository jobRotationTempRepository, PriorityAllocatorService priorityAllocatorService, RouteOptimizationService routeOptimizationService, RouteRepository routeRepository) {
        this.jobRotationRepository = jobRotationRepository;
        this.jobRotationService = jobRotationService;
        this.staffRepository = staffRepository;
        this.shiftRepository = shiftRepository;
        this.vehicleRepository = vehicleRepository;
        this.rotationLogRepository = rotationLogRepository;
        this.jobPositionRepository = jobPositionRepository;
        this.messagingTemplate = messagingTemplate;
        this.jobRotationTempRepository = jobRotationTempRepository;
        this.priorityAllocatorService = priorityAllocatorService;
        this.routeOptimizationService = routeOptimizationService;
        this.routeRepository = routeRepository;
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
//    @GetMapping("/{id}")
//    public ResponseEntity<?> getJobRotationById(@PathVariable Integer id) {
//        try {
//            Optional<JobRotation> rotation = jobRotationRepository.findById(id);
//            if (rotation.isEmpty()) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                        .body(new ErrorMessage("Lịch phân công không tìm thấy"));
//            }
//
//            // Kiểm tra quyền truy cập
//            String username = SecurityContextHolder.getContext().getAuthentication().getName();
//            Staff currentUser = staffRepository.findByUserName(username);
//            if (currentUser == null) {
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                        .body(new ErrorMessage("Người dùng không hợp lệ"));
//            }
//
//            // Nếu không phải ADMIN, chỉ cho phép xem lịch của chính mình
//            Optional<String> authority = staffRepository.findAuthorityNameByStaffId(currentUser.getId());
//            if (!authority.orElse("").equals("ADMIN") &&
//                    !rotation.get().getStaffId().equals(currentUser.getId())) {
//                return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                        .body(new ErrorMessage("Không có quyền truy cập"));
//            }
//
//            return ResponseEntity.ok(rotation.get());
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(new ErrorMessage("Lỗi khi lấy lịch phân công: " + e.getMessage()));
//        }
//    }


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
        rotations = jobRotationService.getJobRotationsByDate(username, date);
        return ResponseEntity.ok(rotations);
    }

    @Transactional
    @PostMapping("/driver/register-shift")
    public ResponseEntity<?> registerDriverShift(@RequestBody List<DriverShiftRegistrationRequest> requests) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Staff driver = staffRepository.findByUserName(username);
            Integer staffId = driver.getId();

            List<String> messages = new ArrayList<>();

            for (DriverShiftRegistrationRequest request : requests) {
                LocalDate date = request.getRotationDate();

                // B1: Đẩy WAITLIST có carry_points cao lên trước (nếu còn slot)
                priorityAllocatorService.promoteWaitlistIfAny(date);

                for (Integer shiftId : request.getShiftId()) {
                    if (rotationLogRepository.existsByStaffIdAndShiftIdAndRotationDate(
                            staffId, shiftId, date)) {
                        throw new IllegalStateException("Đã đăng ký ca " + shiftId + " vào ngày " + date);
                    }

                    long currentAssigned = rotationLogRepository.countAssignedForUpdate(date);
                    int capacity = (int) vehicleRepository.count();

                    RotationLog log = new RotationLog();
                    log.setStaffId(staffId);
                    log.setShiftId(shiftId);
                    log.setRequestedAt(LocalDateTime.now());
                    log.setRotationDate(date);

                    if (currentAssigned < capacity) {
                        log.setStatus("ASSIGNED");
                        log.setUpdatedAt(LocalDateTime.now());
                        messages.add("Ngày " + date + " (ca " + shiftId + "): ĐÃ ĐƯỢC PHÂN CÔNG.");
                    } else {
                        log.setStatus("WAITLIST");
                        messages.add("Ngày " + date + " (ca " + shiftId + "): HẾT CHỖ, bạn đang ở WAITLIST.");
                    }

                    rotationLogRepository.save(log);
                }
            }

            return ResponseEntity.ok(new ResponseMessage(String.join("\n", messages)));

        } catch (IllegalStateException e) {
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
        Optional<JobRotationTemp> jobOpt = jobRotationTempRepository.findByIdAndStaffId(request.getJobRotationId(), collector.getId());
        if (jobOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorMessage("Công việc không hợp lệ hoặc không thuộc về bạn"));
        }
        if (jobOpt.get().getStatus().equals("COMPLETED")) {
            return ResponseEntity.badRequest().body(new ErrorMessage("Công việc này đã được hoàn thành"));
        }
        JobRotationTemp job = jobOpt.get();
        job.setSmallTrucksCount(request.getSmallTrucksCount());
        job.setTonnage(BigDecimal.valueOf(request.getSmallTrucksCount()*0.5));
        job.setStatus("COMPLETED");
        job.setUpdatedAt(LocalDateTime.now());
        jobRotationTempRepository.save(job);

        JobUpdateDTO message = new JobUpdateDTO();
        message.setJobRotationId(job.getId());
        message.setFullName(collector.getFullName());
        Shift shift = shiftRepository.findById(job.getShiftId())
                .orElse(null);
        message.setShift(shift.getName());
        message.setTonnage(job.getTonnage());
        message.setStatus(job.getStatus());
        messagingTemplate.convertAndSend("/topic/job-collector-updates", message);
        return ResponseEntity.ok(new ResponseMessage("Đã hoàn thành " + request.getSmallTrucksCount() + " xe đẩy nhỏ (~" + request.getSmallTrucksCount()*0.5 + " tấn)"));
    }

    @GetMapping("/assign-vehicle")
    public ResponseEntity<?> assignVehicle(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = (date != null) ? date : LocalDate.now();
        int result = jobRotationService.assignAllVehicles(target);
        if (result == 1) {
            return ResponseEntity.ok(new ResponseMessage("Đã phân công xe cho tài xế ASSIGNED"));

        } else if (result == -1) {
            return ResponseEntity.badRequest().body(new ErrorMessage("Không đủ tài xế"));
        } else {
            return ResponseEntity.badRequest().body(new ErrorMessage("Không đủ xe"));
        }

    }




    @GetMapping("/test-promote")
    public ResponseEntity<?> testPromote(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = (date != null) ? date : LocalDate.now();
        priorityAllocatorService.promoteWaitlistIfAny(target);
        return ResponseEntity.ok("Đã promote WAITLIST -> ASSIGNED cho ngày " + target);
    }
    @PostMapping("/test-finalize")
    public ResponseEntity<?> testFinalize(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        if (date == null) {
            date = LocalDate.now(); // mặc định hôm nay
        }
        int moved = jobRotationService.finalizeDailyAssignments(date);
        return ResponseEntity.ok(new ResponseMessage("Đã finalize " + moved + " bản ghi cho ngày " + date));
    }
    private record JobCollectorCompletedMessage(Integer jobId, String status, BigDecimal totalTonnage,JobPosition position ) {}

    @PostMapping("/driver/completed")
    public ResponseEntity<?> driverMarkCompleted(@RequestBody MarkCompletionRequest request) {
        if (request.getJobRotationId() == null) {
            return ResponseEntity.badRequest().body(new ErrorMessage("Thiếu thông tin jobRotationId hoặc tonnage"));
        }
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Staff currentDriver = staffRepository.findByUserName(username);
            if (currentDriver == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorMessage("Không tìm thấy thông tin tài xế"));
            }
            MarkCompletionResponse response = jobRotationService.markDriverJobCompleted(request, currentDriver);
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

        List<JobRotationTemp> jobs = jobRotationTempRepository.findByStaffId(driver.getId());
        if (jobs.isEmpty()) {
            return ResponseEntity.ok(new ResponseMessage("Không có công việc cần thực hiện"));
        }

        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(jobs.get(0).getVehicleId());
        if (vehicleOpt.isEmpty()) {
            return ResponseEntity.ok(new ResponseMessage("Xe không hợp lệ"));
        }
        Vehicle vehicle = vehicleOpt.get();
        List<Route> routes = routeRepository.findByVehicleId(vehicle.getId());
        List<DriverRouteResponse> optimizedRoutes = new ArrayList<>();
        optimizedRoutes.add(newDriverRoute(routes.getFirst(),999, null));
        for (Route route : routes) {
            Optional<JobRotationTemp> jobOpt = jobs.stream()
                    .filter(job -> job.getId().equals(route.getRotationId()))
                    .findFirst();

            if (jobOpt.isPresent()) {
                JobRotationTemp job = jobOpt.get();
                Optional<JobPosition> positionOpt = jobPositionRepository.findById(job.getJobPositionId());
                positionOpt.ifPresent(jobPosition -> optimizedRoutes.add(newDriverRoute(route, jobPosition.getId(),job.getStatus())));
            }


        }
        optimizedRoutes.add(newDriverRoute(routes.getLast(),1000, null));

        Map<String, Object> response = new HashMap<>();
        response.put("vehicle", vehicle);
        response.put("routes", optimizedRoutes);
        response.put("message", "Tối ưu hóa lộ trình thành công");

        return ResponseEntity.ok(response);
    }
    private DriverRouteResponse newDriverRoute(Route route, Integer positionID, String status) {
        Optional<JobPosition> jobPosition = jobPositionRepository.findById(positionID);
        JobPositionDTO dto = new JobPositionDTO();
        dto.setId(jobPosition.get().getId());
        dto.setAddress(jobPosition.get().getAddress());
        dto.setLat(jobPosition.get().getLat());
        dto.setLng(jobPosition.get().getLng());
        dto.setName(jobPosition.get().getName());
        dto.setIndex(route.getIndex());
        dto.setArrival(route.getArrival());
        dto.setType(route.getType());
        return new DriverRouteResponse(route.getRotationId(), dto, status);
    }

    // Các record và class hỗ trợ
    private record ResponseMessage(String message, Object data) {
        ResponseMessage(String message) {
            this(message, null);
        }
    }

    private record ErrorMessage(String error) {}
}