package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.SleepSession;
import com.sportfd.healthapp.model.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.time.OffsetDateTime;
import java.util.List;


public interface SleepSessionRepository extends JpaRepository<SleepSession, Long> {
    Optional<SleepSession> findByPatientIdAndProviderAndSourceId(Long pid, Provider provider, String sourceId);
    List<SleepSession> findByPatientIdAndProviderAndStartTimeBetween(Long pid, Provider provider, OffsetDateTime from, OffsetDateTime to);
    List<SleepSession> findByPatientIdAndStartTimeBetweenOrderByStartTimeAsc(
            Long patientId, OffsetDateTime from, OffsetDateTime to);
}