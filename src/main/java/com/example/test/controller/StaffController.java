package com.example.test.controller;

import com.example.test.dto.StaffDTO;
import com.example.test.entity.Staff;
import com.example.test.repository.StaffRepository;
import com.example.test.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffRepository staffRepository;
    private final StaffService staffService;

    @Autowired
    public StaffController(StaffRepository staffRepository, StaffService staffService) {
        this.staffRepository = staffRepository;
        this.staffService = staffService;
    }

    // Tạo nhân viên mới (chỉ ADMIN)
    @PostMapping
    public ResponseEntity<?> createStaff(@RequestBody Staff staff) {
        try {
            Staff savedStaff = staffService.createStaff(staff);
            return ResponseEntity.ok("{\"message\": \"Staff created successfully\", \"id\": " + savedStaff.getId() + "}");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error creating staff: " + e.getMessage() + "\"}");
        }
    }

    // Lấy danh sách tất cả nhân viên (chỉ ADMIN)
    @GetMapping
    public ResponseEntity<?> getAllStaff() {
        try {
            List<Staff> staffList = staffRepository.findAll();

            // Convert sang DTO để ẩn password
            List<StaffDTO> staffDTOList = staffList.stream()
                    .map(StaffDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(staffDTOList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error retrieving staff list: " + e.getMessage() + "\"}");
        }
    }

    // Lấy thông tin nhân viên theo ID (ADMIN hoặc chính nhân viên đó)
    @GetMapping("/{id}")
    public ResponseEntity<?> getStaffById(@PathVariable Integer id) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Staff currentStaff = staffRepository.findByUserName(username);

            // Chỉ ADMIN hoặc chính nhân viên đó mới có thể xem thông tin
            if (currentStaff == null || (!currentStaff.getAuthorityId().equals(3) && !currentStaff.getId().equals(id))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("{\"error\": \"Access denied\"}");
            }

            Optional<Staff> staff = staffRepository.findById(id);
            Optional<StaffDTO> staffDTO = staff.map(StaffDTO::new);
            if (staff.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("{\"error\": \"Staff not found\"}");
            }

            return ResponseEntity.ok(staffDTO.get());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error retrieving staff: " + e.getMessage() + "\"}");
        }
    }

    // Cập nhật thông tin nhân viên (chỉ ADMIN)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateStaff(@PathVariable Integer id, @RequestBody Staff staff) {
        try {
            Staff updatedStaff = staffService.updateStaff(id, staff);
            return ResponseEntity.ok("{\"message\": \"Staff updated successfully\", \"id\": " + updatedStaff.getId() + "}");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error updating staff: " + e.getMessage() + "\"}");
        }
    }

    // Xóa nhân viên (chỉ ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStaff(@PathVariable Integer id) {
        try {
            staffService.deleteStaff(id);
            return ResponseEntity.ok("{\"message\": \"Staff deleted successfully\"}");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error deleting staff: " + e.getMessage() + "\"}");
        }
    }

    // Lấy danh sách nhân viên theo vai trò (chỉ ADMIN)
    @GetMapping("/role/{role}")
    public ResponseEntity<?> getStaffByRole(@PathVariable String role) {
        try {
            List<Staff> staffList = staffRepository.findStaffByAuthorityName(role);
            return ResponseEntity.ok(staffList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error retrieving staff by role: " + e.getMessage() + "\"}");
        }
    }

    // Lấy thông tin profile của nhân viên hiện tại
    @GetMapping("/profile")
    public ResponseEntity<?> getCurrentStaffProfile() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Staff staff = staffRepository.findByUserName(username);

            if (staff == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("{\"error\": \"Invalid user\"}");
            }

            return ResponseEntity.ok(staff);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error retrieving profile: " + e.getMessage() + "\"}");
        }
    }

    // Cập nhật thông tin profile của nhân viên hiện tại
    @PutMapping("/profile")
    public ResponseEntity<?> updateCurrentStaffProfile(@RequestBody Staff staffUpdate) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Staff currentStaff = staffRepository.findByUserName(username);

            if (currentStaff == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("{\"error\": \"Invalid user\"}");
            }

            Staff updatedStaff = staffService.updateStaffProfile(currentStaff.getId(), staffUpdate);
            return ResponseEntity.ok("{\"message\": \"Profile updated successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error updating profile: " + e.getMessage() + "\"}");
        }
    }
}