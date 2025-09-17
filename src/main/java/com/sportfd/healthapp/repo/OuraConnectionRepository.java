package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.OuraConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OuraConnectionRepository extends JpaRepository<OuraConnection, Long> {
    Optional<OuraConnection> findByUserId(Long userId);
    void deleteByUserId(Long userId);
    boolean existsByUserId(Long userId);
}