package com.sportfd.healthapp.integration.garmin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.sportfd.healthapp.integration.ProviderClient;
import com.sportfd.healthapp.integration.polar.PolarClient;
import com.sportfd.healthapp.model.*;
import com.sportfd.healthapp.model.enums.Provider;
import com.sportfd.healthapp.repo.*;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Component
public class GarminClient implements ProviderClient {
    private final RestClient garminRest;
    private final RestClient authRest;
    private final ConnectionRepository connections;
    private final PatientRepository patientRepository;
    private final GarminWebhookRepository webhookRepo;

    @Qualifier("garminHealthRestClient")
    private final RestClient healthRest;
    private final PkceStore pkceStore;
    @Qualifier("garminActivityRestClient")
    private final RestClient activityRest;
    @Value("${app.garmin.client-id}")     private String clientId;
    @Value("${app.garmin.client-secret}") private String clientSecret;
    @Value("${app.garmin.redirect-uri}")  private String redirectUri;

    @Value("${app.garmin.authorize-url:https://connect.garmin.com/oauth2Confirm}")
    private String authorizeUrl;
    private final GarminDailySummaryRepository dailyRepo;
    private final GarminSleepRepository sleepRepo;
    private final GarminActivityRepository activityRepo;

    @Override public Provider provider() { return Provider.GARMIN; }

    public GarminClient(@Qualifier("garminRestClient") RestClient garminRest, @Qualifier("garminAuthClient") RestClient authRest,
                        ConnectionRepository connections,
                        PatientRepository patientRepository,
                        GarminWebhookRepository webhookRepo, @Qualifier("garminHealthRestClient") RestClient healthRest, PkceStore pkceStore, @Qualifier("garminActivityRestClient") RestClient activityRest, GarminDailySummaryRepository dailyRepo, GarminSleepRepository sleepRepo, GarminActivityRepository activityRepo) {
        this.garminRest = garminRest;
        this.authRest = authRest;
        this.connections = connections;
        this.patientRepository = patientRepository;
        this.webhookRepo = webhookRepo;
        this.healthRest = healthRest;
        this.pkceStore = pkceStore;
        this.activityRest = activityRest;
        this.dailyRepo = dailyRepo;
        this.sleepRepo = sleepRepo;
        this.activityRepo = activityRepo;
    }

    @Value("${app.garmin.scopes")
    private String configuredScopes;
    @Value("${app.garmin.authorize-path:/oauth2/authorize}") private String authorizePath;
    @Value("${app.garmin.token-path:/oauth2/token}")         private String tokenPath;
    public String defaultScopes() { return configuredScopes; }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
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
        long endSec   = to.toEpochSecond();

        JsonNode root = garminRest.get()
                .uri(uri -> UriComponentsBuilder.fromPath("/wellness-api/rest/sleeps")
                        .queryParam("uploadStartTimeInSeconds", startSec)
                        .queryParam("uploadEndTimeInSeconds", endSec)
                        .queryParam("token", pullToken)      // pull token из конфига
                        .build(true).toUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + conn.getAccessToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);

        if (root == null) return;
        var items = root.isArray() ? root : root.path("sleeps");
        if (!items.isArray()) return;

