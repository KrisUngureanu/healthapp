package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.GarminDailySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GarminDailySummaryRepository extends JpaRepository<GarminDailySummary, Long> {
    GarminDailySummary findBySummaryId(String summaryId);

    List<GarminDailySummary> findAllByPatientId(Long patientId);
}
