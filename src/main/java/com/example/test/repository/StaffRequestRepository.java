package com.example.test.repository;

import com.example.test.entity.StaffRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffRequestRepository extends JpaRepository<StaffRequest, Long> {
    boolean findByEmail(String email);

    boolean existsByEmail(String email);
}
