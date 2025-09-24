package com.sportfd.healthapp.integration.oura;

import com.sportfd.healthapp.model.*;
import com.sportfd.healthapp.model.enums.Provider;
import com.sportfd.healthapp.repo.*;
import com.sportfd.healthapp.integration.oura.dto.OuraTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OuraService {

    private static final Provider PROVIDER = Provider.OURA;

    private final RestTemplate rest;
    private final ConnectionRepository conns;
    private final PatientRepository patientRepository;


    private final OuraSleepRepository ouraSleepRepository;
    private final OuraDaylySleepRepository ouraDaylySleepRepository;
    private final OuraHeartRateRepository ouraHeartRateRepository;
    private final OuraSpoRepository ouraSpoRepository;
    private final OuraReadinessRepository ouraReadinessRepository;
    private final OuraActivityRepository ouraActivityRepository;



    @Value("${app.oura.client-id}")    private String clientId;
    @Value("${app.oura.client-secret}") private String clientSecret;
    @Value("${app.oura.redirect-uri}")  private String redirectUri;

    @Transactional
    public void exchangeCodeAndSave(Long patientId, String code) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var resp = rest.postForEntity(
                "https://api.ouraring.com/oauth/token",
                new HttpEntity<>(form, headers),
                OuraTokenResponse.class
        ).getBody();

        saveTokens(patientId, resp);
    }

    @Transactional
    public void refreshIfNeeded(Long patientId) {
        var c = conns.findByPatientIdAndProvider(patientId, PROVIDER)
                .orElseThrow(() -> new IllegalStateException("Oura not connected for patient " + patientId));

        if (c.getExpiresAt() != null && c.getExpiresAt().isAfter(OffsetDateTime.now().plusSeconds(60))) return;

        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", c.getRefreshToken());
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var resp = rest.postForEntity(
                "https://api.ouraring.com/oauth/token",
                new HttpEntity<>(form, headers),
                OuraTokenResponse.class
        ).getBody();

        saveTokens(patientId, resp);
    }

    @Transactional
    public void disconnect(Long patientId) {
        conns.deleteByPatientIdAndProvider(patientId, PROVIDER);
    }

    private void saveTokens(Long patientId, OuraTokenResponse res) {
        var c = conns.findByPatientIdAndProvider(patientId, PROVIDER).orElse(new Connection());
        c.setPatientId(patientId);
        Optional<Patient> patient = patientRepository.findById(patientId);
        patient.get().setStatus("active");
        patient.get().setDevice("oura");
        c.setProvider(PROVIDER);
        c.setAccessToken(res.getAccessToken());
        c.setRefreshToken(res.getRefreshToken());
        c.setScope(res.getScope());
        var exp = OffsetDateTime.ofInstant(Instant.now().plusSeconds(res.getExpiresIn()), ZoneOffset.UTC);
        c.setExpiresAt(exp);
        conns.save(c);
    }

    public List<OuraActivity> getOuraActivity(Long pid){
        return ouraActivityRepository.findByPatientId(pid);
    }

    public List<OuraDaylySleep> getOuraDaylySleep(Long pid){
        return ouraDaylySleepRepository.findByPatientId(pid);
    }

    public List<OuraHeartRate> getOuraHeartRate(Long pid){
        Pageable top10 = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "timeRecord"));
        return ouraHeartRateRepository.findByPatientId(pid, top10);
    }

    public List<OuraReadiness> getOuraReadiness(Long pid){
        return ouraReadinessRepository.findByPatientId(pid);
    }

    public List<OuraSleep> getOuraSleep(Long pid){
        return ouraSleepRepository.findByPatientId(pid);
    }
    public List<OuraSpo> getOuraSpo(Long pid){
        return ouraSpoRepository.findByPatientId(pid);
    }


}