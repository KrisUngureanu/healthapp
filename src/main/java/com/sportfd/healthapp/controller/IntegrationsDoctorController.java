package com.sportfd.healthapp.controller;

import com.sportfd.healthapp.integration.ProviderClients;
import com.sportfd.healthapp.model.enums.Provider;
import com.sportfd.healthapp.repo.PatientRepository;
import com.sportfd.healthapp.util.InviteTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class IntegrationsDoctorController {

    private final InviteTokenUtil invite;
    private final ProviderClients clients;
    private final PatientRepository patients;

    @Value("${app.base-url}") private String baseUrl;

    @GetMapping("/patients/{id}/integrations/{provider}/invite")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @ResponseBody
    public String inviteLink(@PathVariable Long id, @PathVariable String provider) {
        // ссылка вида /oauth/{provider}/start?t=...
        String url = baseUrl + "/oauth/" + provider.toLowerCase() + "/start?t=" +
                java.net.URLEncoder.encode(invite.sign(id, java.time.Instant.now().plusSeconds(3600)), java.nio.charset.StandardCharsets.UTF_8);
        return url;
    }

    @PostMapping("/patients/{id}/integrations/{provider}/disconnect")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String disconnect(@PathVariable Long id, @PathVariable String provider) {
        clients.get(Provider.valueOf(provider.toUpperCase())).disconnect(id);
        patients.findById(id).ifPresent(p -> { p.setStatus("inactive"); patients.save(p); });
        return "redirect:/patients/" + id + "?integration=" + provider + "&status=disconnected";
    }
}