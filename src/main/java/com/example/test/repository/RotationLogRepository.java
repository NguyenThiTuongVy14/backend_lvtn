package com.example.test.repository;

import com.example.test.entity.RotationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface RotationLogRepository extends JpaRepository<RotationLog,Integer> {

    List<RotationLog> findByStatusAndRotationDateAndShiftId(String status, LocalDate rotationDate, Integer shiftId);

    @Query("update RotationLog r set r.status = :status where r.staffId = :staffId and r.updatedAt = :updatedAt")
    @Modifying
    void updateStatusByStaffIdAndUpdatedAt(String status, Integer staffId, LocalDateTime updatedAt);

    List<RotationLog> findByStatusAndRotationDate(String request, LocalDate date);
}
