package com.example.test.repository;

import com.example.test.entity.Vehicle;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {
    List<Vehicle> findByStatus(String status);

    @Query("SELECT v FROM Vehicle v " +
            "WHERE v.status = 'AVAILABLE' " +
            "AND v.tonnage >= :requiredTonnage " +
            "AND NOT EXISTS (" +
            "   SELECT 1 FROM JobRotation jr " +
            "   WHERE jr.vehicleId = v.id " +
            "   AND jr.rotationDate = :date" +
            ")")
    List<Vehicle> findAvailableVehicles(
            @Param("requiredTonnage") BigDecimal requiredTonnage,
            @Param("date") LocalDate date
    );

    List<Vehicle> findByStatusAndTonnageGreaterThanEqual(String available, BigDecimal requiredTonnage);

    @Modifying
    @Transactional
    @Query("UPDATE Vehicle v SET v.status = :status WHERE v.id = :vehicleId")
    void updateStatus(@Param("vehicleId") Integer vehicleId, @Param("status") String status);


    List<Vehicle> findByStatusIn(List<String> list);
}
