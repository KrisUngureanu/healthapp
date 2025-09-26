package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.PolarNightRecharge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PolarNightRechargeRepository extends JpaRepository<PolarNightRecharge, Long> {
    Optional<PolarNightRecharge> findByPatientIdAndDate(Long patientId, OffsetDateTime date);

    void deleteByPatientId(Long pid);
}
