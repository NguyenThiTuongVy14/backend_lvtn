package com.example.test.controller;

import com.example.test.dto.MarkCompletionRequest;
import com.example.test.dto.MarkCompletionResponse;
import com.example.test.entity.JobRotation;
import com.example.test.service.CollectionPointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collection-points")
public class CollectionPointController {

    private final CollectionPointService collectionPointService;

    @Autowired
    public CollectionPointController(CollectionPointService collectionPointService) {
        this.collectionPointService = collectionPointService;
    }

    /**
     * API lấy danh sách lịch phân công của collector hiện đang đăng nhập
     */
    @GetMapping("/collector/schedules/{staffId}")
    public ResponseEntity<List<JobRotation>> getCollectorSchedules(@PathVariable Integer staffId) {
        List<JobRotation> schedules = collectionPointService.getCollectorSchedules(staffId);
        return ResponseEntity.ok(schedules);
    }

    /**
     * API lấy danh sách lịch phân công của driver hiện đang đăng nhập
     */
    @GetMapping("/driver/schedules/{staffId}")
    public ResponseEntity<List<JobRotation>> getDriverSchedules(@PathVariable Integer staffId) {
        List<JobRotation> schedules = collectionPointService.getDriverSchedules(staffId);
        return ResponseEntity.ok(schedules);
    }
}