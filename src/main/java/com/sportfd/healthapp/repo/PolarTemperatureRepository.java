package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.PolarTemperature;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PolarTemperatureRepository extends JpaRepository<PolarTemperature, Long> {


    @Query("select w from PolarTemperature w where w.startTime = :startTime and w.patientId = :patientId")
    PolarTemperature findByPatientIdAndStart_time(@Param("patientId") Long patientId, @Param("startTime") OffsetDateTime startTime);
    @Modifying
    @Transactional
    void deleteByPatientId(Long pid);

    List<PolarTemperature> findByPatientId(Long pid, Pageable pageable);
}
