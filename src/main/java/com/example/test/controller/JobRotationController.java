package com.example.test.controller;

import com.example.test.dto.DriverJobWithCollectorStatusDTO;
import com.example.test.dto.JobRotationDetailDTO;
import com.example.test.dto.MarkCompletionRequest;
import com.example.test.dto.MarkCompletionResponse;
import com.example.test.entity.JobRotation;
import com.example.test.entity.Staff;
import com.example.test.repository.JobRotationRepository;
import com.example.test.repository.StaffRepository;
import com.example.test.service.JobRotationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

@RestController
@RequestMapping("/api/job-rotations")
public class JobRotationController {

    private final JobRotationRepository jobRotationRepository;
    private final JobRotationService jobRotationService;
    private final StaffRepository staffRepository;

    @Autowired
    public JobRotationController(JobRotationRepository jobRotationRepository,
                                 JobRotationService jobRotationService,
                                 StaffRepository staffRepository) {
        this.jobRotationRepository = jobRotationRepository;
        this.jobRotationService = jobRotationService;
        this.staffRepository = staffRepository;
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
    @PostMapping("/collector/completed")
    public ResponseEntity<MarkCompletionResponse> collectorMarkCompleted(
            @RequestBody MarkCompletionRequest request) {

        MarkCompletionResponse response = jobRotationService.markJobCompleted(request, "COLLECTOR");
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
//    @GetMapping("/driver")
//    public ResponseEntity<List<DriverJobWithCollectorStatusDTO>> getDriverJobs(
//            @RequestParam(value = "date", required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
//
//        String username = SecurityContextHolder.getContext().getAuthentication().getName();
//        System.out.println("Driver logged in username: " + username);
//
//        List<DriverJobWithCollectorStatusDTO> jobs;
//        if (date != null) {
//            jobs = jobRotationService.getDriverJobsWithCollectorStatusByDate(username, date);
//        } else {
//            jobs = jobRotationService.getDriverJobsWithCollectorStatusByDate(username, LocalDate.now());
//        }
//
//        return ResponseEntity.ok(jobs);
//    }
    private record ResponseMessage(String message, Object data) {
        ResponseMessage(String message) {
            this(message, null);
        }
    }

    private record ErrorMessage(String error) {}
}