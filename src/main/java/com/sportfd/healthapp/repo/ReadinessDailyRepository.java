package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.ReadinessDaily;
import com.sportfd.healthapp.model.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReadinessDailyRepository extends JpaRepository<ReadinessDaily, Long> {
    Optional<ReadinessDaily> findByPatientIdAndProviderAndDay(Long pid, Provider provider, LocalDate day);
    List<ReadinessDaily> findByPatientIdAndProviderAndDayBetween(Long pid, Provider provider, LocalDate start, LocalDate end);
    List<ReadinessDaily> findByPatientIdAndDay(Long patientId, LocalDate day);
}