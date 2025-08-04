package com.example.test.repository;

import com.example.test.entity.RotationLog;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface RotationLogRepository extends JpaRepository<RotationLog,Integer> {

    List<RotationLog> findByStatusAndRotationDateAndShiftId(String status, LocalDate rotationDate, Integer shiftId);

    @Query("update RotationLog r set r.status = :status where r.staffId = :staffId and r.updatedAt = :updatedAt")
    @Modifying
    void updateStatusByStaffIdAndUpdatedAt(String status, Integer staffId, LocalDateTime updatedAt);


    boolean existsByStaffIdAndShiftIdAndRotationDate(Integer staffId, Integer shiftId, LocalDate rotationDate);



    List<RotationLog> findByStatusAndRotationDate(String request, LocalDate date);


    long countByRotationDateAndStatus(LocalDate rotationDate, String status);

    List<RotationLog> findByRotationDateAndStatus(LocalDate rotationDate, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select count(r) from RotationLog r where r.rotationDate = :date and r.status = 'ASSIGNED'")
    long countAssignedForUpdate(@Param("date") LocalDate date);

    /**
     * Lấy WAITLIST theo: carry_points DESC, requestedAt ASC
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
       select r from RotationLog r
       join r.staff s
       where r.rotationDate = :date
         and r.status = 'WAITLIST'
       order by s.carryPoints desc, r.requestedAt asc
    """)
    List<RotationLog> findWaitlistOrderByCarryPointsForUpdate(@Param("date") LocalDate date);

    /**
     * Lấy ASSIGNED theo: carry_points DESC, assignedAt ASC (để khi dư tài xế, cắt đuôi công bằng hơn)
     */
    @Query("""
       select r from RotationLog r
       join r.staff s
       where r.rotationDate = :date
         and r.status = 'ASSIGNED'
       order by s.carryPoints desc, r.updatedAt asc
    """)
    List<RotationLog> findAssignedOrderByCarryPoints(@Param("date") LocalDate date);

    List<RotationLog> findByRotationDate(LocalDate rotationDate);

    RotationLog findByStaffIdAndRotationDate(Integer staffId, LocalDate date);
}
