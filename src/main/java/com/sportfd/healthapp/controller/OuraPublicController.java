package com.sportfd.healthapp.controller;

import com.sportfd.healthapp.integration.oura.OuraService;
import com.sportfd.healthapp.model.Patient;
import com.sportfd.healthapp.repo.PatientRepository;
import com.sportfd.healthapp.util.InviteTokenUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class OuraPublicController {

    private final InviteTokenUtil invite;
    private final OuraService oura;
    private final PatientRepository patientRepository;
    @Value("${app.oura.redirect-uri}") private String redirectUri;

    @Value("${app.oura.client-id}") private String clientId;
    // Старт по инвайту: проверяем токен, кладём pid в сессию, показываем кнопку
    @GetMapping("/oauth/oura/start")
    public String start(@RequestParam("t") String token, HttpSession session, Model model) {
        Long pid = invite.verifyAndGetPatientId(token);
        if (pid == null) return "redirect:/privacy?error=invite";
        session.setAttribute("OURA_PID", pid);
        model.addAttribute("patientId", pid);
        return "patient-oura-start"; // страница с кнопкой "Подключить Oura"
    }

    // Редирект на Oura authorize
    @GetMapping("/oauth/oura/connect")
    public String connect(HttpSession session) {
        Long pid = (Long) session.getAttribute("OURA_PID");
        if (pid == null) return "redirect:/privacy?error=no_pid";
        String state = UUID.randomUUID().toString();
        session.setAttribute("OURA_STATE", state);

        String scopes = "daily workout heartrate spo2";

        String authorizeUrl = UriComponentsBuilder
                .fromHttpUrl("https://cloud.ouraring.com/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scopes)
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
        return "redirect:" + authorizeUrl;
    }


    @GetMapping("/oauth/oura/callback")
    public String callback(@RequestParam String code, @RequestParam String state, HttpSession session) {
        String expected = (String) session.getAttribute("OURA_STATE");
        Long pid = (Long) session.getAttribute("OURA_PID");
        session.removeAttribute("OURA_STATE");
        if (expected == null || !expected.equals(state) || pid == null) {
            return "redirect:/privacy?error=state";
        }
        oura.exchangeCodeAndSave(pid, code);

        return "redirect:/thanks?provider=oura&pid=" + pid;
    }
}