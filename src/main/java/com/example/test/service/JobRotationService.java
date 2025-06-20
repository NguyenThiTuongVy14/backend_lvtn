package com.example.test.service;

import com.example.test.dto.*;
import com.example.test.entity.JobPosition;
import com.example.test.entity.JobRotation;
import com.example.test.entity.Staff;
import com.example.test.entity.Vehicle;
import com.example.test.repository.JobPositionRepository;
import com.example.test.repository.JobRotationRepository;
import com.example.test.repository.StaffRepository;
import com.example.test.repository.VehicleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

@Service
public class JobRotationService {

    private final JobRotationRepository jobRotationRepository;
    private final StaffRepository staffRepository;
    private final JobPositionRepository jobPositionRepository;
    private final VehicleRepository vehicleRepository;

    @Autowired
    public JobRotationService(JobRotationRepository jobRotationRepository,
                              StaffRepository staffRepository,
                              JobPositionRepository jobPositionRepository,
                              VehicleRepository vehicleRepository) {
        this.jobRotationRepository = jobRotationRepository;
        this.staffRepository = staffRepository;
        this.jobPositionRepository = jobPositionRepository;
        this.vehicleRepository = vehicleRepository;
    }
    public List<JobRotationDetailDTO> getMyJobRotationsByDate(String userName, LocalDate date) {
        System.out.println("Getting rotations for user: " + userName + " on date: " + date);
        return jobRotationRepository.findByUserNameAndDate(userName, date);
    }
    /**
     * Lấy danh sách công việc của driver với thông tin collector status theo ngày
     */
    public List<DriverJobWithCollectorStatusDTO> getDriverJobsWithCollectorStatusByDate(
            String username, LocalDate date) {

        return jobRotationRepository.findDriverJobsWithCollectorStatusByDate(username, date);
    }
    @Scheduled(cron = "0 0 * * * *") // mỗi giờ đầu tiên: 0:00, 1:00, 2:00,...
    public void autoFailExpiredJobs() {
        int affected = jobRotationRepository.updateLateJobRotations();
        System.out.println("Updated " + affected + " job rotations to LATE status.");
    }

}