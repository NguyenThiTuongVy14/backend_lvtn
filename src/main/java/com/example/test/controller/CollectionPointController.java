//package com.example.test.controller;
//
//import com.example.test.dto.UpdateStatusRequest;
//import com.example.test.entity.JobPosition;
//import com.example.test.entity.JobRotation;
//import com.example.test.entity.Staff;
//import com.example.test.repository.JobPositionRepository;
//import com.example.test.repository.JobRotationRepository;
//import com.example.test.repository.StaffRepository;
//import com.example.test.service.CollectionPointService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.bind.annotation.*;
//
//import java.math.BigDecimal;
//import java.util.Comparator;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/collection-points")
//public class CollectionPointController {
//
//    private final CollectionPointService collectionPointService;
//    private final JobPositionRepository jobPositionRepository;
//    private final JobRotationRepository jobRotationRepository;
//    private final StaffRepository staffRepository;
//
//    @Autowired
//    public CollectionPointController(CollectionPointService collectionPointService,
//                                     JobPositionRepository jobPositionRepository,
//                                     JobRotationRepository jobRotationRepository, StaffRepository staffRepository) {
//        this.collectionPointService = collectionPointService;
//        this.jobPositionRepository = jobPositionRepository;
//        this.jobRotationRepository = jobRotationRepository;
//        this.staffRepository = staffRepository;
//    }
//
//    @PreAuthorize("hasRole('COLLECTOR')")
//    @PutMapping("/update-status/{id}")
//    public ResponseEntity<?> updateStatus(@PathVariable Integer id, @RequestBody UpdateStatusRequest request) {
//        try {
//            String username = SecurityContextHolder.getContext().getAuthentication().getName();
//            JobPosition updatedPosition = collectionPointService.updateStatus(id, request, username);
//            return ResponseEntity.ok("{\"message\": \"Status updated successfully\"}");
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"" + e.getMessage() + "\"}");
//        }
//    }
//
//    @PreAuthorize("hasRole('DRIVER')")
//    @GetMapping("/nearby-completed")
//    public ResponseEntity<?> getNearbyCompleted(@RequestParam BigDecimal lat, @RequestParam BigDecimal lng) {
//        try {
//            List<JobPosition> completedPoints = jobPositionRepository.findByStatusCompleted();
//            if (completedPoints.isEmpty()) {
//                return ResponseEntity.ok("{\"message\": \"No completed collection points found\"}");
//            }
//
//            completedPoints.sort(Comparator.comparingDouble(p ->
//                    calculateDistance(lat, p.getLat(), lng, p.getLng())));
//            return ResponseEntity.ok(completedPoints);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("{\"error\": \"Error retrieving completed points: " + e.getMessage() + "\"}");
//        }
//    }
//
//    @PreAuthorize("hasRole('DRIVER')")
//    @GetMapping("/assigned")
//    public ResponseEntity<?> getAssignedCollectionPointsForDriver() {
//        try {
//            String username = SecurityContextHolder.getContext().getAuthentication().getName();
//            Staff staff = staffRepository.findByUserName(username);
//            if (staff == null) {
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\": \"Invalid user\"}");
//            }
//
//            List<JobRotation> rotations = jobRotationRepository.findByStaffIdAndStatus(staff.getId(), "ROTATION_ACTIVE");
//            List<JobPosition> assignedPositions = rotations.stream()
//                    .map(JobRotation::getJobPosition)
//                    .toList();
//
//            if (assignedPositions.isEmpty()) {
//                return ResponseEntity.ok("{\"message\": \"No assigned collection points found\"}");
//            }
//
//            return ResponseEntity.ok(assignedPositions);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("{\"error\": \"Error retrieving assigned points: " + e.getMessage() + "\"}");
//        }
//    }
//
//    private double calculateDistance(BigDecimal lat1, BigDecimal lat2, BigDecimal lon1, BigDecimal lon2) {
//        double lat1d = lat1.doubleValue();
//        double lat2d = lat2.doubleValue();
//        double lon1d = lon1.doubleValue();
//        double lon2d = lon2.doubleValue();
//
//        final int R = 6371; // Bán kính Trái Đất (km)
//        double latDistance = Math.toRadians(lat2d - lat1d);
//        double lonDistance = Math.toRadians(lon2d - lon1d);
//        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
//                + Math.cos(Math.toRadians(lat1d)) * Math.cos(Math.toRadians(lat2d))
//                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
//        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
//        return R * c * 1000; // Khoảng cách tính bằng mét
//    }
//}