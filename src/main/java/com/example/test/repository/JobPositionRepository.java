package com.example.test.repository;

import com.example.test.entity.JobPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobPositionRepository extends JpaRepository<JobPosition, Integer> {
    @Query("SELECT j FROM JobPosition j WHERE j.status = 'COMPLETED'")
    List<JobPosition> findByStatusCompleted();
}