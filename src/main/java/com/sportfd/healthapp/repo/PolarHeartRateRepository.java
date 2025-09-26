package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.PolarHeartRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public interface PolarHeartRateRepository extends JpaRepository<PolarHeartRate, Long> {
    @Query("select w from PolarHeartRate w where w.patientId = :patientId and w.date = :date and w.sampleTime = :sampleTime")
    Optional<PolarHeartRate> findByPatientIdAndDateAndSampleTime(Long patientId, LocalDate date, LocalTime sampleTime);

    void deleteByPatientId(Long pid);
}
