package com.example.test.repository;

import com.example.test.entity.DriverRating;
import io.micrometer.observation.ObservationFilter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRatingRepository extends JpaRepository<DriverRating,Integer> {

    ObservationFilter findByDriverId(Integer staffId);
}
