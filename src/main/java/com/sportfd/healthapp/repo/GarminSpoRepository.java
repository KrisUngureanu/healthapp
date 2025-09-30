package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.GarminSpo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GarminSpoRepository extends JpaRepository<GarminSpo, Long> {
    GarminSpo findBySummaryId(String summaryId);

    List<GarminSpo> findAllByPatientId(Long patientId);
}
