package com.sportfd.healthapp.controller;

import com.sportfd.healthapp.dto.PatientCreateRequest;
import com.sportfd.healthapp.model.HrSample;
import com.sportfd.healthapp.model.Patient;
import com.sportfd.healthapp.model.SleepRecord;
import com.sportfd.healthapp.repo.HrSampleRepository;
import com.sportfd.healthapp.repo.PatientRepository;
import com.sportfd.healthapp.repo.SleepRepository;
import com.sportfd.healthapp.service.PatientMetricsService;
import com.sportfd.healthapp.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Controller

public class PatientController {
    private final SleepRepository sleeps;
    private final PatientService patientService;
    private final PatientRepository patientRepository;
    private final PatientMetricsService patientMetricsService;
    public PatientController(SleepRepository sleeps, PatientService patientService, PatientRepository patientRepository, PatientMetricsService patientMetricsService) { this.sleeps = sleeps;
        this.patientService = patientService;
        this.patientRepository = patientRepository;

        this.patientMetricsService = patientMetricsService;
    }


    @GetMapping("/patients/new")
    public String newPatientForm(@ModelAttribute("patientForm") PatientCreateRequest form) {

        return "addpatient";
    }

    @Operation(summary = "Сон пациента за N дней (по умолчанию 7)")
    @GetMapping("/patients/{id}/sleep")
    public List<SleepRecord> getSleep(@PathVariable Long id, @RequestParam(name="days", required=false, defaultValue="7") int days) {
        OffsetDateTime after = OffsetDateTime.now(ZoneOffset.UTC).minusDays(days);
        return sleeps.findByPatientIdAndStartTimeAfterOrderByStartTimeDesc(id, after);
    }


    @PostMapping("/patients")
    public String createPatient(@Valid @ModelAttribute("patientForm") PatientCreateRequest form,
                                BindingResult binding,
                                @AuthenticationPrincipal UserDetails user,
                                Model model) {

        if (binding.hasErrors()) {
            return "addpatient";
        }

        try {
            patientService.createPatient(form, user);
        } catch (IllegalArgumentException ex) {
            binding.reject("createError", ex.getMessage());
            return "addpatient";
        }

        // обратно на главную со списком пациентов
        return "redirect:/?created";
    }

    @GetMapping("/patients/{id}")
    public String patientDetails(@PathVariable Long id, Model model) {
        Patient p = patientRepository.findById(id).orElseThrow();
        model.addAttribute("patient", p);
        var zone = java.time.ZoneId.of("Asia/Almaty"); // можно вынести в настройки/профиль
        var m = patientMetricsService.loadToday(id, zone);
        model.addAttribute("m", m);  // одна обёртка на все таблицы


        return "patientInfo";
    }


}