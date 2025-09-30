package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.GarminHealthSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GarminHealthSnapshotRepository extends JpaRepository<GarminHealthSnapshot, Long> {
    GarminHealthSnapshot findBySummaryId(String summaryId);

    List<GarminHealthSnapshot> findAllByPatientId(Long patientId);
}
