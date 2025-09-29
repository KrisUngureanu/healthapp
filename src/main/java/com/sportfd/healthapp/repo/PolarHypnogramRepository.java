package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.PolarHypnogram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PolarHypnogramRepository extends JpaRepository<PolarHypnogram, Long> {
    @Modifying
    @Transactional
    void deleteBySleepId(Long sleepId);

    List<PolarHypnogram> findAllByPatientIdAndSleepId(Long pid, Long id);
    PolarHypnogram findByPatientIdAndSleepId(Long pid, Long id);

    List<PolarHypnogram> findAllBySleepId(Long sleepId);
}
