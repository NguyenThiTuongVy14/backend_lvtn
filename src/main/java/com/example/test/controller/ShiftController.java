package com.example.test.controller;

import com.example.test.entity.Shift;
import com.example.test.repository.StaffRepository;
import com.example.test.service.ShiftService;
import com.example.test.service.StaffService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping; // Thêm import này
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam; // Thêm import này
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

    private final ShiftService shiftService;
    private final StaffRepository staffRepository;
    public ShiftController(ShiftService shiftService, StaffService staffService, StaffRepository staffRepository) {
        this.shiftService = shiftService;
        this.staffRepository = staffRepository;
    }

    @GetMapping
    public ResponseEntity<List<Shift>> getShifts() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String role = staffRepository.findAuthorityName(username);

        List<Shift> shifts = shiftService.getShiftsByRole(role);
        if (shifts.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(shifts);
    }
}