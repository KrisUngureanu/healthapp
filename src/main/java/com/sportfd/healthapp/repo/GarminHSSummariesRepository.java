package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.GarminHSSummaries;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GarminHSSummariesRepository extends JpaRepository<GarminHSSummaries, Long> {
    List<GarminHSSummaries> findAllBySummaryId(String summaryId);
}
