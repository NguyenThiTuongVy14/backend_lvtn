package com.example.test.repository;

import com.example.test.dto.JobRotationDetailDTO;
import com.example.test.entity.JobRotation;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
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

    @Modifying
    @Transactional
    @Query(value = """
    UPDATE t_job_rotation jr
    SET jr.status = CASE
        WHEN TIMESTAMPDIFF(HOUR, jr.end_date, jr.updated_at) > 1
             AND jr.updated_at IS NOT NULL
             AND jr.status IN ('PENDING', 'ASSIGNED')
        THEN 'FAIL'
                 
        WHEN jr.updated_at > jr.end_date
             AND TIMESTAMPDIFF(HOUR, jr.end_date, jr.updated_at) <= 1
             AND jr.status IN ('PENDING', 'ASSIGNED')
        THEN 'LATE'
                 
        ELSE jr.status
    END """, nativeQuery = true)
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
    WHERE s.user_name = :userName """, nativeQuery = true)
    List<JobRotationDetailDTO> findByUserName(@Param("userName") String userName);

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
    AND DATE(jr.start_date) = DATE(:workDate)
    ORDER BY jr.start_date ASC
    """, nativeQuery = true)
    List<JobRotation> findCollectorJobsByDate(@Param("staffId") Integer staffId,
                                              @Param("workDate") Date workDate);

    // ===========================================
    // DRIVER-SPECIFIC METHODS
    // ===========================================

    /**
     * Lấy danh sách công việc đang pending của tài xế
     */
    List<JobRotation> findByStaffIdAndRoleAndStatusOrderByStartDateAsc(Integer staffId, String role, String status);

    /**
     * Lấy danh sách công việc của tài xế theo ngày
     */
    @Query(value = """
    SELECT jr.* FROM t_job_rotation jr
    WHERE jr.staff_id = :staffId 
    AND jr.role = 'DRIVER'
    AND DATE(jr.start_date) = DATE(:workDate)
    ORDER BY jr.start_date ASC
    """, nativeQuery = true)
    List<JobRotation> findDriverJobsByDate(@Param("staffId") Integer staffId,
                                           @Param("workDate") Date workDate);

    /**
     * Lấy chi tiết công việc tài xế với thông tin đầy đủ
     */
    @Query(value = """
    SELECT 
        jr.id as job_rotation_id,
        jr.status,
        jr.role,
        jr.start_date,
        jr.end_date,
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
        v.capacity
        
    FROM t_job_rotation jr
    JOIN t_user s ON jr.staff_id = s.id
    JOIN t_job_position jp ON jr.position_id = jp.id
    LEFT JOIN t_vehicle v ON jr.vehicle_id = v.id
    
    WHERE jr.staff_id = :staffId 
    AND jr.role = 'DRIVER'
    AND jr.status = :status
    ORDER BY jr.start_date ASC
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
        jr.start_date,
        jr.end_date,
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
        v.capacity
        
    FROM t_job_rotation jr
    JOIN t_user s ON jr.staff_id = s.id
    JOIN t_job_position jp ON jr.position_id = jp.id
    LEFT JOIN t_vehicle v ON jr.vehicle_id = v.id
    
    WHERE jr.staff_id = :staffId 
    AND jr.role = 'COLLECTOR'
    AND jr.status = :status
    ORDER BY jr.start_date ASC
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
    ORDER BY jr.start_date ASC, jr.created_at ASC
    """, nativeQuery = true)
    List<JobRotation> findAllDriverJobs(@Param("staffId") Integer staffId);

    /**
     * Lấy tất cả công việc của collector (không phân biệt status)
     */
    @Query(value = """
    SELECT jr.* FROM t_job_rotation jr
    WHERE jr.staff_id = :staffId 
    AND jr.role = 'COLLECTOR'
    ORDER BY jr.start_date ASC, jr.created_at ASC
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
    AND DATE(jr.start_date) BETWEEN DATE(:startDate) AND DATE(:endDate)
    ORDER BY jr.start_date ASC
    """, nativeQuery = true)
    List<JobRotation> findJobsByDateRange(@Param("staffId") Integer staffId,
                                          @Param("role") String role,
                                          @Param("startDate") Date startDate,
                                          @Param("endDate") Date endDate);

    /**
     * Lấy công việc đang diễn ra (trong khoảng thời gian start_date và end_date)
     */
    @Query(value = """
    SELECT jr.* FROM t_job_rotation jr
    WHERE jr.staff_id = :staffId 
    AND jr.role = :role
    AND NOW() BETWEEN jr.start_date AND jr.end_date
    AND jr.status IN ('PENDING', 'IN_PROGRESS')
    ORDER BY jr.start_date ASC
    """, nativeQuery = true)
    List<JobRotation> findActiveJobs(@Param("staffId") Integer staffId,
                                     @Param("role") String role);

    /**
     * Lấy công việc sắp tới (start_date > hiện tại)
     */
    @Query(value = """
    SELECT jr.* FROM t_job_rotation jr
    WHERE jr.staff_id = :staffId 
    AND jr.role = :role
    AND jr.start_date > NOW()
    AND jr.status = 'PENDING'
    ORDER BY jr.start_date ASC
    """, nativeQuery = true)
    List<JobRotation> findUpcomingJobs(@Param("staffId") Integer staffId,
                                       @Param("role") String role);

    /**
     * Lấy công việc đã quá hạn (end_date < hiện tại và chưa hoàn thành)
     */
    @Query(value = """
    SELECT jr.* FROM t_job_rotation jr
    WHERE jr.staff_id = :staffId 
    AND jr.role = :role
    AND jr.end_date < NOW()
    AND jr.status NOT IN ('COMPLETED', 'FAIL')
    ORDER BY jr.end_date ASC
    """, nativeQuery = true)
    List<JobRotation> findOverdueJobs(@Param("staffId") Integer staffId,
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
    AND DATE(jr.start_date) = DATE(:workDate)
    ORDER BY jr.start_date ASC
    """, nativeQuery = true)
    List<JobRotation> findJobsByPositionAndDate(@Param("positionId") Integer positionId,
                                                @Param("workDate") Date workDate);
}