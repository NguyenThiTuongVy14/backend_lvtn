package com.example.test.repository;

import com.example.test.dto.JobRotationDetailDTO;
import com.example.test.entity.JobRotation;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
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
    @Query(value = """
    UPDATE t_job_rotation jr
    JOIN t_shift s ON jr.shift_id = s.id
    SET jr.status = CASE
        WHEN TIMESTAMPDIFF(HOUR, CAST(s.end_time AS CHAR), jr.update_time) > 4
             AND jr.update_time IS NOT NULL
             AND jr.status IN ('PENDING', 'ASSIGNED')
        THEN 'FAIL'
        
        WHEN jr.update_time > CAST(s.end_time AS CHAR)
             AND TIMESTAMPDIFF(HOUR, CAST(s.end_time AS CHAR), jr.update_time) <= 4
             AND jr.status IN ('PENDING', 'ASSIGNED')
        THEN 'LATE'
        
        ELSE jr.status
    END
""", nativeQuery = true)
    int updateStatusByEndTime();



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


    List<JobRotation> findByStaffIdAndRoleAndRotationDate(Integer staffId, String role, Date rotationDate);

    List<JobRotation> findByStaffIdAndRoleAndStatus(Integer staffId, String collector, String pending);
}