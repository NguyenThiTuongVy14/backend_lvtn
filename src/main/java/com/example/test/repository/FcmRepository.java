package com.example.test.repository;

import com.example.test.entity.FCMToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FcmRepository extends JpaRepository<FCMToken, Integer> {
    List<FCMToken> findByStaffId(Integer staffId);

    @Query("SELECT COUNT(f) > 0 FROM FCMToken f WHERE f.token = :token")
    boolean existsByToken(@Param("token") String token);


    void deleteByToken(String fcmToken);
}
