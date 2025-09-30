package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.GarminHrvValues;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GarminHrvValuesRepository extends JpaRepository<GarminHrvValues, Long> {
}
