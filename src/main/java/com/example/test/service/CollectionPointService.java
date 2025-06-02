package com.example.test.service;

import com.example.test.dto.OptimizedRouteResponse;
import com.example.test.dto.RoutePoint;
import com.example.test.dto.UpdateStatusRequest;
import com.example.test.dto.WebSocketMessage;
import com.example.test.entity.*;
import com.example.test.repository.AuthorityRepository;
import com.example.test.repository.CollectionPointStatusRepository;
import com.example.test.repository.JobPositionRepository;
import com.example.test.repository.JobRotationRepository;
import com.example.test.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CollectionPointService {

    private final JobPositionRepository jobPositionRepository;
    private final StaffRepository staffRepository;
    private final AuthorityRepository authorityRepository;
    private final JobRotationRepository jobRotationRepository;
    private final CollectionPointStatusRepository collectionPointStatusRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public CollectionPointService(JobPositionRepository jobPositionRepository,
                                  StaffRepository staffRepository,
                                  AuthorityRepository authorityRepository,
                                  JobRotationRepository jobRotationRepository,
                                  CollectionPointStatusRepository collectionPointStatusRepository,
                                  SimpMessagingTemplate messagingTemplate) {
        this.jobPositionRepository = jobPositionRepository;
        this.staffRepository = staffRepository;
        this.authorityRepository = authorityRepository;
        this.jobRotationRepository = jobRotationRepository;
        this.collectionPointStatusRepository = collectionPointStatusRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void updateStatus(Integer jobPositionId, UpdateStatusRequest request, String username) {
        JobPosition position = jobPositionRepository.findById(jobPositionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection point not found"));

        Staff staff = staffRepository.findByUserName(username);
        if (staff == null || staff.getAuthorityId() == null) {
            throw new IllegalArgumentException("Invalid user or authority");
        }

        Authority authority = authorityRepository.findById(staff.getAuthorityId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid authority ID: " + staff.getAuthorityId()));

        if (authority.getName().equals("COLLECTOR")) {
            if (!isValidCollectorStatus(request.getCollectorStatus())) {
                throw new IllegalArgumentException("Invalid collector status. Must be PENDING or COMPLETED");
            }

            CollectionPointStatus pointStatus = collectionPointStatusRepository
                    .findByJobPositionIdAndStaffId(jobPositionId, staff.getId())
                    .orElse(new CollectionPointStatus());
            pointStatus.setJobPositionId(jobPositionId);
            pointStatus.setStaffId(staff.getId());
            pointStatus.setCollectorStatus(request.getCollectorStatus());
            pointStatus.setCompletionNotes(request.getCompletionNotes());
            pointStatus.setCreatedAt(LocalDateTime.now());
            pointStatus.setUpdatedAt(LocalDateTime.now());
            collectionPointStatusRepository.save(pointStatus);

            // Thông báo cho tài xế được phân công điểm này
            List<JobRotation> rotations = jobRotationRepository.findByJobPositionIdAndStatus(jobPositionId, "ASSIGNED");
            for (JobRotation rotation : rotations) {
                messagingTemplate.convertAndSend(
                        "/topic/driver/" + rotation.getStaffId(),
                        new WebSocketMessage("COLLECTOR_STATUS_CHANGED", jobPositionId, null, request.getCollectorStatus(),
                                "Point #" + jobPositionId + " updated to " + request.getCollectorStatus(), LocalDateTime.now())
                );
            }
        } else if (authority.getName().equals("DRIVER")) {
            if (!isValidStatus(request.getStatus())) {
                throw new IllegalArgumentException("Invalid status. Must be PENDING, ASSIGNED, COMPLETED, CANCEL, FAIL");
            }

            JobRotation rotation = jobRotationRepository.findByStaffIdAndJobPositionId(staff.getId(), jobPositionId)
                    .orElseThrow(() -> new IllegalArgumentException("Job rotation not found"));
            rotation.setStatus(request.getStatus());
            rotation.setUpdatedAt(LocalDateTime.now());
            jobRotationRepository.save(rotation);
        } else {
            throw new IllegalArgumentException("Only COLLECTOR or DRIVER can update collection point status");
        }

        if (request.getCurrentLatitude() != null && request.getCurrentLongitude() != null) {
            double distance = calculateDistance(
                    request.getCurrentLatitude(), request.getCurrentLongitude(),
                    position.getLat(), position.getLng());
            if (distance > 100) {
                throw new IllegalArgumentException("User is too far from collection point");
            }
        }

        WebSocketMessage wsMessage = new WebSocketMessage();
        wsMessage.setId(jobPositionId);
        wsMessage.setTimestamp(LocalDateTime.now());
        if (authority.getName().equals("COLLECTOR")) {
            wsMessage.setType("COLLECTOR_STATUS_CHANGED");
            wsMessage.setCollectorStatus(request.getCollectorStatus());
            wsMessage.setMessage("Collection point #" + jobPositionId + " collector status updated to " + request.getCollectorStatus() + " by " + username);
        } else {
            wsMessage.setType("POINT_STATUS_CHANGED");
            wsMessage.setStatus(request.getStatus());
            wsMessage.setMessage("Collection point #" + jobPositionId + " status updated to " + request.getStatus() + " by " + username);
        }

        messagingTemplate.convertAndSend("/topic/collection-point-updates", wsMessage);
        messagingTemplate.convertAndSend("/topic/admin", wsMessage);
        messagingTemplate.convertAndSend("/topic/" + authority.getName().toLowerCase() + "/" + staff.getId(), wsMessage);
    }

    public List<JobPosition> getDriverAssignedPoints(Integer driverId) {
        List<JobRotation> rotations = jobRotationRepository.findByStaffIdAndStatus(driverId, "ASSIGNED");
        return rotations.stream()
                .map(JobRotation::getJobPositionId)
                .map(jobPositionRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(position -> position.getStatus().equals("ACTIVE"))
                .collect(Collectors.toList());
    }

    public JobPosition findNearestPendingPoint(BigDecimal currentLat, BigDecimal currentLng, Integer driverId, Integer radius) {
        List<JobPosition> assignedPoints = getDriverAssignedPoints(driverId);
        JobPosition nearestPoint = null;
        double minDistance = Double.MAX_VALUE;

        for (JobPosition point : assignedPoints) {
            CollectionPointStatus pointStatus = collectionPointStatusRepository.findByJobPositionId(point.getId())
                    .orElse(null);
            if (pointStatus != null && pointStatus.getCollectorStatus().equals("COMPLETED")) {
                JobRotation rotation = jobRotationRepository.findByStaffIdAndJobPositionId(driverId, point.getId())
                        .orElse(null);
                if (rotation != null && rotation.getStatus().equals("ASSIGNED")) {
                    double distance = calculateDistance(currentLat, currentLng, point.getLat(), point.getLng());
                    if (distance <= radius && distance < minDistance) {
                        minDistance = distance;
                        nearestPoint = point;
                    }
                }
            }
        }

        return nearestPoint;
    }

    public OptimizedRouteResponse getOptimizedRoute(BigDecimal currentLat, BigDecimal currentLng, Integer driverId, Integer radius) {
        List<JobPosition> assignedPoints = getDriverAssignedPoints(driverId);

        // Phân loại điểm: COMPLETED và PENDING
        List<JobPosition> completedPoints = new ArrayList<>();
        List<JobPosition> pendingPoints = new ArrayList<>();

        for (JobPosition point : assignedPoints) {
            CollectionPointStatus pointStatus = collectionPointStatusRepository.findByJobPositionId(point.getId())
                    .orElse(null);
            JobRotation rotation = jobRotationRepository.findByStaffIdAndJobPositionId(driverId, point.getId())
                    .orElse(null);
            if (rotation == null || !rotation.getStatus().equals("ASSIGNED")) {
                continue;
            }
            double distance = calculateDistance(currentLat, currentLng, point.getLat(), point.getLng());
            if (distance > radius) {
                continue;
            }
            if (pointStatus != null && pointStatus.getCollectorStatus().equals("COMPLETED")) {
                completedPoints.add(point);
            } else {
                pendingPoints.add(point);
            }
        }

        // Kết hợp: COMPLETED trước, PENDING sau
        List<JobPosition> validPoints = new ArrayList<>();
        validPoints.addAll(completedPoints);
        validPoints.addAll(pendingPoints);

        if (validPoints.isEmpty()) {
            return new OptimizedRouteResponse(Collections.emptyList(), 0.0, 0);
        }

        List<JobPosition> route = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        double totalDistance = 0.0;

        JobPosition startPoint = new JobPosition();
        startPoint.setId(0);
        startPoint.setLat(currentLat);
        startPoint.setLng(currentLng);
        validPoints.add(0, startPoint);

        int currentId = 0;
        while (visited.size() < validPoints.size() - 1) {
            final int finalCurrentId = currentId;
            visited.add(finalCurrentId);
            Map<Integer, Double> distances = new HashMap<>();
            Map<Integer, Integer> previous = new HashMap<>();
            PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(node -> node.distance));

            for (JobPosition point : validPoints) {
                distances.put(point.getId(), Double.MAX_VALUE);
            }
            distances.put(finalCurrentId, 0.0);
            pq.offer(new Node(finalCurrentId, 0.0));

            while (!pq.isEmpty()) {
                Node node = pq.poll();
                int nodeId = node.id;
                double nodeDistance = node.distance;

                if (nodeDistance > distances.get(nodeId)) continue;

                for (JobPosition neighbor : validPoints) {
                    if (neighbor.getId() == nodeId || visited.contains(neighbor.getId())) continue;

                    JobPosition currentPoint = validPoints.stream()
                            .filter(p -> p.getId() == nodeId)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Point not found: " + nodeId));

                    double edgeWeight = calculateDistance(
                            currentPoint.getLat(),
                            currentPoint.getLng(),
                            neighbor.getLat(),
                            neighbor.getLng()
                    );

                    double newDistance = distances.get(nodeId) + edgeWeight;
                    if (newDistance < distances.get(neighbor.getId())) {
                        distances.put(neighbor.getId(), newDistance);
                        previous.put(neighbor.getId(), nodeId);
                        pq.offer(new Node(neighbor.getId(), newDistance));
                    }
                }
            }

            double minDistance = Double.MAX_VALUE;
            int nextId = -1;
            for (Map.Entry<Integer, Double> entry : distances.entrySet()) {
                if (!visited.contains(entry.getKey()) && entry.getKey() != 0 && entry.getValue() < minDistance) {
                    minDistance = entry.getValue();
                    nextId = entry.getKey();
                }
            }

            if (nextId == -1) break;

            int finalNextId = nextId;
            JobPosition nextPoint = validPoints.stream()
                    .filter(p -> p.getId() == finalNextId)
                    .findFirst()
                    .orElse(null);
            if (nextPoint != null) {
                route.add(nextPoint);
                totalDistance += minDistance;
                currentId = nextId;
            }
        }

        List<RoutePoint> routePoints = new ArrayList<>();
        for (int i = 0; i < route.size(); i++) {
            JobPosition point = route.get(i);
            CollectionPointStatus pointStatus = collectionPointStatusRepository.findByJobPositionId(point.getId())
                    .orElse(null);

            Double distanceToNext = null;
            Integer estimatedTimeToNext = null;
            if (i < route.size() - 1) {
                JobPosition nextPoint = route.get(i + 1);
                distanceToNext = calculateDistance(point.getLat(), point.getLng(), nextPoint.getLat(), nextPoint.getLng());
                estimatedTimeToNext = (int) Math.round(distanceToNext / 50000 * 60);
            }

            routePoints.add(new RoutePoint(
                    point.getId(),
                    point.getName(),
                    point.getAddress(),
                    point.getLat(),
                    point.getLng(),
                    "ASSIGNED",
                    pointStatus != null ? pointStatus.getCollectorStatus() : "PENDING",
                    distanceToNext,
                    estimatedTimeToNext
            ));
        }

        int totalEstimatedTime = routePoints.stream()
                .filter(p -> p.getEstimatedTimeToNext() != null)
                .mapToInt(RoutePoint::getEstimatedTimeToNext)
                .sum();

        return new OptimizedRouteResponse(routePoints, totalDistance, totalEstimatedTime);
    }

    private boolean isValidStatus(String status) {
        if (status == null) return false;
        return List.of("PENDING", "ASSIGNED", "COMPLETED", "CANCEL", "FAIL").contains(status);
    }

    private boolean isValidCollectorStatus(String collectorStatus) {
        if (collectorStatus == null) return false;
        try {
            CollectorStatus.valueOf(collectorStatus);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
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

    private static class Node {
        int id;
        double distance;

        Node(int id, double distance) {
            this.id = id;
            this.distance = distance;
        }
    }
}