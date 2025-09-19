package com.sportfd.healthapp.repo;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportfd.healthapp.model.HrSample;
import com.sportfd.healthapp.model.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface HrSampleRepository extends JpaRepository<HrSample, Long> {
    List<HrSample> findByPatientIdAndProviderAndTsBetweenOrderByTsAsc(Long pid, Provider provider, OffsetDateTime from, OffsetDateTime to);

    Optional<HrSample> findByPatientIdAndProviderAndTs(
            Long pid, Provider provider, OffsetDateTime ts);

    List<HrSample> findByPatientIdAndTsBetweenOrderByTsAsc(
            Long patientId, OffsetDateTime from, OffsetDateTime to);
}