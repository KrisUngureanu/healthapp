package com.sportfd.healthapp.integration.garmin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sportfd.healthapp.integration.ProviderClient;
import com.sportfd.healthapp.integration.polar.PolarClient;
import com.sportfd.healthapp.model.*;
import com.sportfd.healthapp.model.enums.GarminSummaryType;
import com.sportfd.healthapp.model.enums.Provider;
import com.sportfd.healthapp.repo.*;
import jakarta.persistence.Column;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import static com.sportfd.healthapp.util.TimeUtil.parseFlexibleAdditional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Component
public class GarminClient implements ProviderClient {
    private final RestClient garminRest;
    private final RestClient authRest;
    private final ConnectionRepository connections;
    private final PatientRepository patientRepository;
    private final GarminWebhookRepository webhookRepo;
    private final GarminHRVRepository hrvRepository;
    private final GarminHrvValuesRepository hrvValuesRepository;
    @Qualifier("garminHealthRestClient")
    private final RestClient healthRest;
    private final PkceStore pkceStore;
    private final GarminSpoRepository spoRepository;
    private final GarminSpoValuesRepository spoValuesRepository;
    private final GarminTemperatureRepository temperatureRepository;
    private final GarminActivityRepository activityRepository;
    private final GarminHealthSnapshotRepository healthSnapshotRepository;
    private final GarminHSSummariesRepository hsSummariesRepository;
    private final GarminDailySummaryRepository dailySummaryRepository;
    @Value("${app.garmin.client-id}")
    private String clientId;
    @Value("${app.garmin.client-secret}")
    private String clientSecret;
    @Value("${app.garmin.redirect-uri}")
    private String redirectUri;

    @Value("${app.garmin.authorize-url:https://connect.garmin.com/oauth2Confirm}")
    private String authorizeUrl;

    private final GarminSleepRepository sleepRepo;


    @Override
    public Provider provider() {
        return Provider.GARMIN;
    }

    public GarminClient(@Qualifier("garminRestClient") RestClient garminRest, @Qualifier("garminAuthClient") RestClient authRest,
                        ConnectionRepository connections,
                        PatientRepository patientRepository,
                        GarminWebhookRepository webhookRepo, GarminHRVRepository hrvRepository, GarminHrvValuesRepository hrvValuesRepository, @Qualifier("garminHealthRestClient") RestClient healthRest, PkceStore pkceStore, GarminSpoRepository spoRepository, GarminSpoValuesRepository spoValuesRepository, GarminTemperatureRepository temperatureRepository, GarminActivityRepository activityRepository, GarminHealthSnapshotRepository healthSnapshotRepository, GarminHSSummariesRepository hsSummariesRepository, GarminDailySummaryRepository dailySummaryRepository, GarminSleepRepository sleepRepo) {
        this.garminRest = garminRest;
        this.authRest = authRest;
        this.connections = connections;
        this.patientRepository = patientRepository;
        this.webhookRepo = webhookRepo;
        this.hrvRepository = hrvRepository;
        this.hrvValuesRepository = hrvValuesRepository;
        this.healthRest = healthRest;
        this.pkceStore = pkceStore;
        this.spoRepository = spoRepository;
        this.spoValuesRepository = spoValuesRepository;
        this.temperatureRepository = temperatureRepository;
        this.activityRepository = activityRepository;
        this.healthSnapshotRepository = healthSnapshotRepository;
        this.hsSummariesRepository = hsSummariesRepository;
        this.dailySummaryRepository = dailySummaryRepository;


        this.sleepRepo = sleepRepo;

    }

    @Value("${app.garmin.scopes")
    private String configuredScopes;
    @Value("${app.garmin.authorize-path:/oauth2/authorize}")
    private String authorizePath;
    @Value("${app.garmin.token-path:/oauth2/token}")
    private String tokenPath;

