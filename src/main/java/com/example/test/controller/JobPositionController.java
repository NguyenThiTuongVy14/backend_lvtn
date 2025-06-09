package com.example.test.controller;

import com.example.test.entity.JobPosition;
import com.example.test.repository.JobPositionRepository;
import com.example.test.repository.StaffRepository;
import com.example.test.service.JobPositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/job-positions")
public class JobPositionController {

    private final JobPositionRepository jobPositionRepository;
    private final JobPositionService jobPositionService;

    @Autowired
    public JobPositionController(JobPositionRepository jobPositionRepository,
                                 JobPositionService jobPositionService,
                                 StaffRepository staffRepository) {
        this.jobPositionRepository = jobPositionRepository;
        this.jobPositionService = jobPositionService;

    }

    // Tạo vị trí công việc mới (chỉ ADMIN)
    @PostMapping
    public ResponseEntity<?> createJobPosition(@RequestBody JobPosition jobPosition) {
        try {
            JobPosition savedPosition = jobPositionService.createJobPosition(jobPosition);
            return ResponseEntity.ok("{\"message\": \"Job position created successfully\", \"id\": " + savedPosition.getId() + "}");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error creating job position: " + e.getMessage() + "\"}");
        }
    }

    // Lấy danh sách tất cả vị trí công việc (chỉ ADMIN)
    @GetMapping
    public ResponseEntity<?> getAllJobPositions() {
        try {
            List<JobPosition> positions = jobPositionRepository.findAll();
            return ResponseEntity.ok(positions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error retrieving job positions: " + e.getMessage() + "\"}");
        }
    }

    // Lấy vị trí công việc theo ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getJobPositionById(@PathVariable Integer id) {
        try {
            Optional<JobPosition> position = jobPositionRepository.findById(id);
            if (position.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("{\"error\": \"Job position not found\"}");
            }
            return ResponseEntity.ok(position.get());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error retrieving job position: " + e.getMessage() + "\"}");
        }
    }

    // Cập nhật vị trí công việc (chỉ ADMIN)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateJobPosition(@PathVariable Integer id, @RequestBody JobPosition jobPosition) {
        try {
            JobPosition updatedPosition = jobPositionService.updateJobPosition(id, jobPosition);
            return ResponseEntity.ok("{\"message\": \"Job position updated successfully\", \"id\": " + updatedPosition.getId() + "}");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error updating job position: " + e.getMessage() + "\"}");
        }
    }

    // Xóa vị trí công việc (chỉ ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJobPosition(@PathVariable Integer id) {
        try {
            jobPositionService.deleteJobPosition(id);
            return ResponseEntity.ok("{\"message\": \"Job position deleted successfully\"}");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error deleting job position: " + e.getMessage() + "\"}");
        }
    }


    // Tìm vị trí công việc gần nhất theo tọa độ
    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyJobPositions(@RequestParam BigDecimal lat,
                                                   @RequestParam BigDecimal lng,
                                                   @RequestParam(defaultValue = "5000") Double radius) {
        try {
            List<JobPosition> nearbyPositions = jobPositionService.findNearbyPositions(lat, lng, radius);
            return ResponseEntity.ok(nearbyPositions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error retrieving nearby positions: " + e.getMessage() + "\"}");
        }
    }


}