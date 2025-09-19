package com.sportfd.healthapp.controller;

import com.sportfd.healthapp.integration.oura.OuraService;
import com.sportfd.healthapp.repo.PatientRepository;
import com.sportfd.healthapp.util.InviteTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class OuraDoctorController {

    private final InviteTokenUtil invite;
    private final OuraService ouraService;
    private final PatientRepository patientRepository;
    @Value("${app.base-url}") private String baseUrl;


    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @GetMapping("/patients/{id}/{provider}/invite")
    @ResponseBody
    public ResponseEntity<String> inviteLink(@PathVariable("id") Long patientId, @PathVariable("provider") String provider) {
        String url = invite.buildInviteUrl(baseUrl, patientId, provider ,60);
        return ResponseEntity.ok(url);
    }


    @PostMapping("/patients/{id}/{provider}/disconnect")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String disconnectOura(@PathVariable("id") Long patientId, @PathVariable("provider") String provider) {
        if (provider.equals("oura")){
            ouraService.disconnect(patientId);
        }

        patientRepository.findById(patientId).ifPresent(p -> {
            p.setStatus("inactive");
            patientRepository.save(p);
        });

        return "redirect:/patients/" + patientId + "?oura=disconnected";
    }
}