    public String defaultScopes() {
        return configuredScopes;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @Override
    public String buildAuthorizeUrl(String state, String scopes, String redirectOverride) {
        String ru = (redirectOverride != null && !redirectOverride.isBlank())
                ? redirectOverride
                : redirectUri;

        // 1) Сгенерировать PKCE (verifier + challenge)
        String codeVerifier = Pkce.generateVerifier();          // 43–128 символов
        String codeChallenge = Pkce.challengeS256(codeVerifier);

        // 2) Сохранить verifier вместе с state (для последующего обмена в /callback)
        pkceStore.put(state, codeVerifier);

        // 3) Собрать абсолютный URL авторизации
        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(authorizeUrl)
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", ru)
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256");

        // Garmin обычно игнорирует scope, но добавим если не пустой
        if (scopes != null && !scopes.isBlank()) {
            b.queryParam("scope", scopes);
        }

        return b.build(true).toUriString();
    }

    @Override
    @Transactional
    public void exchangeCodeAndSave(Long patientId, String code) {
        String form = "grant_type=authorization_code"
                + "&code=" + enc(code)
                + "&redirect_uri=" + enc(redirectUri)
                + "&client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret);

        TokenResp token = authRest.post()
                .uri(tokenPath)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResp.class);

        upsertTokens(patientId, token);
        fillExternalUserId(patientId);
    }

    @Transactional
    @Override
    public void exchangeCodeAndSave(Long patientId, String code, String state) {
        String codeVerifier = pkceStore.take(state); // PKCE
        if (codeVerifier == null) throw new IllegalStateException("Missing code_verifier (PKCE)");

        String form = "grant_type=authorization_code"
                + "&code=" + enc(code)
                + "&redirect_uri=" + enc(redirectUri)
                + "&client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret)
                + "&code_verifier=" + enc(codeVerifier);

        TokenResp token = authRest.post()
                .uri(tokenPath) // /di-oauth2-service/oauth/token — база diauth.garmin.com
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResp.class);

