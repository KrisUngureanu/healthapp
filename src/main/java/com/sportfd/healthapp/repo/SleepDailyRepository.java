package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.SleepDaily;
import com.sportfd.healthapp.model.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SleepDailyRepository extends JpaRepository<SleepDaily, Long> {
    Optional<SleepDaily> findByPatientIdAndProviderAndDay(Long patientId, Provider provider, LocalDate day);
    List<SleepDaily> findByPatientIdAndProviderAndDayBetween(Long pid, Provider provider, LocalDate start, LocalDate end);
    List<SleepDaily> findByPatientIdAndDay(Long patientId, LocalDate day);
}