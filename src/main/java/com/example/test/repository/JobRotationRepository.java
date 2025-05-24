package com.example.test.repository;

import com.example.test.entity.JobRotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRotationRepository extends JpaRepository<JobRotation, Integer> {
    List<JobRotation> findByStaffIdAndStatus(Integer staffId, String status);
}