        if (token == null || token.accessToken == null)
            throw new IllegalStateException("Garmin token exchange failed");
        Optional<Patient> patient = patientRepository.findById(patientId);
        patient.get().setStatus("active");
        patient.get().setDevice("garmin");
        upsertTokens(patientId, token);
    }

    @Transactional
    protected void upsertTokens(Long pid, TokenResp t) {
        Connection c = connections.findByPatientIdAndProvider(pid, Provider.GARMIN)
                .orElseGet(Connection::new);
        if (c.getId() == null) {
            c.setPatientId(pid);
            c.setProvider(Provider.GARMIN);
        }
        c.setAccessToken(t.accessToken);
        if (t.refreshToken != null) c.setRefreshToken(t.refreshToken);
        if (t.scope != null) c.setScope(t.scope);
        if (t.expiresIn != null) {
            c.setExpiresAt(OffsetDateTime.now().plusSeconds(t.expiresIn));
        }
        connections.save(c);
    }

    @Override
    @Transactional
    public void disconnect(Long patientId) {
        connections.deleteByPatientIdAndProvider(patientId, Provider.GARMIN);
    }

    @Override
    public void syncSleep(Long pid, OffsetDateTime from, OffsetDateTime to) {
        var conn = connections.findByPatientIdAndProvider(pid, Provider.GARMIN)
                .orElseThrow(() -> new IllegalStateException("Garmin not connected for patient " + pid));

        long startSec = from.toEpochSecond();
        long endSec = to.toEpochSecond();

        JsonNode root = garminRest.get()
                .uri(uri -> UriComponentsBuilder.fromPath("/wellness-api/rest/sleeps")
                        .queryParam("uploadStartTimeInSeconds", startSec)
                        .queryParam("uploadEndTimeInSeconds", endSec)
                        .queryParam("token", pullToken)
                        .build(true).toUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + conn.getAccessToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);

        if (root == null) return;
        var items = root.isArray() ? root : root.path("sleeps");
        if (!items.isArray()) return;

        for (JsonNode rec : items) {

            String sleepId = text(rec, "summaryId", "sleepId", "id");

            long startTimeInSeconds = rec.path("startTimeInSeconds").asLong(0);
            int offsetSeconds = rec.path("startTimeOffsetInSeconds").asInt(0);
            OffsetDateTime start = null;
            if (startTimeInSeconds > 0) {
                OffsetDateTime startUtc = OffsetDateTime.ofInstant(Instant.ofEpochSecond(startTimeInSeconds), ZoneOffset.UTC);
                start = startUtc.withOffsetSameInstant(ZoneOffset.ofTotalSeconds(offsetSeconds));
            }


            int durationSec = rec.path("durationInSeconds").asInt(0);
            OffsetDateTime end = (start != null && durationSec > 0) ? start.plusSeconds(durationSec) : null;


            int score = rec.has("overallSleepScore") && rec.get("overallSleepScore").has("value")
                    ? rec.get("overallSleepScore").get("value").asInt(0)
                    : rec.path("overallSleepScore").asInt(0);


            if (sleepId == null) {
                String basis = (start != null ? start.toString() : "") + "|" + durationSec + "|" + rec.path("calendarDate").asText("");
                sleepId = UUID.nameUUIDFromBytes(basis.getBytes(StandardCharsets.UTF_8)).toString();
            }
            int deepSleepDuration = rec.path("deepSleepDurationInSeconds").asInt(0);

            int lightSleepDuration = rec.path("lightSleepDurationInSeconds").asInt(0);

            int remSleep = rec.path("remSleepInSeconds").asInt(0);

            int awakeDuration = rec.path("awakeDurationInSeconds").asInt(0);

            JsonNode ss = rec.path("sleepScores");
            Map<String, String> subscores = new HashMap<>();
            ss.fieldNames().forEachRemaining(k -> {
                String q = ss.path(k).path("qualifierKey").asText(null);
                if (q != null) subscores.put(k, q);
            });


            String totalDurationQ = subscores.get("totalDuration");
            String stressQ = subscores.get("stress");
            String awakeCount = subscores.get("awakeCount");
            String remPercentage = subscores.get("remPercentage");
            String lightPercentage = subscores.get("lightPercentage");
            String deepPercentage = subscores.get("deepPercentage");
            String restlessness = subscores.get("restlessness");


            var entity = sleepRepo.findBySleepId(sleepId).orElseGet(GarminSleep::new);
            entity.setSleepId(sleepId);
            entity.setPatientId(pid);
            entity.setStartTime(start);
            entity.setEndTime(end);
            entity.setDurationSec(durationSec);
            entity.setDeepSleepDuration(deepSleepDuration);
            entity.setLightSleepDuration(lightSleepDuration);
            entity.setRemSleep(remSleep);
            entity.setAwakeDuration(awakeDuration);
            entity.setScore(score);
            entity.setPayloadJson(rec.toString());
            entity.setTotalDurationQ(totalDurationQ);
            entity.setStressQ(stressQ);
            entity.setAwakeCount(awakeCount);
            entity.setRemPercentage(remPercentage);
            entity.setLightPercentage(lightPercentage);
            entity.setDeepPercentage(deepPercentage);
            entity.setRestlessness(restlessness);
            sleepRepo.save(entity);
        }
    }

    private static String text(JsonNode n, String... keys) {
        for (String k : keys) if (n.hasNonNull(k)) return n.get(k).asText();
        return null;
    }

    @Override
    public void syncAll(Long pid, OffsetDateTime from, OffsetDateTime to) {
        syncSleep(pid,from,to);
        syncSpO2(pid,from,to);
        syncTemperature(pid,from,to);
        syncHeartRate(pid,from,to);
        syncActivityDaily(pid,from,to);
        syncDailySummary(pid,from,to);
        syncHealthSnapshot(pid,from,to);

    }

    @Override
    public void syncHeartRate(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        var conn = connections.findByPatientIdAndProvider(patientId, Provider.GARMIN)
                .orElseThrow(() -> new IllegalStateException("Garmin not connected for patient " + patientId));

        long startSec = from.toEpochSecond();
        long endSec = to.toEpochSecond();

        JsonNode root = garminRest.get()
                .uri(uri -> UriComponentsBuilder.fromPath("/wellness-api/rest/hrv")
                        .queryParam("uploadStartTimeInSeconds", startSec)
                        .queryParam("uploadEndTimeInSeconds", endSec)
                        .queryParam("token", pullToken)
                        .build(true).toUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + conn.getAccessToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);
        if (root == null) return;
        ArrayNode items = (ArrayNode) root;
        if (!items.isArray()) return;
        for (JsonNode it : items) {
            String summaryId = it.path("summaryId").asText(null);
            String calendarDate = it.path("calendarDate").asText();
            Integer lastNightAvg = it.path("lastNightAvg").asInt();
            Integer lastNight5MinHigh = it.path("lastNight5MinHigh").asInt();
            Integer startTimeOffsetInSeconds = it.path("startTimeOffsetInSeconds").asInt();
            Integer durationInSeconds = it.path("durationInSeconds").asInt();
            Integer startTimeInSeconds = it.path("startTimeInSeconds").asInt();
            //проверим на существование
            GarminHRV existing = hrvRepository.findBySummaryId(summaryId);
            if (existing == null) {
                GarminHRV garminHRV = new GarminHRV();
                garminHRV.setSummaryId(summaryId);
                garminHRV.setCalendarDate(calendarDate);
                garminHRV.setLastNightAvg(lastNightAvg);
                garminHRV.setLastNight5MinHigh(lastNight5MinHigh);
                garminHRV.setStartTimeOffsetInSeconds(startTimeOffsetInSeconds);
                garminHRV.setStartTimeInSeconds(startTimeInSeconds);
                garminHRV.setDurationInSeconds(durationInSeconds);
                garminHRV.setPatientId(patientId);
                hrvRepository.save(garminHRV);

                JsonNode hrvValues = it.path("hrvValues");
                if (hrvValues.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> fields = hrvValues.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fields.next();
                        String timeInSec = entry.getKey();
                        int value = entry.getValue().asInt();
                        GarminHrvValues hrvValues1 = new GarminHrvValues();
                        hrvValues1.setSummaryId(summaryId);
                        hrvValues1.setTimeInSecond(timeInSec);
                        hrvValues1.setValue((long) value);
                        hrvValuesRepository.save(hrvValues1);

                    }
                }
            }

        }

    }

    @Override
    public void syncSpO2(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        var conn = connections.findByPatientIdAndProvider(patientId, Provider.GARMIN)
                .orElseThrow(() -> new IllegalStateException("Garmin not connected for patient " + patientId));

        long startSec = from.toEpochSecond();
        long endSec = to.toEpochSecond();

        JsonNode root = garminRest.get()
                .uri(uri -> UriComponentsBuilder.fromPath("/wellness-api/rest/pulseOx")
                        .queryParam("uploadStartTimeInSeconds", startSec)
                        .queryParam("uploadEndTimeInSeconds", endSec)
                        .queryParam("token", pullToken)
                        .build(true).toUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + conn.getAccessToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);
        if (root == null) return;
        ArrayNode items = (ArrayNode) root;
        if (!items.isArray()) return;
        for (JsonNode it : items) {
            String summaryId = it.path("summaryId").asText(null);
            String calendarDate = it.path("calendarDate").asText();

            //проверим на существование
            GarminSpo exist = spoRepository.findBySummaryId(summaryId);
            if (exist == null) {
                GarminSpo garminSpo = new GarminSpo();
                garminSpo.setSummaryId(summaryId);
                garminSpo.setPatientId(patientId);
                garminSpo.setCalendarDate(parseFlexibleAdditional(calendarDate, null));
                spoRepository.save(garminSpo);
                JsonNode timeOffsetSpo2Values = it.path("timeOffsetSpo2Values");
                if (timeOffsetSpo2Values.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> fields = timeOffsetSpo2Values.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fields.next();
                        String timeInSec = entry.getKey();
                        int value = entry.getValue().asInt();
                        GarminSpoValues garminSpoValues = new GarminSpoValues();
                        garminSpoValues.setSummaryId(summaryId);
                        garminSpoValues.setTimeinSec(timeInSec);
                        garminSpoValues.setValue(value);
                        spoValuesRepository.save(garminSpoValues);

                    }
                }
            }


        }
    }

    @Override
    public void syncActivityDaily(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        var conn = connections.findByPatientIdAndProvider(patientId, Provider.GARMIN)
                .orElseThrow(() -> new IllegalStateException("Garmin not connected for patient " + patientId));

        long startSec = from.toEpochSecond();
        long endSec = to.toEpochSecond();

        JsonNode root = garminRest.get()
                .uri(uri -> UriComponentsBuilder.fromPath("/wellness-api/rest/activities")
                        .queryParam("uploadStartTimeInSeconds", startSec)
                        .queryParam("uploadEndTimeInSeconds", endSec)
                        .queryParam("token", pullToken)
                        .build(true).toUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + conn.getAccessToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);
        if (root == null) return;
        ArrayNode items = (ArrayNode) root;
        if (!items.isArray()) return;
        for (JsonNode it : items) {
            String activityId = it.path("activityId").asText();
            String sport = it.path("activityName").asText();
            Long ts = (long) it.path("startTimeInSeconds").asInt();
            Instant instant = Instant.ofEpochSecond(ts);
            OffsetDateTime startTime = instant.atOffset(ZoneOffset.UTC);
            int maxHr = it.path("maxHeartRateInBeatsPerMinute").asInt();
            int calories = it.path("activeKilocalories").asInt();
            float distanceMeters = it.path("distanceInMeters").asInt();
            String summaryId = it.path("summaryId").asText();

            String activityName = it.path("activityName").asText();
            String activityDescription = it.path("activityDescription").asText();
            String activityType = it.path("activityType").asText();
            float averageHeartRateInBeatsPerMinute = (float) it.path("averageHeartRateInBeatsPerMinute").asDouble(0d);
            float averageRunCadenceInStepsPerMinute = (float) it.path("averageRunCadenceInStepsPerMinute").asDouble(0d);
            float averageSpeedInMetersPerSecond = (float) it.path("averageRunCadenceInStepsPerMinute").asDouble(0d);
            float averagePaceInMinutesPerKilometer = (float) it.path("averagePaceInMinutesPerKilometer").asDouble(0d);
            Integer steps = it.path("steps").asInt(it.path("pushes").asInt(0));

            //провеим на существование
            GarminActivity exist = activityRepository.findBySummaryId(summaryId);
            if (exist == null) {
                GarminActivity garminActivity = new GarminActivity();
                garminActivity.setPatientId(patientId);
                garminActivity.setSummaryId(summaryId);
                garminActivity.setActivityId(activityId);
                garminActivity.setSport(sport);
                garminActivity.setStartTime(startTime);
                garminActivity.setMaxHr(maxHr);
                garminActivity.setCalories(calories);
                garminActivity.setDistanceMeters(distanceMeters);
                garminActivity.setActivityName(activityName);
                garminActivity.setActivityDescription(activityDescription);
                garminActivity.setActivityType(activityType);
                garminActivity.setAverageHeartRateInBeatsPerMinute(averageHeartRateInBeatsPerMinute);
                garminActivity.setAverageSpeedInMetersPerSecond(averageSpeedInMetersPerSecond);
                garminActivity.setAverageRunCadenceInStepsPerMinute(averageRunCadenceInStepsPerMinute);
                garminActivity.setAveragePaceInMinutesPerKilometer(averagePaceInMinutesPerKilometer);
                garminActivity.setSteps(steps);
                activityRepository.save(garminActivity);
            }
        }
    }

    @Override
    public void syncTemperature(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        var conn = connections.findByPatientIdAndProvider(patientId, Provider.GARMIN)
                .orElseThrow(() -> new IllegalStateException("Garmin not connected for patient " + patientId));

        long startSec = from.toEpochSecond();
        long endSec = to.toEpochSecond();

        JsonNode root = garminRest.get()
                .uri(uri -> UriComponentsBuilder.fromPath("/wellness-api/rest/skinTemp")
                        .queryParam("uploadStartTimeInSeconds", startSec)
                        .queryParam("uploadEndTimeInSeconds", endSec)
                        .queryParam("token", pullToken)
                        .build(true).toUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + conn.getAccessToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);
        if (root == null) return;
        ArrayNode items = (ArrayNode) root;
        if (!items.isArray()) return;
        for (JsonNode it : items) {
            String summaryId = it.path("summaryId").asText(null);
            String calendarDate = it.path("calendarDate").asText();
            float avgDeviationCelsius = (float) it.path("avgDeviationCelsius").asDouble(0d);
            int durationInSeconds = it.path("durationInSeconds").asInt();
            int startTimeInSeconds = it.path("startTimeInSeconds").asInt();
            int startTimeOffsetInSeconds = it.path("startTimeOffsetInSeconds").asInt();
            GarminTemperature exist = temperatureRepository.findBySummaryId(summaryId);
            if (exist == null) {
                GarminTemperature garminTemperature = new GarminTemperature();
                garminTemperature.setSummaryId(summaryId);
                garminTemperature.setPatientId(patientId);
                garminTemperature.setCalendarDate(parseFlexibleAdditional(calendarDate, null));
                garminTemperature.setAvgDeviationCelsius(avgDeviationCelsius);
                garminTemperature.setDurationInSeconds(durationInSeconds);
                garminTemperature.setStartTimeInSeconds(startTimeInSeconds);
                garminTemperature.setStartTimeOffsetInSeconds(startTimeOffsetInSeconds);
                temperatureRepository.save(garminTemperature);
            }

        }
    }

    @Override
    public void syncDailySummary(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        var conn = connections.findByPatientIdAndProvider(patientId, Provider.GARMIN)
                .orElseThrow(() -> new IllegalStateException("Garmin not connected for patient " + patientId));

        long startSec = from.toEpochSecond();
        long endSec = to.toEpochSecond();

        JsonNode root = garminRest.get()
                .uri(uri -> UriComponentsBuilder.fromPath("/wellness-api/rest/dailies")
                        .queryParam("uploadStartTimeInSeconds", startSec)
                        .queryParam("uploadEndTimeInSeconds", endSec)
                        .queryParam("token", pullToken)
                        .build(true).toUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + conn.getAccessToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);
        if (root == null) return;
        ArrayNode items = (ArrayNode) root;
        if (!items.isArray()) return;
        for (JsonNode it : items) {
            String summaryId = it.path("summaryId").asText(null);
            String calendarDate = it.path("calendarDate").asText();
            Integer steps = it.path("steps").asInt();
            Integer activeKilocalories = it.path("activeKilocalories").asInt();
            Integer bmrKilocalories = it.path("bmrKilocalories").asInt();

            Integer maxStressLevel = it.path("maxStressLevel").asInt();
            Integer bodyBatteryChargedValue = it.path("bodyBatteryChargedValue").asInt();
            Integer bodyBatteryDrainedValue = it.path("bodyBatteryDrainedValue").asInt();
            OffsetDateTime updatedAt = OffsetDateTime.now();
            String activityType = it.path("activityType").asText();
            Integer pushes = it.path("pushes").asInt();
            Float distanceInMeters = (float) it.path("distanceInMeters").asDouble(0d);
            Float pushDistanceInMeters = (float) it.path("pushDistanceInMeters").asDouble(0d);
            Integer floorsClimbed = it.path("floorsClimbed").asInt();
            Integer minHeartRateInBeatsPerMinute = it.path("minHeartRateInBeatsPerMinute").asInt();
            Integer maxHeartRateInBeatsPerMinute = it.path("maxHeartRateInBeatsPerMinute").asInt();
            Integer averageHeartRateInBeatsPerMinute = it.path("averageHeartRateInBeatsPerMinute").asInt();
            Integer restingHeartRateInBeatsPerMinute = it.path("restingHeartRateInBeatsPerMinute").asInt();
            Integer averageStressLevel = it.path("averageStressLevel").asInt();


            GarminDailySummary dailySummary = dailySummaryRepository.findBySummaryId(summaryId);
            if (dailySummary == null) {
                dailySummary = new GarminDailySummary();
                dailySummary.setSummaryId(summaryId);
                dailySummary.setPatientId(patientId);

                dailySummary.setDay(calendarDate);
                dailySummary.setSteps(steps);
                dailySummary.setActiveKilocalories(activeKilocalories);
                dailySummary.setBmrKilocalories(bmrKilocalories);
                dailySummary.setMaxStressLevel(maxStressLevel);
                dailySummary.setBodyBatteryChargedValue(bodyBatteryChargedValue);
                dailySummary.setBodyBatteryDrainedValue(bodyBatteryDrainedValue);
                dailySummary.setUpdatedAt(updatedAt);
                dailySummary.setActivityType(activityType);
                dailySummary.setPushes(pushes);
                dailySummary.setDistanceInMeters(distanceInMeters);
                dailySummary.setPushDistanceInMeters(pushDistanceInMeters);
                dailySummary.setFloorsClimbed(floorsClimbed);
                dailySummary.setMinHeartRateInBeatsPerMinute(minHeartRateInBeatsPerMinute);
                dailySummary.setMaxHeartRateInBeatsPerMinute(maxHeartRateInBeatsPerMinute);
                dailySummary.setAverageHeartRateInBeatsPerMinute(averageHeartRateInBeatsPerMinute);
                dailySummary.setRestingHeartRateInBeatsPerMinute(restingHeartRateInBeatsPerMinute);
                dailySummary.setAverageStressLevel(averageStressLevel);
                dailySummary.setPayloadJson(it.toString());
                dailySummaryRepository.save(dailySummary);
            }
        }
    }

    @Override
    public void syncHealthSnapshot(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        var conn = connections.findByPatientIdAndProvider(patientId, Provider.GARMIN)
                .orElseThrow(() -> new IllegalStateException("Garmin not connected for patient " + patientId));

        long startSec = from.toEpochSecond();
        long endSec = to.toEpochSecond();

        JsonNode root = garminRest.get()
                .uri(uri -> UriComponentsBuilder.fromPath("/wellness-api/rest/healthSnapshot")
                        .queryParam("uploadStartTimeInSeconds", startSec)
                        .queryParam("uploadEndTimeInSeconds", endSec)
                        .queryParam("token", pullToken)
                        .build(true).toUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + conn.getAccessToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);
        if (root == null) return;
        ArrayNode items = (ArrayNode) root;
        if (!items.isArray()) return;
        for (JsonNode it : items) {
            String summaryId = it.path("summaryId").asText(null);
            String calendarDate = it.path("calendarDate").asText();
            int startTimeInSeconds = it.path("startTimeInSeconds").asInt();
            int durationInSeconds = it.path("durationInSeconds").asInt();
            int startTimeOffsetInSeconds = it.path("startTimeOffsetInSeconds").asInt();
            //найдем
            GarminHealthSnapshot healthSnapshot = healthSnapshotRepository.findBySummaryId(summaryId);
            if (healthSnapshot == null) {
                healthSnapshot = new GarminHealthSnapshot();
                healthSnapshot.setCalendarDate(calendarDate);
                healthSnapshot.setSummaryId(summaryId);
                healthSnapshot.setStartTimeInSeconds(startTimeInSeconds);
                healthSnapshot.setDurationInSeconds(durationInSeconds);
                healthSnapshot.setStartTimeOffsetInSeconds(startTimeOffsetInSeconds);
                healthSnapshot.setPatientId(patientId);
                healthSnapshotRepository.save(healthSnapshot);
                JsonNode summaries = it.path("summaries");
                if (summaries.isArray()) {
                    for (JsonNode s : summaries) {
                        String summaryType = s.path("summaryType").asText();
                        float minValue = (float) s.path("minValue").asDouble();
                        float maxValue = (float) s.path("maxValue").asDouble();
                        float avgValue = (float) s.path("avgValue").asDouble();

                        GarminSummaryType garminSummaryType = GarminSummaryType.valueOf(summaryType);
                        GarminHSSummaries hsSummaries = new GarminHSSummaries();
                        hsSummaries.setSummaryType(garminSummaryType);
                        hsSummaries.setSummaryId(summaryId);
                        hsSummaries.setAvgValue(avgValue);
                        hsSummaries.setMinValue(minValue);
                        hsSummaries.setMaxValue(maxValue);
                        hsSummariesRepository.save(hsSummaries);

                    }
                }
            }


        }
    }

    private Connection requireConn(Long pid) {
        return connections.findByPatientIdAndProvider(pid, Provider.GARMIN)
                .orElseThrow(() -> new IllegalStateException("Garmin not connected for patient " + pid));
    }


    public static class TokenResp {
        @JsonProperty("access_token")
        public String accessToken;
        @JsonProperty("refresh_token")
        public String refreshToken;
        @JsonProperty("expires_in")
        public Long expiresIn;
        @JsonProperty("token_type")
        public String tokenType;
        @JsonProperty("scope")
        public String scope;
    }

    public final class Pkce {
        private static final SecureRandom RND = new SecureRandom();

        public static String generateVerifier() {
            byte[] bytes = new byte[64]; // 64 => ~86 chars base64url
            RND.nextBytes(bytes);
            return base64Url(bytes);
        }

        public static String challengeS256(String verifier) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                return base64Url(md.digest(verifier.getBytes(StandardCharsets.US_ASCII)));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }

        private static String base64Url(byte[] b) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        }
    }

    @Override
    public void fillExternalUserId(Long patientId) {
        Connection c = requireConn(patientId);
        String token = c.getAccessToken();

        JsonNode resp = garminRest.get()
                .uri("/wellness-api/rest/user/id")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(JsonNode.class);

        String userId = null;
        if (resp != null) {
            userId = resp.path("userId").asText(null);
            if (userId == null) userId = resp.path("userAccessRefId").asText(null);
            if (userId == null) userId = resp.path("id").asText(null);
        }
        if (userId == null) {
            throw new IllegalStateException("Cannot read userId from Garmin response: " + (resp == null ? "null" : resp.toString()));
        }

        if (c.getExternalUserId() == null || !c.getExternalUserId().equals(userId)) {
            c.setExternalUserId(userId);
            connections.save(c);
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long pid, boolean alsoDisconnectLocally) {
        String token = requireToken(pid);


        ResponseEntity<Void> resp = null;
        try {
            resp = RestClient.create()
                    .delete()
                    .uri("https://apis.garmin.com/wellness-api/rest/user/registration/")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            System.out.println("KRIS_ERR " + e);
        }


        if (resp != null && resp.getStatusCode().is2xxSuccessful()) {

            if (alsoDisconnectLocally) {
                connections.findByPatientIdAndProvider(pid, Provider.GARMIN).ifPresent(c -> {
                    c.setExternalUserId(null);
                    c.setAccessToken(null);
                    c.setRefreshToken(null);
                    c.setExpiresAt(null);
                    connections.save(c);
                });


                connections.deleteByPatientIdAndProvider(pid, Provider.POLAR);

                patientRepository.deleteById(pid);
            }

        }


    }

    @Value("${app.garmin.pull-token}")
    private String pullToken;


    public JsonNode fetchSleeps(String bearer, long startSec, long endSec) {
        return garminRest.get()
                .uri(uri -> UriComponentsBuilder.fromPath("/wellness-api/rest/sleeps")
                        .queryParam("uploadStartTimeInSeconds", startSec)
                        .queryParam("uploadEndTimeInSeconds", endSec)
                        .queryParam("token", pullToken) // <-- pull token
                        .build().toUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer)
                .retrieve()
                .body(JsonNode.class);
    }

    private String requireToken(Long pid) {
        Connection c = connections.findByPatientIdAndProvider(pid, Provider.GARMIN)
                .orElseThrow(() -> new IllegalStateException("Garimin not connected for patient " + pid));
        String token = c.getAccessToken();
        if (token == null || token.isBlank())
            throw new IllegalStateException("Garimin access token is null for patient " + pid);
        return token;
    }

    private String requireExternalUserId(Long pid) {
        var c = connections.findByPatientIdAndProvider(pid, Provider.GARMIN)
                .orElseThrow(() -> new IllegalStateException("Garmin not connected for patient " + pid));
        String ext = c.getExternalUserId();
        if (ext == null || ext.isBlank())
            throw new IllegalStateException("externalUserId is empty; user not registered?");
        int slash = ext.lastIndexOf('/');
        return slash >= 0 ? ext.substring(slash + 1) : ext;
    }


}