        for (JsonNode rec : items) {
            // 1) идентификатор сна
            String sleepId = text(rec, "summaryId", "sleepId", "id");
            // 2) время начала: epoch + оффсет (в секундах)
            long startTimeInSeconds = rec.path("startTimeInSeconds").asLong(0);
            int  offsetSeconds      = rec.path("startTimeOffsetInSeconds").asInt(0);
            OffsetDateTime start = null;
            if (startTimeInSeconds > 0) {
                OffsetDateTime startUtc = OffsetDateTime.ofInstant(Instant.ofEpochSecond(startTimeInSeconds), ZoneOffset.UTC);
                start = startUtc.withOffsetSameInstant(ZoneOffset.ofTotalSeconds(offsetSeconds));
            }

            // 3) длительность и конец
            int durationSec = rec.path("durationInSeconds").asInt(0);
            OffsetDateTime end = (start != null && durationSec > 0) ? start.plusSeconds(durationSec) : null;

            // 4) оценка сна
            int score = rec.has("overallSleepScore") && rec.get("overallSleepScore").has("value")
                    ? rec.get("overallSleepScore").get("value").asInt(0)
                    : rec.path("overallSleepScore").asInt(0);

            // если нет id — сгенерим детерминистический
            if (sleepId == null) {
                String basis = (start != null ? start.toString() : "") + "|" + durationSec + "|" + rec.path("calendarDate").asText("");
                sleepId = UUID.nameUUIDFromBytes(basis.getBytes(StandardCharsets.UTF_8)).toString();
            }

            // upsert
            var entity = sleepRepo.findBySleepId(sleepId).orElseGet(GarminSleep::new);
            entity.setSleepId(sleepId);
            entity.setPatientId(pid);
            entity.setStartTime(start);
            entity.setEndTime(end);
            entity.setDurationSec(durationSec);
            entity.setScore(score);
            entity.setPayloadJson(rec.toString());

            sleepRepo.save(entity);
        }
    }
    private static String text(JsonNode n, String... keys) {
        for (String k : keys) if (n.hasNonNull(k)) return n.get(k).asText();
        return null;
    }
    @Override
    public void syncAll(Long pid, OffsetDateTime from, OffsetDateTime to) {

    }

    @Override
    public void syncHeartRate(Long patientId, OffsetDateTime from, OffsetDateTime to) {

    }

    @Override
    public void syncSpO2(Long patientId, OffsetDateTime from, OffsetDateTime to) {

    }

    private Connection requireConn(Long pid) {
        return connections.findByPatientIdAndProvider(pid, Provider.GARMIN)
                .orElseThrow(() -> new IllegalStateException("Garmin not connected for patient " + pid));
    }

    public JsonNode getJson(RestClient rc, String url, String accessToken) {
        return rc.get().uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);
    }

    /** Пример: стянуть Daily Summary по дате / summaryId */
    public JsonNode fetchDailySummary(String userId, String dayOrId, String accessToken) {
        // Вариант 1: по дате
        String url = UriComponentsBuilder.fromPath("/user/" + userId + "/dailySummary/" + dayOrId)
                .build().toUriString();
        return getJson(healthRest, url, accessToken);
    }

    /** Пример: сон */
    public JsonNode fetchSleep(String userId, String sleepIdOrDate, String accessToken) {
        String url = UriComponentsBuilder.fromPath("/user/" + userId + "/sleep/" + sleepIdOrDate)
                .build().toUriString();
        return getJson(healthRest, url, accessToken);
    }

    /** Пример: тренировка/активность */
    public JsonNode fetchActivity(String userId, String activityId, String accessToken) {
        String url = UriComponentsBuilder.fromPath("/user/" + userId + "/activities/" + activityId)
                .build().toUriString();
        return getJson(activityRest, url, accessToken);
    }

    // ==== Разбор «деталей» и сохранение (упрощённые примеры маппинга) ====

    @Transactional
    public void pullDetailsByType(GarminWebhookEvent evt) {
        // Если у тебя есть связь userId -> patientId (external_user_id в Connection), найдём patient:
        Connection c =  connections.findByExternalUserIdAndProvider(evt.getUserId(), Provider.GARMIN)
                .orElse(null);
        if (c == null) return;

        String token = c.getAccessToken();
        Long pid = c.getPatientId();
        String type = evt.getEventType();

        if ("DAILY_SUMMARY".equalsIgnoreCase(type) && evt.getSummaryId() != null) {
            JsonNode js = fetchDailySummary(evt.getUserId(), evt.getSummaryId(), token);
            upsertDailySummary(pid, js);
        } else if ("SLEEP".equalsIgnoreCase(type) && evt.getSummaryId() != null) {
            JsonNode js = fetchSleep(evt.getUserId(), evt.getSummaryId(), token);
            upsertSleep(pid, js);
        } else if ("ACTIVITY".equalsIgnoreCase(type) && evt.getSummaryId() != null) {
            JsonNode js = fetchActivity(evt.getUserId(), evt.getSummaryId(), token);
            upsertActivity(pid, js);
        }
    }

    private void upsertDailySummary(Long pid, JsonNode js) {
        // Пример полей — замапь под реальный JSON
        String id = js.path("summaryId").asText(null);
        if (id == null) return;

        GarminDailySummary e = dailyRepo.findBySummaryId(id).orElseGet(GarminDailySummary::new);
        e.setSummaryId(id);
        e.setPatientId(pid);
        e.setDay(js.path("day").asText(null));
        e.setSteps(js.path("steps").asInt(0));
        e.setCalories(js.path("calories").asInt(0));
        e.setStress(js.path("stress").path("score").asInt(0));
        e.setBodyBattery(js.path("bodyBattery").path("score").asInt(0));
        e.setPayloadJson(js.toString());
        e.setUpdatedAt(OffsetDateTime.now());
        dailyRepo.save(e);
    }

    private void upsertSleep(Long pid, JsonNode js) {
        String id = js.path("sleepId").asText(null);
        if (id == null) return;

        GarminSleep s = sleepRepo.findBySleepId(id).orElseGet(GarminSleep::new);
        s.setSleepId(id);
        s.setPatientId(pid);
        s.setStartTime(tryParse(js.path("startTime").asText(null)));
        s.setEndTime(tryParse(js.path("endTime").asText(null)));
        s.setScore(js.path("score").asInt(0));
        s.setDurationSec(js.path("durationSec").asInt(0));
        s.setPayloadJson(js.toString());
        sleepRepo.save(s);
    }

    private void upsertActivity(Long pid, JsonNode js) {
        String id = js.path("activityId").asText(null);
        if (id == null) return;

        GarminActivity a = activityRepo.findByActivityId(id).orElseGet(GarminActivity::new);
        a.setActivityId(id);
        a.setPatientId(pid);
        a.setSport(js.path("sport").asText(null));
        a.setStartTime(tryParse(js.path("startTime").asText(null)));
        a.setEndTime(tryParse(js.path("endTime").asText(null)));
        a.setAvgHr(js.path("avgHr").asInt(0));
        a.setMaxHr(js.path("maxHr").asInt(0));
        a.setCalories(js.path("calories").asInt(0));
        a.setDistanceMeters((float) js.path("distanceMeters").asDouble(0));
        a.setPayloadJson(js.toString());
        activityRepo.save(a);
    }

    private OffsetDateTime tryParse(String s) {
        try { return (s == null ? null : OffsetDateTime.parse(s)); }
        catch (Exception e) { return null; }
    }
    public static class TokenResp {
        @JsonProperty("access_token")  public String accessToken;
        @JsonProperty("refresh_token") public String refreshToken;
        @JsonProperty("expires_in")    public Long expiresIn;
        @JsonProperty("token_type")    public String tokenType;
        @JsonProperty("scope")         public String scope;
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
            } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
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

    @Value("${app.garmin.pull-token}") private String pullToken;





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
        if (ext == null || ext.isBlank()) throw new IllegalStateException("externalUserId is empty; user not registered?");
        int slash = ext.lastIndexOf('/');
        return slash >= 0 ? ext.substring(slash + 1) : ext;
    }


}
