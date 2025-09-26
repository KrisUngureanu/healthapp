package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.PolarTemperature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PolarTemperatureRepository extends JpaRepository<PolarTemperature, Long> {


    @Query("select w from PolarTemperature w where w.start_time = :startTime and w.patientId = :patientId")
    Optional<PolarTemperature> findByPatientIdAndStart_time(@Param("patientId") Long patientId, @Param("startTime") OffsetDateTime startTime);

    void deleteByPatientId(Long pid);
}
