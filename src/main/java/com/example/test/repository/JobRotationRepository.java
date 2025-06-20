package com.example.test.repository;

import com.example.test.dto.DriverJobWithCollectorStatusDTO;
import com.example.test.dto.JobRotationDetailDTO;
import com.example.test.entity.JobRotation;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JobRotationRepository extends JpaRepository<JobRotation, Integer> {

    // Tìm lịch phân công theo ID nhân viên
    List<JobRotation> findByStaffId(Integer staffId);

    // Thống kê lịch phân công theo tên vị trí công việc
    @Query(value = "SELECT jp.name, COUNT(jr.id) " +
            "FROM t_job_rotation jr " +
            "JOIN t_job_position jp ON jr.position_id = jp.id " +
            "GROUP BY jp.name", nativeQuery = true)
    List<Object[]> getRotationStatistics();

    // Cập nhật trạng thái dựa trên thời gian ca làm việc
    @Modifying
    @Transactional
    @Query(value = """
    UPDATE t_job_rotation jr
    JOIN t_shift s ON jr.shift_id = s.id
    SET jr.status = 'LATE'
    WHERE jr.status = 'ASSIGNED' 
      AND (
        -- Trường hợp 1: Ngày rotation đã qua
        jr.rotation_date < CURDATE()
        OR 
        -- Trường hợp 2: Kết hợp ngày + giờ để so sánh chính xác
        TIMESTAMP(jr.rotation_date, ADDTIME(s.end_time, '01:00:00')) < NOW()
      )
    """, nativeQuery = true)
    int updateLateJobRotations();

    @Query(value = """
    SELECT 
        jr.id,
        jr.status,
        jr.rotation_date,
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
        s.email,
        
        sh.id AS shift_id,
        sh.name AS shift_name,
        sh.start_time,
        sh.end_time
    FROM t_job_rotation jr
    JOIN t_user s ON jr.staff_id = s.id
    JOIN t_job_position jp ON jr.position_id = jp.id
    JOIN t_shift sh ON jr.shift_id = sh.id
    LEFT JOIN t_vehicle v ON jr.vehicle_id = v.id
    WHERE s.user_name = :userName
    AND DATE(jr.rotation_date) = DATE(:rotationDate)
        """, nativeQuery = true)
    List<JobRotationDetailDTO> findByUserNameAndDate(@Param("userName") String userName,
                                                     @Param("rotationDate") LocalDate rotationDate);
    /**
     * Lấy tất cả công việc của tài xế (không phân biệt status)
     */
    @Query(value = """
    SELECT jr.* FROM t_job_rotation jr
    WHERE jr.staff_id = :staffId 
    AND jr.role = 'DRIVER'
    ORDER BY jr.rotation_date ASC, jr.created_at ASC
    """, nativeQuery = true)
    List<JobRotation> findAllDriverJobs(@Param("staffId") Integer staffId);

    /**
     * Lấy tất cả công việc của collector (không phân biệt status)
     */
    @Query(value = """
    SELECT jr.* FROM t_job_rotation jr
    WHERE jr.staff_id = :staffId 
    AND jr.role = 'COLLECTOR'
    ORDER BY jr.rotation_date ASC, jr.created_at ASC
    """, nativeQuery = true)
    List<JobRotation> findAllCollectorJobs(@Param("staffId") Integer staffId);


    // Trong method lấy danh sách job của driver, có thể join để biết trạng thái:
    @Query("SELECT j FROM JobRotation j " +
            "LEFT JOIN RotationStoreId rs ON rs.rotationIdDriver = j.id " +
            "WHERE j.staffId = :driverId AND j.role = 'DRIVER' AND rs.isCompleted = true")
    List<JobRotation> findDriverJobsWithCompletionStatus(@Param("driverId") Integer driverId);

    /**
     * Query lấy công việc của driver kèm thông tin collector status
     */
    @Query(value = """
        SELECT 
            jr.id,
            jr.status,
            jr.rotation_date,
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
            s.email,
            
            sh.id AS shift_id,
            sh.name AS shift_name,
            sh.start_time,
            sh.end_time,
            
            -- Chỉ trạng thái collector đã hoàn thành hay chưa
            rs.is_completed AS collector_completed
            
        FROM t_job_rotation jr
        JOIN t_user s ON jr.staff_id = s.id
        JOIN t_job_position jp ON jr.position_id = jp.id
        JOIN t_shift sh ON jr.shift_id = sh.id
        LEFT JOIN t_vehicle v ON jr.vehicle_id = v.id
        
        -- JOIN với bảng rotation_storeId để lấy trạng thái collector
        LEFT JOIN t_rotation_storeId rs ON rs.rotation_id_driver = jr.id
        
        WHERE s.user_name = :userName
        AND jr.role = 'DRIVER'
        AND DATE(jr.rotation_date) = DATE(:rotationDate)
        
        ORDER BY jr.created_at DESC
        """, nativeQuery = true)
    List<Object[]> findDriverJobsWithCollectorStatusByDate(
            @Param("userName") String userName,
            @Param("rotationDate") LocalDate rotationDate);
//    List<Object[]> findDriverJobsWithCollectorStatusByDate(
//            @Param("userName") String userName,
//            @Param("rotationDate") LocalDate rotationDate);

}