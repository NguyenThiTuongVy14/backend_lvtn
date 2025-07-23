package com.example.test.controller;

import com.example.test.dto.StaffRegisterRequest;
import com.example.test.service.StaffRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/register")
@RequiredArgsConstructor
public class StaffRequestController {
    private final StaffRequestService staffRequestService;

    @PostMapping("/driver")
    public ResponseEntity<String> registerDriver(@RequestBody StaffRegisterRequest request) {
        return ResponseEntity.ok(staffRequestService.register(request, 2));
    }

    @PostMapping("/collector")
    public ResponseEntity<String> registerCollector(@RequestBody StaffRegisterRequest request) {
        return ResponseEntity.ok(staffRequestService.register(request, 1));
    }
}
