package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.PolarHeartRateSamplesSleep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PolarHeartRateSamplesSleepRepository extends JpaRepository<PolarHeartRateSamplesSleep, Long> {
    @Modifying
    @Transactional
    void deleteBySleepId(Long sleepId);


    List<PolarHeartRateSamplesSleep> findAllByPatientIdAndSleepId(Long pid, Long id);
    PolarHeartRateSamplesSleep findByPatientIdAndSleepId(Long pid, Long id);

    List<PolarHeartRateSamplesSleep> findAllBySleepId(Long sleepid);
}
