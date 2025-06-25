package com.example.test.repository;

import com.example.test.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftRepository extends JpaRepository<Shift, Integer> {

}
