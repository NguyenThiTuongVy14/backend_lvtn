package com.example.test.repository;

import com.example.test.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Integer> {


    Optional<Route> findRouteByRotationId(Integer rotationId);

    List<Route> findByVehicleId(Integer id);
}
