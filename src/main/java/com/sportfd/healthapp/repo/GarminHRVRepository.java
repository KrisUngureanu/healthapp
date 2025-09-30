package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.GarminHRV;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GarminHRVRepository extends JpaRepository<GarminHRV, Long> {
    GarminHRV findBySummaryId(String summaryId);
}
