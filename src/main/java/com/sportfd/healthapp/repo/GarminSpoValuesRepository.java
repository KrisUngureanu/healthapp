package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.GarminSpoValues;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GarminSpoValuesRepository extends JpaRepository<GarminSpoValues, Long> {
    List<GarminSpoValues> findAllBySummaryId(String summaryId);
}
