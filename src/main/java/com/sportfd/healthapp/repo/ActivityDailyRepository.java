package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.ActivityDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sportfd.healthapp.model.ActivityDaily;
import com.sportfd.healthapp.model.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ActivityDailyRepository extends JpaRepository<ActivityDaily, Long> {
    Optional<ActivityDaily> findByPatientIdAndProviderAndDay(Long pid, Provider provider, LocalDate day);
    List<ActivityDaily> findByPatientIdAndProviderAndDayBetween(Long pid, Provider provider, LocalDate start, LocalDate end);
    List<ActivityDaily> findByPatientIdAndDay(Long patientId, LocalDate day);
}