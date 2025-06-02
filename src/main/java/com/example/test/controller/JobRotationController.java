package com.example.test.controller;

import com.example.test.entity.JobRotation;
import com.example.test.entity.Staff;
import com.example.test.repository.JobRotationRepository;
import com.example.test.repository.StaffRepository;
import com.example.test.service.JobRotationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

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

    // Tạo lịch phân công mới (chỉ ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> createJobRotation(@RequestBody JobRotation jobRotation) {
        try {
            JobRotation savedRotation = jobRotationService.createJobRotation(jobRotation);
            return ResponseEntity.ok(new ResponseMessage("Lịch phân công được tạo thành công", savedRotation.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorMessage(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi khi tạo lịch phân công: " + e.getMessage()));
        }
    }

    // Lấy danh sách tất cả lịch phân công (chỉ ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN') or hasRole('COLLECTOR') or hasRole('DRIVER')")
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

    // Cập nhật lịch phân công (chỉ ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateJobRotation(@PathVariable Integer id, @RequestBody JobRotation jobRotation) {
        try {
            JobRotation updatedRotation = jobRotationService.updateJobRotation(id, jobRotation);
            return ResponseEntity.ok(new ResponseMessage("Lịch phân công được cập nhật thành công", updatedRotation.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorMessage(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi khi cập nhật lịch phân công: " + e.getMessage()));
        }
    }

    // Xóa lịch phân công (chỉ ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJobRotation(@PathVariable Integer id) {
        try {
            jobRotationService.deleteJobRotation(id);
            return ResponseEntity.ok(new ResponseMessage("Lịch phân công đã được xóa thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorMessage(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi khi xóa lịch phân công: " + e.getMessage()));
        }
    }

    // Lấy lịch phân công của nhân viên theo ID (chỉ ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
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

    // Lấy lịch phân công của nhân viên đang đăng nhập
    @PreAuthorize("hasRole('COLLECTOR') or hasRole('DRIVER')")
    @GetMapping("/my-rotations")
    public ResponseEntity<?> getMyJobRotations() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Staff staff = staffRepository.findByUserName(username);

            if (staff == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorMessage("Người dùng không hợp lệ"));
            }

            List<JobRotation> rotations = jobRotationRepository.findByStaffId(staff.getId());
            return ResponseEntity.ok(rotations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi khi lấy lịch phân công của bạn: " + e.getMessage()));
        }
    }

    // Lấy lịch phân công đang hoạt động của nhân viên đang đăng nhập
    @PreAuthorize("hasRole('COLLECTOR') or hasRole('DRIVER')")
    @GetMapping("/my-active-rotations")
    public ResponseEntity<?> getMyActiveJobRotations() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Staff staff = staffRepository.findByUserName(username);

            if (staff == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorMessage("Người dùng không hợp lệ"));
            }

            List<JobRotation> activeRotations = jobRotationRepository.findByStaffIdAndStatus(staff.getId(), "ACTIVE");
            return ResponseEntity.ok(activeRotations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi khi lấy lịch phân công đang hoạt động: " + e.getMessage()));
        }
    }

    // Lấy lịch phân công theo trạng thái (chỉ ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getJobRotationsByStatus(@PathVariable String status) {
        try {
            List<JobRotation> rotations = jobRotationRepository.findByStatus(status);
            return ResponseEntity.ok(rotations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi khi lấy lịch phân công theo trạng thái: " + e.getMessage()));
        }
    }

    // Cập nhật trạng thái lịch phân công (chỉ ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateJobRotationStatus(@PathVariable Integer id, @RequestParam String status) {
        try {
            JobRotation updatedRotation = jobRotationService.updateJobRotationStatus(id, status);
            return ResponseEntity.ok(new ResponseMessage("Trạng thái lịch phân công được cập nhật thành công", updatedRotation.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorMessage(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi khi cập nhật trạng thái lịch phân công: " + e.getMessage()));
        }
    }

    // Tự động phân công lịch phân công (chỉ ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/auto-assign")
    public ResponseEntity<?> autoAssignJobRotations() {
        try {
            List<JobRotation> assignedRotations = jobRotationService.autoAssignJobRotations();
            return ResponseEntity.ok(new ResponseMessage("Phân công tự động hoàn tất", assignedRotations.size()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorMessage(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi khi phân công tự động: " + e.getMessage()));
        }
    }

    // Lấy thống kê lịch phân công (chỉ ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
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

    // Đếm số lượng lịch phân công theo trạng thái (chỉ ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/count/status")
    public ResponseEntity<?> getJobRotationCountByStatus() {
        try {
            List<Object[]> counts = jobRotationRepository.countByStatus();
            return ResponseEntity.ok(counts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Lỗi khi đếm lịch phân công theo trạng thái: " + e.getMessage()));
        }
    }

    // Lớp hỗ trợ định dạng phản hồi
    private record ResponseMessage(String message, Object data) {
        ResponseMessage(String message) {
            this(message, null);
        }
    }

    private record ErrorMessage(String error) {}
}