package com.sportfd.healthapp.repo;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportfd.healthapp.model.HrSample;
import com.sportfd.healthapp.model.Spo2Sample;
import com.sportfd.healthapp.model.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface Spo2SampleRepository extends JpaRepository<Spo2Sample, Long> {
    List<Spo2Sample> findByPatientIdAndProviderAndTsBetweenOrderByTsAsc(Long pid, Provider provider, OffsetDateTime from, OffsetDateTime to);

    Optional<HrSample> findByPatientIdAndProviderAndTs(Long patientId, Provider provider, OffsetDateTime ts);
    List<Spo2Sample> findByPatientIdAndTsBetweenOrderByTsAsc(
            Long patientId, OffsetDateTime from, OffsetDateTime to);
}