package com.example.test.repository;

import com.example.test.dto.JobRotationDetailDTO;
import com.example.test.entity.JobRotation;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRotationRepository extends JpaRepository<JobRotation, Integer> {
    // Tìm lịch phân công theo ID nhân viên
    List<JobRotation> findByStaffId(Integer staffId);


    // Thống kê lịch phân công theo tên vị trí công việc
    @Query(value = "SELECT jp.name, COUNT(jr.id) " +
            "FROM t_job_rotation jr " +
            "JOIN t_job_position jp ON jr.job_position_id = jp.id " +
            "GROUP BY jp.name", nativeQuery = true)
    List<Object[]> getRotationStatistics();

    @Modifying
    @Transactional
    @Query(value = "UPDATE t_job_rotation SET status = 'FAIL' " +
            "WHERE rotation_date < NOW() " +
            "AND status IN ('PENDING')", nativeQuery = true)
    int updateStatusToFail();


    @Query(value = """
    SELECT 
        jr.id,
        jr.status,
        jr.created_at,
        jr.updated_at,

        s.id AS staff_id,
        s.full_name AS staff_name,

        jp.id AS position_id,
        jp.name AS job_position_name,

        v.id AS vehicle_id,
        v.license_plate,

        jp.lat,
        jp.lng,
        jp.address,

        s.phone,
        s.email

    FROM t_job_rotation jr
    JOIN t_user s ON jr.staff_id = s.id
    JOIN t_job_position jp ON jr.position_id = jp.id
    LEFT JOIN t_vehicle v ON jr.vehicle_id = v.id
    WHERE s.user_name = :userName
""", nativeQuery = true)
    List<JobRotationDetailDTO> findByUserName(@Param("userName") String userName);


}