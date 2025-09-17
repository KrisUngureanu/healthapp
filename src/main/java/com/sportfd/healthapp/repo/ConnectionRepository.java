package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.Connection;
import com.sportfd.healthapp.model.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {
    List<Connection> findByPatientId(Long patientId);
    boolean existsByPatientIdAndProvider(Long patientId, Provider provider);
}
