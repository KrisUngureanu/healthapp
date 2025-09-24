package com.sportfd.healthapp.integration.polar;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportfd.healthapp.integration.ProviderClient;
import com.sportfd.healthapp.model.*;
import com.sportfd.healthapp.model.enums.Provider;
import com.sportfd.healthapp.repo.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Component

public class PolarClient implements ProviderClient {
    @Override
    public void exchangeCodeAndSave(Long patientId, String code) {

    }

    @Override
    public int syncHeartRate(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        return 0;
    }

    @Override
    public int syncSpO2(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        return 0;
    }

    @Override
    public void syncSleep(Long pid, OffsetDateTime from, OffsetDateTime to) {
    }

    @Override
    public void syncRecovery(Long pid, OffsetDateTime from, OffsetDateTime to) {
    }

    @Override
    public void syncWorkout(Long pid, OffsetDateTime from, OffsetDateTime to) {
    }

    @Override
    public void syncAll(Long pid, OffsetDateTime from, OffsetDateTime to) {

    }

    @Override
    public void syncCycles(Long pid, OffsetDateTime from, OffsetDateTime to) {
    }

    @Qualifier("polarRestClient")
    private final RestClient polar;
    private final ConnectionRepository connections;
    private final PatientRepository patientRepo;


    private final PatientRepository patientRepository;
    @Value("${app.polar.client-id}")     private String clientId;
    @Value("${app.polar.client-secret}") private String clientSecret;
    @Value("${app.polar.redirect-uri}")  private String redirectUri;

    public PolarClient(@Qualifier("polarRestClient") RestClient polar, ConnectionRepository connections, PatientRepository patientRepo, PatientRepository patientRepository) {
        this.polar = polar;
        this.connections = connections;
        this.patientRepo = patientRepo;


        this.patientRepository = patientRepository;
    }

    @Override public Provider provider() { return Provider.POLAR; }

    // ---------- OAuth ----------

    @Override
    public String buildAuthorizeUrl(String state, String scopes, String redirect) {
        String ru = (redirect != null && !redirect.isBlank()) ? redirect : redirectUri;
        var b = UriComponentsBuilder
                .fromHttpUrl("https://flow.polar.com/oauth2/authorization")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", ru)
                .queryParam("state", state);
//        if (scopes != null && !scopes.isBlank()) {
//            b.queryParam("scope", scopes.trim());
//        }
        return b.build(true).toUriString();
    }



    @Override
    @Transactional
    public void disconnect(Long patientId) {
        connections.deleteByPatientIdAndProvider(patientId, Provider.POLAR);
    }

    // ---------- Daily Activity (Beta) ----------

    @Override
    @Transactional
    public int syncActivityDaily(Long patientId, OffsetDateTime from, OffsetDateTime to) {
       return 0;
    }




}
