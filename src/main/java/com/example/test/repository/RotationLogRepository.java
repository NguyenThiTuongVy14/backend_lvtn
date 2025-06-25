package com.example.test.repository;

import com.example.test.entity.RotationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RotationLogRepository extends JpaRepository<RotationLog,Integer> {
    List<RotationLog> findByStatusAndRotationDate(String status, LocalDate rotationDate);
}
