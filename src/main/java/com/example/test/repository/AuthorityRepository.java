package com.example.test.repository;

import com.example.test.entity.Authority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorityRepository extends JpaRepository<Authority, Integer> {

    /**
     * Tìm kiếm authority theo tên
     */
    Optional<Authority> findByName(String authorityName);

    /**
     * Kiểm tra authority có tồn tại không
     */
    boolean existsByName(String authorityName);
}