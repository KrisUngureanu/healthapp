package com.sportfd.healthapp.controller;

import com.sportfd.healthapp.dto.PatientCreateRequest;
import com.sportfd.healthapp.integration.oura.OuraService;
import com.sportfd.healthapp.integration.whoop.WhoopService;
import com.sportfd.healthapp.model.*;
import com.sportfd.healthapp.repo.PatientRepository;
import com.sportfd.healthapp.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller

public class PatientController {

    private final PatientService patientService;
    private final PatientRepository patientRepository;
    private final OuraService ouraService;
    private final WhoopService whoopService;

    public PatientController(PatientService patientService, PatientRepository patientRepository, OuraService ouraService, WhoopService whoopService) {
        this.patientService = patientService;
        this.patientRepository = patientRepository;


        this.ouraService = ouraService;
        this.whoopService = whoopService;
    }


    @GetMapping("/patients/new")
    public String newPatientForm(@ModelAttribute("patientForm") PatientCreateRequest form) {

        return "addpatient";
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

        return "redirect:/?created";
    }

    @GetMapping("/patients/{id}")
    public String patientDetails(@PathVariable Long id, Model model) {
        Patient p = patientRepository.findById(id).orElseThrow();
        model.addAttribute("patient", p);
        String device = p.getDevice();
        String status = p.getStatus();
        if (device != null && status.equals("active")) {
            if (device.equals("oura")) {
                List<OuraActivity> ouraActivities = ouraService.getOuraActivity(id);

                model.addAttribute("ouraActivities", ouraActivities);

                List<OuraDaylySleep> ouraDaylySleep = ouraService.getOuraDaylySleep(id);

                model.addAttribute("ouraDaylySleep", ouraDaylySleep);


                List<OuraHeartRate> ouraHeartRate = ouraService.getOuraHeartRate(id);

                model.addAttribute("ouraHeartRate", ouraHeartRate);


                List<OuraReadiness> ouraReadiness = ouraService.getOuraReadiness(id);

                model.addAttribute("ouraReadiness", ouraReadiness);


                List<OuraSleep> ouraSleep = ouraService.getOuraSleep(id);

                model.addAttribute("ouraSleep", ouraSleep);

                List<OuraSpo> ouraSpo = ouraService.getOuraSpo(id);

                model.addAttribute("ouraSpo", ouraSpo);

            } else if (device.equals("whoop")) {
                List<WhoopCycle> whoopCycles = whoopService.getWhoopCycle(id);

                model.addAttribute("whoopCycles", whoopCycles);


                List<WhoopRecovery> whoopRecovery = whoopService.getWhoopRecovery(id);

                model.addAttribute("whoopRecovery", whoopRecovery);


                List<WhoopSleep> whoopSleep = whoopService.getWhoopSleep(id);

                model.addAttribute("whoopSleep", whoopSleep);


                List<WhoopWorkout> whoopWorkout = whoopService.getWhoopWorkout(id);

                model.addAttribute("whoopWorkout", whoopWorkout);


            }

        }
        return "patientInfo";
    }


}