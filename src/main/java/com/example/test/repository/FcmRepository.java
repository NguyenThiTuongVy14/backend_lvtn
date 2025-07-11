package com.example.test.repository;

import com.example.test.entity.FCMToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FcmRepository extends JpaRepository<FCMToken, Integer> {
    List<FCMToken> findByStaffId(Integer staffId);

}
