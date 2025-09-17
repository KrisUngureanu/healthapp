package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.RecoveryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecoveryRepository extends JpaRepository<RecoveryRecord, Long> {
}
