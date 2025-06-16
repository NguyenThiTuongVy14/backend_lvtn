package com.example.test.repository;

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
    AND jr.rotation_date BETWEEN :startDate AND :endDate
    """, nativeQuery = true)
        List<JobRotationDetailDTO> findByUserNameAndDateRange(@Param("userName") String userName,
                                                              @Param("startDate") LocalDate startDate,
                                                              @Param("endDate") LocalDate endDate);
    // Existing collector methods
    List<JobRotation> findByStaffIdAndRoleAndStatus(Integer staffId, String role, String status);

    // ===========================================
    // COLLECTOR-SPECIFIC METHODS
    // ===========================================

    /**
     * Lấy công việc collector theo ngày
     */
    @Query(value = """
    SELECT jr.* FROM t_job_rotation jr
    WHERE jr.staff_id = :staffId 
    AND jr.role = 'COLLECTOR'
    AND jr.rotation_date = :rotationDate
    ORDER BY jr.created_at ASC
    """, nativeQuery = true)
    List<JobRotation> findCollectorJobsByDate(@Param("staffId") Integer staffId,
                                              @Param("rotationDate") LocalDate rotationDate);

    // ===========================================
    // DRIVER-SPECIFIC METHODS
    // ===========================================

    /**
     * Lấy danh sách công việc của tài xế theo ngày
     */
    @Query(value = """
    SELECT jr.* FROM t_job_rotation jr
    WHERE jr.staff_id = :staffId 
    AND jr.role = 'DRIVER'
    AND jr.rotation_date = :rotationDate
    ORDER BY jr.created_at ASC
    """, nativeQuery = true)
    List<JobRotation> findDriverJobsByDate(@Param("staffId") Integer staffId,
                                           @Param("rotationDate") LocalDate rotationDate);

    /**
     * Lấy chi tiết công việc tài xế với thông tin đầy đủ
     */
    @Query(value = """
    SELECT 
        jr.id as job_rotation_id,
        jr.status,
        jr.role,
        jr.rotation_date,
        jr.created_at,
        jr.updated_at,
        
        s.id as staff_id,
        s.full_name as staff_name,
        
        jp.id as position_id,
        jp.name as job_position_name,
        jp.address,
        jp.lat,
        jp.lng,
        
        v.id as vehicle_id,
        v.license_plate,
        v.capacity,
        
        sh.id as shift_id,
        sh.name as shift_name,
        sh.start_time,
        sh.end_time
        
    FROM t_job_rotation jr
    JOIN t_user s ON jr.staff_id = s.id
    JOIN t_job_position jp ON jr.position_id = jp.id
    JOIN t_shift sh ON jr.shift_id = sh.id
    LEFT JOIN t_vehicle v ON jr.vehicle_id = v.id
    
    WHERE jr.staff_id = :staffId 
    AND jr.role = 'DRIVER'
    AND jr.status = :status
    ORDER BY jr.rotation_date ASC, sh.start_time ASC
    """, nativeQuery = true)
    List<Object[]> findDriverJobsWithDetails(@Param("staffId") Integer staffId,
                                             @Param("status") String status);

    /**
     * Lấy chi tiết công việc collector với thông tin đầy đủ
     */
    @Query(value = """
    SELECT 
        jr.id as job_rotation_id,
        jr.status,
        jr.role,
        jr.rotation_date,
        jr.created_at,
        jr.updated_at,
        
        s.id as staff_id,
        s.full_name as staff_name,
        
        jp.id as position_id,
        jp.name as job_position_name,
        jp.address,
        jp.lat,
        jp.lng,
        
        v.id as vehicle_id,
        v.license_plate,
        v.capacity,
        
        sh.id as shift_id,
        sh.name as shift_name,
        sh.start_time,
        sh.end_time
        
    FROM t_job_rotation jr
    JOIN t_user s ON jr.staff_id = s.id
    JOIN t_job_position jp ON jr.position_id = jp.id
    JOIN t_shift sh ON jr.shift_id = sh.id
    LEFT JOIN t_vehicle v ON jr.vehicle_id = v.id
    
    WHERE jr.staff_id = :staffId 
    AND jr.role = 'COLLECTOR'
    AND jr.status = :status
    ORDER BY jr.rotation_date ASC, sh.start_time ASC
    """, nativeQuery = true)
    List<Object[]> findCollectorJobsWithDetails(@Param("staffId") Integer staffId,
                                                @Param("status") String status);

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

    /**
     * Đếm số lượng công việc theo trạng thái và role
     */
    @Query(value = """
    SELECT jr.status, jr.role, COUNT(*) 
    FROM t_job_rotation jr
    WHERE jr.staff_id = :staffId 
    GROUP BY jr.status, jr.role
    """, nativeQuery = true)
    List<Object[]> countJobsByStatusAndRole(@Param("staffId") Integer staffId);

    /**
     * Lấy công việc trong khoảng thời gian
     */
    @Query(value = """
    SELECT jr.* FROM t_job_rotation jr
    WHERE jr.staff_id = :staffId 
    AND jr.role = :role
    AND jr.rotation_date BETWEEN :startDate AND :endDate
    ORDER BY jr.rotation_date ASC
    """, nativeQuery = true)
    List<JobRotation> findJobsByDateRange(@Param("staffId") Integer staffId,
                                          @Param("role") String role,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    /**
     * Lấy công việc hôm nay theo ca làm việc
     */
    @Query(value = """
    SELECT jr.* FROM t_job_rotation jr
    JOIN t_shift s ON jr.shift_id = s.id
    WHERE jr.staff_id = :staffId 
    AND jr.role = :role
    AND jr.rotation_date = CURDATE()
    AND jr.status IN ('PENDING', 'IN_PROGRESS')
    ORDER BY s.start_time ASC
    """, nativeQuery = true)
    List<JobRotation> findTodayJobs(@Param("staffId") Integer staffId,
                                    @Param("role") String role);

    /**
     * Lấy công việc sắp tới (rotation_date > hiện tại)
     */
    @Query(value = """
    SELECT jr.* FROM t_job_rotation jr
    JOIN t_shift s ON jr.shift_id = s.id
    WHERE jr.staff_id = :staffId 
    AND jr.role = :role
    AND jr.rotation_date > CURDATE()
    AND jr.status = 'PENDING'
    ORDER BY jr.rotation_date ASC, s.start_time ASC
    """, nativeQuery = true)
    List<JobRotation> findUpcomingJobs(@Param("staffId") Integer staffId,
                                       @Param("role") String role);

    /**
     * Lấy công việc đã quá hạn (rotation_date < hiện tại và chưa hoàn thành)
     */
    @Query(value = """
    SELECT jr.* FROM t_job_rotation jr
    WHERE jr.staff_id = :staffId 
    AND jr.role = :role
    AND jr.rotation_date < CURDATE()
    AND jr.status NOT IN ('COMPLETED', 'FAIL')
    ORDER BY jr.rotation_date ASC
    """, nativeQuery = true)
    List<JobRotation> findOverdueJobs(@Param("staffId") Integer staffId,
                                      @Param("role") String role);

    /**
     * Lấy công việc trong ca hiện tại
     */
    @Query(value = """
    SELECT jr.* FROM t_job_rotation jr
    JOIN t_shift s ON jr.shift_id = s.id
    WHERE jr.staff_id = :staffId 
    AND jr.role = :role
    AND jr.rotation_date = CURDATE()
    AND CURTIME() BETWEEN s.start_time AND s.end_time
    AND jr.status IN ('PENDING', 'IN_PROGRESS')
    ORDER BY s.start_time ASC
    """, nativeQuery = true)
    List<JobRotation> findCurrentShiftJobs(@Param("staffId") Integer staffId,
                                           @Param("role") String role);

    /**
     * Cập nhật trạng thái công việc
     */
    @Modifying
    @Transactional
    @Query(value = """
    UPDATE t_job_rotation 
    SET status = :status, updated_at = NOW()
    WHERE id = :jobRotationId
    """, nativeQuery = true)
    int updateJobStatus(@Param("jobRotationId") Integer jobRotationId,
                        @Param("status") String status);

    /**
     * Lấy danh sách công việc theo địa điểm trong ngày
     */
    @Query(value = """
    SELECT jr.* FROM t_job_rotation jr
    WHERE jr.position_id = :positionId 
    AND jr.rotation_date = :rotationDate
    ORDER BY jr.created_at ASC
    """, nativeQuery = true)
    List<JobRotation> findJobsByPositionAndDate(@Param("positionId") Integer positionId,
                                                @Param("rotationDate") LocalDate rotationDate);

    /**
     * Lấy công việc theo ca làm việc
     */
    @Query(value = """
    SELECT jr.* FROM t_job_rotation jr
    WHERE jr.shift_id = :shiftId
    AND jr.rotation_date = :rotationDate
    ORDER BY jr.created_at ASC
    """, nativeQuery = true)
    List<JobRotation> findJobsByShiftAndDate(@Param("shiftId") Integer shiftId,
                                             @Param("rotationDate") LocalDate rotationDate);

    /**
     * Lấy công việc theo nhân viên và ca làm việc
     */
    @Query(value = """
    SELECT jr.* FROM t_job_rotation jr
    WHERE jr.staff_id = :staffId
    AND jr.shift_id = :shiftId
    AND jr.rotation_date = :rotationDate
    """, nativeQuery = true)
    List<JobRotation> findJobsByStaffAndShiftAndDate(@Param("staffId") Integer staffId,
                                                     @Param("shiftId") Integer shiftId,
                                                     @Param("rotationDate") LocalDate rotationDate);
}