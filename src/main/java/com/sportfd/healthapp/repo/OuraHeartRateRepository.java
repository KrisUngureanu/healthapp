package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.OuraDaylySleep;
import com.sportfd.healthapp.model.OuraHeartRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OuraHeartRateRepository extends JpaRepository<OuraHeartRate, Long> {
    @Query("select d from OuraHeartRate d where d.patient_id = :pid")
    List<OuraHeartRate> findByPatientId(@Param("pid") Long patientId);
}
