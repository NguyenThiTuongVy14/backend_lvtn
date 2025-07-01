package com.example.test.repository;

import com.example.test.entity.JobPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface JobPositionRepository extends JpaRepository<JobPosition, Integer> {
    @Query(value = "SELECT * FROM t_job_position jp " +
            "WHERE (6371 * acos(cos(radians(:lat)) * cos(radians(jp.lat)) * " +
            "cos(radians(jp.lng) - radians(:lng)) + " +
            "sin(radians(:lat)) * sin(radians(jp.lat)))) <= :distance",
            nativeQuery = true)
    List<JobPosition> findNearbyPositions(@Param("lat") BigDecimal lat,
                                          @Param("lng") BigDecimal lng,
                                          @Param("distance") Double distance);
    // Tìm danh sách vị trí công việc theo trạng thái
    List<JobPosition> findByStatus(String status);

    // Tìm vị trí công việc theo ID và trạng thái
    List<JobPosition> findByIdAndStatus(Integer id, String status);

    String findNameById(Integer positionId);

    // Tìm vị trí công việc theo tải trọng yêu cầu
}