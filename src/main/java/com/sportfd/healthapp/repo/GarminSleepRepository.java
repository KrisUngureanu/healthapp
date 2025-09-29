package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.GarminSleep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GarminSleepRepository extends JpaRepository<GarminSleep, Long> {
    Optional<GarminSleep> findBySleepId(String sleepId);
}
