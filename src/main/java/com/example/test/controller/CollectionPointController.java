package com.example.test.controller;

import com.example.test.dto.*;
import com.example.test.entity.CollectionPointStatus;
import com.example.test.entity.JobPosition;
import com.example.test.entity.Staff;
import com.example.test.repository.CollectionPointStatusRepository;
import com.example.test.repository.JobPositionRepository;
import com.example.test.repository.JobRotationRepository;
import com.example.test.repository.StaffRepository;
import com.example.test.service.CollectionPointService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/collection-points")
public class CollectionPointController {

    private final CollectionPointService collectionPointService;
    private final StaffRepository staffRepository;
    private final CollectionPointStatusRepository collectionPointStatusRepository;

    @Autowired
    public CollectionPointController(CollectionPointService collectionPointService,
                                     JobPositionRepository jobPositionRepository,
                                     JobRotationRepository jobRotationRepository,
                                     StaffRepository staffRepository,
                                     CollectionPointStatusRepository collectionPointStatusRepository) {
        this.collectionPointService = collectionPointService;
        this.staffRepository = staffRepository;
        this.collectionPointStatusRepository = collectionPointStatusRepository;
    }

    @PreAuthorize("hasRole('COLLECTOR')")
    @PostMapping("/{id}/complete-collection")
    public ResponseEntity<?> completeCollection(@PathVariable Integer id, @RequestBody UpdateStatusRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            request.setCollectorStatus("COMPLETED");
            collectionPointService.updateStatus(id, request, username);
            return ResponseEntity.ok(new UpdateStatusResponse(
                    "Collector completed collection point",
                    id,
                    "COMPLETED",
                    LocalDateTime.now()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{id}/complete-driver-collection")
    public ResponseEntity<?> completeDriverCollection(@PathVariable Integer id, @RequestBody UpdateStatusRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            request.setStatus("COMPLETED");
            collectionPointService.updateStatus(id, request, username);
            return ResponseEntity.ok(new UpdateStatusResponse(
                    "Collection completed successfully",
                    id,
                    "COMPLETED",
                    LocalDateTime.now()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/driver-assigned")
    public ResponseEntity<List<JobPositionResponse>> getDriverAssignedPoints() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Staff driver = staffRepository.findByUserName(username);
        if (driver == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        List<JobPosition> points = collectionPointService.getDriverAssignedPoints(driver.getId());
        List<JobPositionResponse> response = points.stream().map(point -> {
            CollectionPointStatus pointStatus = collectionPointStatusRepository.findByJobPositionId(point.getId())
                    .orElse(new CollectionPointStatus());
            return new JobPositionResponse(
                    point.getId(),
                    point.getName(),
                    point.getAddress(),
                    point.getLat(),
                    point.getLng(),
                    point.getStatus(),
                    pointStatus.getCollectorStatus() != null ? pointStatus.getCollectorStatus() : "PENDING",
                    point.getCreatedAt()
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/nearest-pending")
    public ResponseEntity<?> getNearestPendingPoint(
            @RequestParam("lat") BigDecimal currentLat,
            @RequestParam("lng") BigDecimal currentLng,
            @RequestParam(value = "radius", defaultValue = "10000") Integer radius) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Staff driver = staffRepository.findByUserName(username);
            if (driver == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid user"));
            }

            JobPosition nearestPoint = collectionPointService.findNearestPendingPoint(currentLat, currentLng, driver.getId(), radius);
            if (nearestPoint == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("No pending collection point found within the specified radius or with completed collector status."));
            }

            CollectionPointStatus pointStatus = collectionPointStatusRepository.findByJobPositionId(nearestPoint.getId())
                    .orElse(new CollectionPointStatus());
            double distance = calculateDistance(
                    currentLat, currentLng, nearestPoint.getLat(), nearestPoint.getLng());
            int estimatedTravelTime = (int) Math.round(distance / 50000 * 60);

            NearestPendingPointResponse response = new NearestPendingPointResponse(
                    nearestPoint.getId(),
                    nearestPoint.getName(),
                    nearestPoint.getAddress(),
                    nearestPoint.getLat(),
                    nearestPoint.getLng(),
                    distance,
                    estimatedTravelTime,
                    "ASSIGNED",
                    pointStatus.getCollectorStatus() != null ? pointStatus.getCollectorStatus() : "PENDING"
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/optimized-route")
    public ResponseEntity<?> getOptimizedRoute(
            @RequestParam("lat") BigDecimal currentLat,
            @RequestParam("lng") BigDecimal currentLng,
            @RequestParam(value = "radius", defaultValue = "10000") Integer radius) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Staff driver = staffRepository.findByUserName(username);
            if (driver == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid user"));
            }

            OptimizedRouteResponse route = collectionPointService.getOptimizedRoute(currentLat, currentLng, driver.getId(), radius);
            if (route.getPoints().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("No valid collection points found for route optimization."));
            }

            return ResponseEntity.ok(route);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
        }
    }

    private double calculateDistance(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double lngDistance = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000;
    }

    @Getter
    @Setter
    static
    class UpdateStatusResponse {
        private String message;
        private Integer pointId;
        private String status;
        private LocalDateTime completionTime;

        public UpdateStatusResponse(String message, Integer pointId, String status, LocalDateTime completionTime) {
            this.message = message;
            this.pointId = pointId;
            this.status = status;
            this.completionTime = completionTime;
        }

    }

    @Getter
    @Setter
    static
    class JobPositionResponse {
        private Integer id;
        private String name;
        private String address;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String status;
        private String collectorStatus;
        private LocalDateTime assignedDate;

        public JobPositionResponse(Integer id, String name, String address, BigDecimal latitude,
                                   BigDecimal longitude, String status, String collectorStatus,
                                   LocalDateTime assignedDate) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
            this.status = status;
            this.collectorStatus = collectorStatus;
            this.assignedDate = assignedDate;
        }

    }

    @Getter
    @Setter
    static
    class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }


    }
}