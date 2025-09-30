package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.GarminTemperature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GarminTemperatureRepository extends JpaRepository<GarminTemperature, Long> {
    GarminTemperature findBySummaryId(String summaryId);

    List<GarminTemperature> findAllByPatientId(Long patientId);
}
