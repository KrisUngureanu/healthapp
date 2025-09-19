package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.Connection;
import com.sportfd.healthapp.model.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {
    List<Connection> findByPatientId(Long patientId);
    boolean existsByPatientIdAndProvider(Long patientId, Provider provider);
    Optional<Connection> findByPatientIdAndProvider(Long patientId, String provider);
    boolean existsByPatientIdAndProvider(Long patientId, String provider);
    void deleteByPatientIdAndProvider(Long patientId, String provider);


    Optional<Connection> findByPatientIdAndProvider(Long patientId, Provider provider);

    void deleteByPatientIdAndProvider(Long patientId, Provider provider);
}
