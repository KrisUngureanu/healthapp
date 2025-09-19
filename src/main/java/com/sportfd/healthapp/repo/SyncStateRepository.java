package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.SyncState;
import com.sportfd.healthapp.model.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SyncStateRepository extends JpaRepository<SyncState, Long> {
    Optional<SyncState> findByPatientIdAndProvider(Long pid, Provider provider);
}