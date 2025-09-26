package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.PolarTemperatureSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PolarTemperatureSampleRepository extends JpaRepository<PolarTemperatureSample, Long> {
    void deleteByTemperatureId(Long temperatureId);

    @Query("select w from PolarTemperatureSample w where w.temperatureId = :temperatureId and w.sampleTime = :sampleTime")
    Optional<PolarTemperatureSample> findByTemperatureIdAndSampleTime(@Param("temperatureId") Long temperatureId, @Param("sampleTime") OffsetDateTime sampleTime);

    void deleteByPatientId(Long pid);
}
