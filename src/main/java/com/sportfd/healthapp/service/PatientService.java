package com.sportfd.healthapp.service;

import com.sportfd.healthapp.dto.PatientCreateRequest;
import com.sportfd.healthapp.integration.oura.OuraClient;
import com.sportfd.healthapp.integration.oura.OuraService;
import com.sportfd.healthapp.model.Patient;
import com.sportfd.healthapp.model.Users;
import com.sportfd.healthapp.repo.PatientRepository;
import com.sportfd.healthapp.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository usersRepository;



    @Transactional
    public Patient createPatient(PatientCreateRequest req, UserDetails currentUser) {
        // находим текущего врача
        Users me = usersRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));

        Patient p = new Patient();
        p.setFullname(req.getFullname());
        p.setDoctorId(me.getId());               // ← тут выставляем ID текущего пользователя
        p.setStatus("wait");

        return patientRepository.save(p);
    }
}