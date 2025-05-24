package com.example.test.repository;

import com.example.test.entity.CollectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollectionRequestRepository extends JpaRepository<CollectionRequest, Integer> {

    // Lấy danh sách yêu cầu thu gom theo collector
    List<CollectionRequest> findByCollectorId(Integer collectorId);

    // Lấy danh sách yêu cầu thu gom theo trạng thái
    List<CollectionRequest> findByStatus(String status);

    // Lấy danh sách yêu cầu thu gom theo người tạo
//    List<CollectionRequest> findByCreatedBy(String createdBy);

    // Lấy danh sách yêu cầu thu gom theo collector và trạng thái
    @Query("SELECT c FROM CollectionRequest c WHERE c.collector.id = :collectorId AND c.status = :status")
    List<CollectionRequest> findByCollectorIdAndStatus(@Param("collectorId") Integer collectorId, @Param("status") String status);

    // Đếm số lượng yêu cầu thu gom theo trạng thái
    @Query("SELECT c.status, COUNT(c) FROM CollectionRequest c GROUP BY c.status")
    List<Object[]> countByStatus();
}