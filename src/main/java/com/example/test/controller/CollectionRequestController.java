package com.example.test.controller;

import com.example.test.dto.UpdateStatusRequest;
import com.example.test.entity.CollectionRequest;
import com.example.test.entity.Staff;
import com.example.test.repository.CollectionRequestRepository;
import com.example.test.repository.StaffRepository;
import com.example.test.service.CollectionRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collection-requests")
public class CollectionRequestController {

    private final CollectionRequestRepository collectionRequestRepository;
    private final StaffRepository staffRepository;
    private final CollectionRequestService collectionRequestService;

    @Autowired
    public CollectionRequestController(CollectionRequestRepository collectionRequestRepository,
                                       StaffRepository staffRepository,
                                       CollectionRequestService collectionRequestService) {
        this.collectionRequestRepository = collectionRequestRepository;
        this.staffRepository = staffRepository;
        this.collectionRequestService = collectionRequestService;
    }

    // Tạo và phân công yêu cầu thu gom (chỉ ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> createCollectionRequest(@RequestBody CollectionRequest request) {
        try {
            CollectionRequest savedRequest = collectionRequestService.createAndAssignCollectionRequest(request);
            return ResponseEntity.ok("{\"message\": \"Collection request created and assigned successfully\", \"id\": " + savedRequest.getId() + "}");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error creating collection request: " + e.getMessage() + "\"}");
        }
    }

    // Lấy danh sách yêu cầu thu gom được phân công cho COLLECTOR
    @PreAuthorize("hasRole('COLLECTOR')")
    @GetMapping("/collector/{collectorId}")
    public ResponseEntity<?> getAssignedCollectionRequests(@PathVariable Integer collectorId) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Staff staff = staffRepository.findByUserName(username);
            if (staff == null || !staff.getId().equals(collectorId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("{\"error\": \"You can only access your own assigned requests\"}");
            }

            List<CollectionRequest> requests = collectionRequestRepository.findByCollectorId(collectorId);
            if (requests.isEmpty()) {
                return ResponseEntity.ok("{\"message\": \"No assigned collection requests found\"}");
            }

            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error retrieving assigned requests: " + e.getMessage() + "\"}");
        }
    }

    // Cập nhật trạng thái yêu cầu thu gom (chỉ COLLECTOR)
    @PreAuthorize("hasRole('COLLECTOR')")
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateCollectionRequestStatus(@PathVariable Integer id, @RequestBody UpdateStatusRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Staff staff = staffRepository.findByUserName(username);
            if (staff == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("{\"error\": \"Invalid user\"}");
            }

            CollectionRequest updatedRequest = collectionRequestService.updateCollectionRequestStatus(id, request, staff);
            return ResponseEntity.ok("{\"message\": \"Collection request status updated successfully\", \"id\": " + updatedRequest.getId() + "}");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error updating collection request: " + e.getMessage() + "\"}");
        }
    }

    // Đếm số lượng yêu cầu thu gom theo trạng thái (chỉ ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/count/status")
    public ResponseEntity<?> getCollectionRequestCountByStatus() {
        try {
            List<Object[]> counts = collectionRequestRepository.countByStatus();
            return ResponseEntity.ok(counts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error retrieving collection request counts: " + e.getMessage() + "\"}");
        }
    }
}