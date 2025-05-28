package com.example.test.service;

import com.example.test.entity.JobPosition;
import com.example.test.repository.JobPositionRepository;
import com.example.test.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class JobPositionService {

    private final JobPositionRepository jobPositionRepository;
    private final StaffRepository staffRepository;

    @Autowired
    public JobPositionService(JobPositionRepository jobPositionRepository, StaffRepository staffRepository) {
        this.jobPositionRepository = jobPositionRepository;
        this.staffRepository = staffRepository;
    }

    public JobPosition createJobPosition(JobPosition jobPosition) {
        validateJobPosition(jobPosition);
        if (!staffRepository.existsById(jobPosition.getCreatedBy())) {
            throw new IllegalArgumentException("Invalid createdBy: Staff does not exist");
        }
        jobPosition.setCreatedAt(LocalDateTime.now());
        jobPosition.setUpdatedAt(LocalDateTime.now());
        return jobPositionRepository.save(jobPosition);
    }

    public JobPosition updateJobPosition(Integer id, JobPosition jobPosition) {
        Optional<JobPosition> existingPosition = jobPositionRepository.findById(id);
        if (existingPosition.isEmpty()) {
            throw new IllegalArgumentException("Job position not found");
        }
        validateJobPosition(jobPosition);
        if (!staffRepository.existsById(jobPosition.getCreatedBy())) {
            throw new IllegalArgumentException("Invalid createdBy: Staff does not exist");
        }
        JobPosition updatedPosition = existingPosition.get();
        updatedPosition.setName(jobPosition.getName());
        updatedPosition.setAddress(jobPosition.getAddress());
        updatedPosition.setLat(jobPosition.getLat());
        updatedPosition.setLng(jobPosition.getLng());
        updatedPosition.setImage(jobPosition.getImage());
        updatedPosition.setStatus(jobPosition.getStatus());
        updatedPosition.setUpdatedAt(LocalDateTime.now());
        return jobPositionRepository.save(updatedPosition);
    }

    public void deleteJobPosition(Integer id) {
        if (!jobPositionRepository.existsById(id)) {
            throw new IllegalArgumentException("Job position not found");
        }
        jobPositionRepository.deleteById(id);
    }

    public List<JobPosition> findNearbyPositions(BigDecimal lat, BigDecimal lng, Double radius) {

        return jobPositionRepository.findNearbyPositions(lat, lng, radius / 1000);
    }

    private void validateJobPosition(JobPosition jobPosition) {
        if (jobPosition.getName() == null || jobPosition.getName().isEmpty()) {
            throw new IllegalArgumentException("Job position name is required");
        }
        if (jobPosition.getAddress() == null || jobPosition.getAddress().isEmpty()) {
            throw new IllegalArgumentException("Address is required");
        }
        if (jobPosition.getLat() == null || jobPosition.getLng() == null) {
            throw new IllegalArgumentException("Latitude and longitude are required");
        }
        if (jobPosition.getStatus() == null ||
                !jobPosition.getStatus().equals("ACTIVE") && !jobPosition.getStatus().equals("INACTIVE")) {
            throw new IllegalArgumentException("Status must be ACTIVE or INACTIVE");
        }
        if (jobPosition.getCreatedBy() == null) {
            throw new IllegalArgumentException("CreatedBy is required");
        }
    }
}