package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.PolarHeartRate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface PolarHeartRateRepository extends JpaRepository<PolarHeartRate, Long> {
    @Query("select w from PolarHeartRate w where w.patientId = :patientId and w.date = :date and w.sampleTime = :sampleTime")
    Optional<PolarHeartRate> findByPatientIdAndDateAndSampleTime(Long patientId, LocalDate date, LocalTime sampleTime);
    @Modifying
    @Transactional
    void deleteByPatientId(Long pid);

    List<PolarHeartRate> findByPatientId(Long pid,  Pageable pageable);


    List<PolarHeartRate> findByPatientIdOrderByDateAscSampleTimeAsc(Long patientId, Pageable pageable);
}
