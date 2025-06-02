package com.example.test.repository;

import com.example.test.entity.JobRotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRotationRepository extends JpaRepository<JobRotation, Integer> {
    // Tìm lịch phân công theo ID nhân viên
    List<JobRotation> findByStaffId(Integer staffId);

    // Tìm lịch phân công theo ID nhân viên và trạng thái
    List<JobRotation> findByStaffIdAndStatus(Integer staffId, String status);

    // Tìm lịch phân công theo trạng thái
    List<JobRotation> findByStatus(String status);

    // Thống kê số lượng lịch phân công theo trạng thái
    @Query("SELECT jr.status, COUNT(jr) FROM JobRotation jr GROUP BY jr.status")
    List<Object[]> countByStatus();

    // Thống kê lịch phân công theo tên vị trí công việc
    @Query(value = "SELECT jp.name, COUNT(jr.id) " +
            "FROM t_job_rotation jr " +
            "JOIN t_job_position jp ON jr.job_position_id = jp.id " +
            "GROUP BY jp.name", nativeQuery = true)
    List<Object[]> getRotationStatistics();

    Optional<JobRotation> findByStaffIdAndJobPositionId(Integer staffId, Integer jobPositionId);
    List<JobRotation> findByJobPositionIdAndStatus(Integer jobPositionId, String status);
}