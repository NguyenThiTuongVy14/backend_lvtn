package com.example.test.repository;

import com.example.test.dto.DriverJobWithCollectorStatusDTO;
import com.example.test.dto.JobRotationDetailDTO;
import com.example.test.entity.JobRotation;
import com.example.test.entity.JobRotationTemp;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRotationTempRepository extends JpaRepository<JobRotationTemp, Integer> {

    // Tìm lịch phân công theo ID nhân viên
    List<JobRotationTemp> findByStaffId(Integer staffId);

    // Thống kê lịch phân công theo tên vị trí công việc
    @Query(value = "SELECT jp.name, COUNT(jr.id) " +
            "FROM t_job_rotation_temp jr " +
            "JOIN t_job_position jp ON jr.position_id = jp.id " +
            "GROUP BY jp.name", nativeQuery = true)
    List<Object[]> getRotationStatistics();

    // Cập nhật trạng thái dựa trên thời gian ca làm việc
    @Modifying
    @Transactional
    @Query(value = """
    UPDATE t_job_rotation_temp jr
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
    FROM t_job_rotation_temp jr
    JOIN t_user s ON jr.staff_id = s.id
    JOIN t_job_position jp ON jr.job_position_id = jp.id
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
    SELECT jr.* FROM t_job_rotation_temp jr
    WHERE jr.staff_id = :staffId 
    AND jr.role = 'DRIVER'
    ORDER BY jr.rotation_date ASC, jr.created_at ASC
    """, nativeQuery = true)
    List<JobRotationTemp> findAllDriverJobs(@Param("staffId") Integer staffId);


    @Query(value = """
    SELECT jr.* FROM t_job_rotation_temp jr
    WHERE jr.staff_id = :staffId 
    AND jr.role = 'COLLECTOR'
    ORDER BY jr.rotation_date ASC, jr.created_at ASC
    """, nativeQuery = true)
    List<JobRotationTemp> findAllCollectorJobs(@Param("staffId") Integer staffId);


    // Trong method lấy danh sách job của driver, có thể join để biết trạng thái:
    @Query("SELECT j FROM JobRotationTemp j " +
            "LEFT JOIN RotationStoreId rs ON rs.rotationIdDriver = j.id " +
            "WHERE j.staffId = :driverId AND j.role = 'DRIVER' AND rs.isCompleted = true")
    List<JobRotationTemp> findDriverJobsWithCompletionStatus(@Param("driverId") Integer driverId);

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
            
            
        FROM t_job_rotation_temp jr
        JOIN t_user s ON jr.staff_id = s.id
        JOIN t_job_position jp ON jr.position_id = jp.id
        JOIN t_shift sh ON jr.shift_id = sh.id
        LEFT JOIN t_vehicle v ON jr.vehicle_id = v.id
        WHERE s.user_name = :userName
        AND DATE(jr.rotation_date) = DATE(:rotationDate)
        ORDER BY jr.created_at DESC
        """, nativeQuery = true)
    List<DriverJobWithCollectorStatusDTO> findDriverJobsWithCollectorStatusByDate(
            @Param("userName") String userName,
            @Param("rotationDate") LocalDate rotationDate);




    @Query(value = """
            SELECT t_job_rotation_temp.*
            FROM t_job_rotation_temp
            LEFT JOIN t_shift ON t_job_rotation_temp.shift_id = t_shift.id
            LEFT JOIN t_user ON t_job_rotation_temp.staff_id = t_user.id
            WHERE t_job_rotation_temp.status = ''
            AND t_job_rotation_temp.role = 'DRIVER'
            AND left_tonnage > :tonnage
            AND NOW() BETWEEN t_shift.start_time AND t_shift.end_time
            ORDER BY t_user.rate DESC LIMIT 1;
                                                    
    """, nativeQuery = true)
    Optional<JobRotationTemp> findDriver(@Param("tonnage") Integer tonnage);


    boolean existsByVehicleIdAndRotationDate(Integer vehicleId, LocalDate date);

    List<JobRotationTemp> findByStaffIdAndRotationDateAndStatus(Integer id, LocalDate now, String pending);


    List<JobRotationTemp> findByRoleAndStatusAndRotationDate(String collector, String completed, LocalDate now);
    @Modifying
    @Query("UPDATE JobRotationTemp jr SET jr.status = :status WHERE jr.id = :jobId")
    void updateJobStatus(@Param("jobId") Integer jobId, @Param("status") String status);

    boolean existsByStaffIdAndRotationDateAndShiftId(Integer staffId, LocalDate rotationDate, Integer shiftId);

    @Query("SELECT jr FROM JobRotationTemp jr " +
            "WHERE jr.staffId = :driverId " +
            "AND jr.rotationDate = :date " +
            "AND jr.status = :status " +
            "AND jr.role = 'DRIVER'")
    List<JobRotationTemp> findCurrentDriverJobs(Integer driverId, LocalDate date, String status);

    @Query("SELECT jr.id, jr.jobPositionId " +
            "FROM JobRotationTemp jr " +
            "WHERE jr.rotationDate = :date " +
            "AND jr.status = 'COMPLETED' " +
            "AND jr.role = 'COLLECTOR' " +
            "AND jr.shiftId = :shiftId")
    List<Object[]> findCompletedCollectionsWithTruckCounts(
            @Param("date") LocalDate date,
            @Param("shiftId") Integer shiftId
    );

    List<JobRotationTemp> findByRotationDateAndShiftIdAndRole(LocalDate date, Integer shiftId, String driver);

    List<JobRotationTemp> findByVehicleIdAndRotationDateAndShiftId(Integer vehicleId, LocalDate date, Integer shiftId);

    @Query("SELECT jr FROM JobRotationTemp jr WHERE jr.staffId = :staffId " +
            "AND jr.rotationDate = :rotationDate " +
            "AND jr.shiftId = :shiftId " +
            "AND jr.status != :excludeStatus")
    List<JobRotationTemp> findByStaffIdAndRotationDateAndShiftIdAndStatusNot(
            @Param("staffId") Integer staffId,
            @Param("rotationDate") LocalDate rotationDate,
            @Param("shiftId") Integer shiftId,
            @Param("excludeStatus") String excludeStatus
    );

    List<JobRotationTemp> findByVehicleIdNullAndRole(String role);
}