package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.PolarSleep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PolarSleepRepository extends JpaRepository<PolarSleep, Long> {
    PolarSleep findByPatientIdAndDate(Long patientId, LocalDate date);

    List<PolarSleep> findByPatientId(Long pid);
}