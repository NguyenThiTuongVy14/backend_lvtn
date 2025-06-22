package com.example.test.repository;

import com.example.test.entity.RotationStoreId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public  interface RotationStoreIdRepository extends JpaRepository<RotationStoreId, Integer> {
    List<RotationStoreId> findByRotationIdCollector(Integer rotationIdCollector);
}
