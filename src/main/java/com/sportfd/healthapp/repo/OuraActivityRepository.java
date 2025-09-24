package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.OuraActivity;
import com.sportfd.healthapp.model.OuraDaylySleep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OuraActivityRepository extends JpaRepository<OuraActivity, Long> {
    @Query("select w from OuraActivity w where w.record_id = :recordId")
    Optional<OuraActivity> findByRecordId(@Param("recordId") String recordId);


    @Query("select d from OuraActivity d where d.patient_id = :pid")
    List<OuraActivity> findByPatientId(@Param("pid") Long patientId);
}
