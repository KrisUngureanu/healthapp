package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.PolarTemperatureSample;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PolarTemperatureSampleRepository extends JpaRepository<PolarTemperatureSample, Long> {
    @Modifying
    @Transactional
    void deleteByTemperatureId(Long temperatureId);

    @Query("select w from PolarTemperatureSample w where w.temperatureId = :temperatureId and w.sampleTime = :sampleTime")
    Optional<PolarTemperatureSample> findByTemperatureIdAndSampleTime(@Param("temperatureId") Long temperatureId, @Param("sampleTime") OffsetDateTime sampleTime);
    @Modifying
    @Transactional
    void deleteByPatientId(Long pid);

    List<PolarTemperatureSample> findByPatientId(Long pid, Pageable pageable);
}
