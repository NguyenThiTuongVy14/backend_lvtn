package com.example.test.controller;

import com.example.test.dto.DriverMarkCompletionRequest;
import com.example.test.dto.DriverMarkCompletionResponse;
import com.example.test.dto.MarkCompletionRequest;
import com.example.test.dto.MarkCompletionResponse;
import com.example.test.entity.JobRotation;
import com.example.test.repository.StaffRepository;
import com.example.test.service.CollectionPointService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/collection-points")
public class CollectionPointController {

    private final CollectionPointService collectionPointService;
    private final StaffRepository staffRepository;

    @Autowired
    public CollectionPointController(CollectionPointService collectionPointService,
                                     StaffRepository staffRepository) {
        this.collectionPointService = collectionPointService;
        this.staffRepository = staffRepository;
    }

    /**
     * API đánh dấu hoàn thành điểm thu gom cho collector
     */
    @PostMapping("/mark-completed")
    public ResponseEntity<MarkCompletionResponse> markCollectionPointCompleted(
            @RequestBody MarkCompletionRequest request) {

        // Validation
        if (request.getJobRotationId() == null) {
            MarkCompletionResponse errorResponse = new MarkCompletionResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Job rotation ID không được để trống");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        MarkCompletionResponse response = collectionPointService.markCollectionPointCompleted(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    /**
     * API lấy danh sách công việc của collector theo ngày
     */
    @GetMapping("/collector-jobs/{staffId}")
    public ResponseEntity<List<JobRotation>> getCollectorJobs(
            @PathVariable Integer staffId,
            @RequestParam(required = false) String date) {

        try {
            Date rotationDate = null;
            if (date != null && !date.isEmpty()) {
                rotationDate = java.sql.Date.valueOf(date); // Format: YYYY-MM-DD
            }

            List<JobRotation> jobs = collectionPointService.getCollectorJobs(staffId, rotationDate);
            return ResponseEntity.ok(jobs);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    /**
     * API lấy danh sách công việc đang pending của collector
     */
    @GetMapping("/collector-pending-jobs/{staffId}")
    public ResponseEntity<List<JobRotation>> getPendingCollectorJobs(@PathVariable Integer staffId) {

        List<JobRotation> pendingJobs = collectionPointService.getPendingCollectorJobs(staffId);
        return ResponseEntity.ok(pendingJobs);
    }

//    @PostMapping("/driver/mark-completed")
//    public ResponseEntity<DriverMarkCompletionResponse> driverMarkCompleted(
//            @RequestBody DriverMarkCompletionRequest request) {
//
//        // Validation
//        if (request.getJobRotationId() == null) {
//            DriverMarkCompletionResponse errorResponse = new DriverMarkCompletionResponse();
//            errorResponse.setSuccess(false);
//            errorResponse.setMessage("Job rotation ID không được để trống");
//            return ResponseEntity.badRequest().body(errorResponse);
//        }
//
//        if (request.getJobPositionId() == null) {
//            DriverMarkCompletionResponse errorResponse = new DriverMarkCompletionResponse();
//            errorResponse.setSuccess(false);
//            errorResponse.setMessage("Job position ID không được để trống");
//            return ResponseEntity.badRequest().body(errorResponse);
//        }
//
//        if (request.getShiftId() == null) {
//            DriverMarkCompletionResponse errorResponse = new DriverMarkCompletionResponse();
//            errorResponse.setSuccess(false);
//            errorResponse.setMessage("Shift ID không được để trống");
//            return ResponseEntity.badRequest().body(errorResponse);
//        }
//
//        DriverMarkCompletionResponse response = collectionPointService.driverMarkCompleted(request);
//
//        if (response.isSuccess()) {
//            return ResponseEntity.ok(response);
//        } else {
//            return ResponseEntity.badRequest().body(response);
//        }
//    }

}