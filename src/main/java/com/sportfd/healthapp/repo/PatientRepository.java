package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    List<Patient> findAllByDoctorId(Long doctorId);
}
