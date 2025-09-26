package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.PolarNightRecharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PolarNightRechargeRepository extends JpaRepository<PolarNightRecharge, Long> {
    Optional<PolarNightRecharge> findByPatientIdAndDate(Long patientId, OffsetDateTime date);
    @Modifying
    @Transactional
    void deleteByPatientId(Long pid);

    List<PolarNightRecharge> findByPatientId(Long pid);
}
