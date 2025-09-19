package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.ActivitySession;
import com.sportfd.healthapp.model.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ActivitySessionRepository extends JpaRepository<ActivitySession, Long> {
    Optional<ActivitySession> findByPatientIdAndProviderAndSourceId(Long pid, Provider provider, String sourceId);
    List<ActivitySession> findByPatientIdAndProviderAndStartTimeBetween(Long pid, Provider provider, OffsetDateTime from, OffsetDateTime to);
    List<ActivitySession> findByPatientIdAndStartTimeBetweenOrderByStartTimeAsc(
            Long patientId, OffsetDateTime from, OffsetDateTime to);
}