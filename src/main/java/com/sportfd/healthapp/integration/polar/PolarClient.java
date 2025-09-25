package com.sportfd.healthapp.integration.polar;

import com.sportfd.healthapp.integration.ProviderClient;
import com.sportfd.healthapp.model.Connection;
import com.sportfd.healthapp.model.Patient;
import com.sportfd.healthapp.model.enums.Provider;
import com.sportfd.healthapp.repo.ConnectionRepository;
import com.sportfd.healthapp.repo.PatientRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Base64;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Component
public class PolarClient implements ProviderClient {

    private static final String POLAR_AUTH_BASE = "https://flow.polar.com/oauth2/authorization";
    private static final String POLAR_TOKEN_URL  = "https://polarremote.com/v2/oauth2/token";
    private static final String DEFAULT_SCOPE    = "accesslink.read_all";

    @Qualifier("polarRestClient")
    private final RestClient polar; // можно использовать для API Polar (не для авторизации)
    private final ConnectionRepository connections;
    private final PatientRepository patientRepository;

    @Value("${app.polar.client-id}")     private String clientId;
    @Value("${app.polar.client-secret}") private String clientSecret;
    @Value("${app.polar.redirect-uri}")  private String redirectUri;

    public PolarClient(@Qualifier("polarRestClient") RestClient polar,
                       ConnectionRepository connections,
                       PatientRepository patientRepository) {
        this.polar = polar;
        this.connections = connections;
        this.patientRepository = patientRepository;
    }

    @Override public Provider provider() { return Provider.POLAR; }

    /**
     * Формирование URL для шага авторизации.
     * Пробрасываем response_type=code, client_id, redirect_uri, scope, state.
     */
    @Override
    public String buildAuthorizeUrl(String state, String scopes, String redirect) {
        String ru = (redirect != null && !redirect.isBlank()) ? redirect : redirectUri;
        String scope = (scopes != null && !scopes.isBlank()) ? scopes : DEFAULT_SCOPE;

        return UriComponentsBuilder
                .fromHttpUrl(POLAR_AUTH_BASE)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", ru)
//                .queryParam("scope", scope)

                .queryParam("state", Objects.toString(state, ""))
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    /**
     * Обмен кода на access/refresh токены с соблюдением требований Polar:
     * - POST https://polarremote.com/v2/oauth2/token
     * - Authorization: Basic base64(client_id:client_secret)
     * - Content-Type: application/x-www-form-urlencoded
     * - grant_type=authorization_code&code=...&redirect_uri=...(если использовали при authorize)
     */
    @Override
    @Transactional
    public void exchangeCodeAndSave(Long patientId, String code) {
        String basic = "Basic " + Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        // redirect_uri обязательно указывать, если вы передавали его на шаге авторизации (мы передаём)
        String form = "grant_type=authorization_code"
                + "&code=" + enc(code)
                + "&redirect_uri=" + enc(redirectUri);

        TokenResp token;
        try {
            token = RestClient.create()
                    .post()
                    .uri(POLAR_TOKEN_URL)
                    .header(HttpHeaders.AUTHORIZATION, basic)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve()
                    .body(TokenResp.class);
        } catch (HttpClientErrorException e) {
            // Удобно логировать тело ответа от Polar (401/400)
            throw new RuntimeException("Polar token exchange failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        }

        if (token == null || token.access_token == null) {
            throw new RuntimeException("Polar token exchange returned empty token");
        }

        // Отметим пациента активным и с устройством POLAR
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));
        patient.setStatus("active");
        patient.setDevice("polar");

        // Сохраним/обновим токены в нашей таблице connections
        upsertTokens(patientId, token);
    }

    @Override
    @Transactional
    public void disconnect(Long patientId) {
        connections.deleteByPatientIdAndProvider(patientId, Provider.POLAR);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @Transactional
    protected void upsertTokens(Long pid, TokenResp t) {
        var c = connections.findByPatientIdAndProvider(pid, Provider.POLAR).orElseGet(Connection::new);
        if (c.getId() == null) {
            c.setPatientId(pid);
            c.setProvider(Provider.POLAR);
        }
        c.setAccessToken(t.access_token);
        if (t.refresh_token != null) c.setRefreshToken(t.refresh_token);
        c.setScope(t.scope);
        if (t.expires_in != null) {
            c.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(t.expires_in));
        }
        c.setTokenType("Bearer");
        connections.save(c);
    }

    private static class TokenResp {
        public String access_token;
        public String refresh_token;
        public Long   expires_in;
        public String scope;
        // иногда провайдеры возвращают token_type — Polar может не возвращать, мы ставим "Bearer" сами
        public String token_type;
    }

    @Override
    public void syncSleep(Long pid, OffsetDateTime from, OffsetDateTime to) {

    }

    @Override
    public void syncAll(Long pid, OffsetDateTime from, OffsetDateTime to) {

    }

    @Override
    public int syncHeartRate(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        return 0;
    }

    @Override
    public int syncSpO2(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        return 0;
    }
}
