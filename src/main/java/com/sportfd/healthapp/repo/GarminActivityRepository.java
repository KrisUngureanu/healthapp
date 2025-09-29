package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.GarminActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GarminActivityRepository extends JpaRepository<GarminActivity, Long> {
    Optional<GarminActivity> findByActivityId(String activityId);
}
