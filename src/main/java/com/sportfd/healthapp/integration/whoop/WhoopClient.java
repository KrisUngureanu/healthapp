package com.sportfd.healthapp.integration.whoop;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportfd.healthapp.integration.ProviderClient;
import com.sportfd.healthapp.model.Connection;
import com.sportfd.healthapp.model.Patient;
import com.sportfd.healthapp.model.enums.Provider;
import com.sportfd.healthapp.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Optional;
import java.util.function.Consumer;

@Component

public class WhoopClient implements ProviderClient {

    @Qualifier("whoopRestClient")
    private final RestClient whoopclient;
    private final ConnectionRepository connections;
    private final SleepDailyRepository sleepDailyRepo;
    private final Spo2SampleRepository spo2SampleRepository;
    public final ActivityDailyRepository activityDailyRepo;
    private final ReadinessDailyRepository readinessDailyRepo;
    private final SleepSessionRepository sleepSessionRepo;
    private final ActivitySessionRepository activitySessionRepo;
    private final HrSampleRepository hrSampleRepo;
    private final PatientRepository patientRepository;
    @Value("${app.whoop.client-id}")     private String clientId;
    @Value("${app.whoop.client-secret}") private String clientSecret;
    @Value("${app.whoop.redirect-uri}")  private String redirectUri;

    public WhoopClient(@Qualifier("whoopRestClient") RestClient whoopclient, ConnectionRepository connections, SleepDailyRepository sleepDailyRepo, Spo2SampleRepository spo2SampleRepository, ActivityDailyRepository activityDailyRepo, ReadinessDailyRepository readinessDailyRepo, SleepSessionRepository sleepSessionRepo, ActivitySessionRepository activitySessionRepo, HrSampleRepository hrSampleRepo, PatientRepository patientRepository) {
        this.whoopclient = whoopclient;
        this.connections = connections;
        this.sleepDailyRepo = sleepDailyRepo;
        this.spo2SampleRepository = spo2SampleRepository;
        this.activityDailyRepo = activityDailyRepo;
        this.readinessDailyRepo = readinessDailyRepo;
        this.sleepSessionRepo = sleepSessionRepo;
        this.activitySessionRepo = activitySessionRepo;
        this.hrSampleRepo = hrSampleRepo;
        this.patientRepository = patientRepository;
    }

    @Override public Provider provider() { return Provider.WHOOP; }

    @Override
    public String buildAuthorizeUrl(String state, String scopes, String overrideRedirectUri) {
        // не затеняем поле redirectUri
        String ru = (overrideRedirectUri != null && !overrideRedirectUri.isBlank())
                ? overrideRedirectUri
                : this.redirectUri;

        return UriComponentsBuilder
                .fromHttpUrl("https://api.prod.whoop.com/oauth/oauth2/auth")
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", ru)
                .queryParam("scope", scopes)
                .queryParam("state", state)
                .build()
                .encode(java.nio.charset.StandardCharsets.UTF_8)
                .toUriString();
    }

    @Override
    public void exchangeCodeAndSave(Long patientId, String code) {
        // ВАЖНО: правильный путь — /oauth/oauth2/token
        var form = "grant_type=authorization_code"
                + "&code=" + enc(code)
                + "&redirect_uri=" + enc(redirectUri)
                + "&client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret);

        var token = whoopclient.post()
                .uri("/oauth/oauth2/token") // <-- было /oauth/token (неверно)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResp.class);
        Optional<Patient> patient = patientRepository.findById(patientId);
        patient.get().setStatus("active");
        patient.get().setDevice("whoop");
        upsertTokens(patientId, token);
    }

    @Override
    public void disconnect(Long patientId) {
        connections.deleteByPatientIdAndProvider(patientId, Provider.WHOOP);
    }


    public int syncDaily(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        Connection c = requireConn(patientId);
        int total = 0;
        total += syncCyclesAsActivityDaily(patientId, from, to, c);
        total += syncRecoveryAsReadinessDaily(patientId, from, to, c);
        total += syncSleepDaily(patientId, from, to, c);
        return total;
    }

    /** Сессии за период: workout→activity_session, sleep→sleep_session */
    public int syncSessions(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        Connection c = requireConn(patientId);
        int total = 0;
        total += syncWorkouts(patientId, from, to, c);
        total += syncSleepSessions(patientId, from, to, c);
        return total;
    }


    public int syncAll(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        return syncDaily(patientId, from, to) + syncSessions(patientId, from, to);
    }


    @Override public int syncHeartRate(Long patientId, OffsetDateTime from, OffsetDateTime to) { return 0; }


    @Override public int syncSpO2(Long patientId, OffsetDateTime from, OffsetDateTime to) { return 0; }

    /* ==================== Реализации по сущностям ==================== */

    // 1) Циклы → activity_daily (strain, kJ→kcal)
    private int syncCyclesAsActivityDaily(Long pid, OffsetDateTime from, OffsetDateTime to, Connection c) {
        String base = "/developer/v2/cycle"; // read:cycles
        return paged(base, from, to, c, node -> {
            var recs = node.path("records");
            if (!recs.isArray()) return;
            for (JsonNode r : recs) {
                OffsetDateTime start = parseTs(r, "start");
                if (start == null) continue;
                LocalDate day = start.atZoneSameInstant(ZoneOffset.UTC).toLocalDate();

                JsonNode score = r.path("score");
                Double strain = score.hasNonNull("strain") ? score.get("strain").asDouble() : null;
                Double kJ     = score.hasNonNull("kilojoule") ? score.get("kilojoule").asDouble() : null;
                Integer avgHr = score.hasNonNull("average_heart_rate") ? score.get("average_heart_rate").asInt() : null;
                Integer maxHr = score.hasNonNull("max_heart_rate") ? score.get("max_heart_rate").asInt() : null;

                var existing = activityDailyRepo.findByPatientIdAndProviderAndDay(pid, Provider.WHOOP, day);
                var e = existing.orElseGet(() -> {
                    var x = new com.sportfd.healthapp.model.ActivityDaily();
                    x.setPatientId(pid); x.setProvider(Provider.WHOOP); x.setDay(day);
                    return x;
                });
                if (strain != null) e.setTrainingLoad((int) strain.floatValue());
                if (kJ != null) e.setCaloriesActive(Math.round(kJ.floatValue() / 4.184f)); // ~ккал

                activityDailyRepo.save(e);
            }
        });
    }


    private int syncRecoveryAsReadinessDaily(Long pid, OffsetDateTime from, OffsetDateTime to, Connection c) {
        String base = "/developer/v2/recovery"; // read:recovery
        return paged(base, from, to, c, node -> {
            var recs = node.path("records");
            if (!recs.isArray()) return;
            for (JsonNode r : recs) {
                OffsetDateTime start = parseTs(r, "created_at", "start");
                if (start == null) continue;
                LocalDate day = start.atZoneSameInstant(ZoneOffset.UTC).toLocalDate();

                JsonNode score = r.path("score");
                Integer scorePct = valInt(score, "recovery_score_percentage"); // иногда "recovery_score_percentage"
                Integer rhr      = valInt(score, "resting_heart_rate");
                Integer hrvMs    = valInt(score, "heart_rate_variability_rmssd_milli");

                var existing = readinessDailyRepo.findByPatientIdAndProviderAndDay(pid, Provider.WHOOP, day);
                var e = existing.orElseGet(() -> {
                    var x = new com.sportfd.healthapp.model.ReadinessDaily();
                    x.setPatientId(pid); x.setProvider(Provider.WHOOP); x.setDay(day);
                    return x;
                });
                if (scorePct != null) e.setScore((int) scorePct.shortValue());
                if (rhr != null)      e.setRhrBpm(rhr.shortValue());
                if (hrvMs != null)    e.setHrvAvgMs(hrvMs.shortValue());
                readinessDailyRepo.save(e);
            }
        });
    }


    private int syncSleepSessions(Long pid, OffsetDateTime from, OffsetDateTime to, Connection c) {
        String base = "/developer/v2/activity/sleep"; // read:sleep
        return paged(base, from, to, c, node -> {
            var recs = node.path("records");
            if (!recs.isArray()) return;
            for (JsonNode r : recs) {
                String id = valStr(r, "id");
                OffsetDateTime start = parseTs(r, "start");
                OffsetDateTime end   = parseTs(r, "end");
                if (start == null || end == null) continue;

                JsonNode score = r.path("score");
                Integer scorePct = valInt(score, "sleep_performance_percentage");

                long durSec = Duration.between(start, end).getSeconds();
                // HRV/RHR по ночи у WHOOP бывает в recovery, но не всегда в sleep — оставим null
                var opt = sleepSessionRepo.findByPatientIdAndProviderAndSourceId(pid, Provider.WHOOP, id);
                var e = opt.orElseGet(() -> {
                    var x = new com.sportfd.healthapp.model.SleepSession();
                    x.setPatientId(pid); x.setProvider(Provider.WHOOP); x.setSourceId(id);
                    x.setStartTime(start); x.setEndTime(end);
                    return x;
                });
                e.setDurationSec((int) durSec);
                if (scorePct != null) e.setScore((int) scorePct.shortValue());
                sleepSessionRepo.save(e);
            }
        });
    }


    private int syncSleepDaily(Long pid, OffsetDateTime from, OffsetDateTime to, Connection c) {
        final int[] saved = {0};
        paged("/developer/v2/activity/sleep", from, to, c, node -> {
            var recs = node.path("records");
            if (!recs.isArray()) return;
            for (JsonNode r : recs) {
                OffsetDateTime start = parseTs(r, "start");
                if (start == null) continue;
                LocalDate day = start.atZoneSameInstant(ZoneOffset.UTC).toLocalDate();

                JsonNode score = r.path("score");
                Integer sleepScore = valInt(score, "sleep_performance_percentage");

                JsonNode stages = score.path("stage_summary");
                long lightMs   = valLong(stages, "light_sleep_duration_milli");
                long remMs     = valLong(stages, "rem_sleep_duration_milli");
                long swsMs     = valLong(stages, "slow_wave_sleep_duration_milli");
                long totalMs   = lightMs + remMs + swsMs;

                var existing = sleepDailyRepo.findByPatientIdAndProviderAndDay(pid, Provider.WHOOP, day);
                var e = existing.orElseGet(() -> {
                    var x = new com.sportfd.healthapp.model.SleepDaily();
                    x.setPatientId(pid); x.setProvider(Provider.WHOOP); x.setDay(day);
                    return x;
                });
                if (sleepScore != null) e.setScore((int) sleepScore.shortValue());
                e.setTotalSleepSec((int) (totalMs / 1000));
                sleepDailyRepo.save(e);
                saved[0]++;
            }
        });
        return saved[0];
    }


    private int syncWorkouts(Long pid, OffsetDateTime from, OffsetDateTime to, Connection c) {
        String base = "/developer/v2/activity/workout"; // read:workout
        return paged(base, from, to, c, node -> {
            var recs = node.path("records");
            if (!recs.isArray()) return;
            for (JsonNode r : recs) {
                String id = valStr(r, "id");
                OffsetDateTime start = parseTs(r, "start");
                OffsetDateTime end   = parseTs(r, "end");
                if (start == null || end == null) continue;

                JsonNode s = r.path("score");
                Integer avgHr = valInt(s, "average_heart_rate");
                Double  kJ    = valDbl(s, "kilojoule");
                Integer distM = valInt(s, "distance_meter");
                // тип спорта может быть как enum/число/строка в зависимости от версии; берём name/label/id
                String sport  = valStr(r, "sport_name");
                if (sport == null) sport = valStr(r, "sport");
                if (sport == null && r.hasNonNull("sport_id")) sport = "sport:" + r.get("sport_id").asText();

                var opt = activitySessionRepo.findByPatientIdAndProviderAndSourceId(pid, Provider.WHOOP, id);
                var e = opt.orElseGet(() -> {
                    var x = new com.sportfd.healthapp.model.ActivitySession();
                    x.setPatientId(pid); x.setProvider(Provider.WHOOP); x.setSourceId(id);
                    x.setStartTime(start); x.setEndTime(end);
                    return x;
                });
                if (avgHr != null) e.setAvgHr(avgHr.shortValue());
                if (kJ != null)    e.setCalories(Math.round((float)(kJ / 4.184)));
                if (distM != null) e.setDistanceM(distM);
                if (sport != null) e.setSportType(sport);
                activitySessionRepo.save(e);
            }
        });
    }



    private Connection requireConn(Long pid) {
        return connections.findByPatientIdAndProvider(pid, Provider.WHOOP)
                .orElseThrow(() -> new IllegalStateException("WHOOP connection not found for patient " + pid));
    }

    private int paged(String basePath, OffsetDateTime from, OffsetDateTime to,
                      Connection c, Consumer<JsonNode> pageConsumer) {
        int saved = 0;
        String next = null;
        String token = c.getAccessToken();
        do {
            String url = org.springframework.web.util.UriComponentsBuilder.fromPath(basePath)
                    .queryParam("start", from.toString())
                    .queryParam("end", to.toString())
                    .queryParam("limit", 25)
                    .queryParamIfPresent("nextToken", Optional.ofNullable(next))
                    .build().encode().toUriString();

            JsonNode root = getJson(url, token);
            if (root == null) break;
            pageConsumer.accept(root);
            next = valStr(root, "next_token");
            saved++; // счёт страниц
        } while (next != null && !next.isBlank());
        return saved;
    }

    private JsonNode getJson(String url, String accessToken) {
        try {
            return whoopclient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            // попытаться обновить
            String fresh = refreshAccessToken(accessToken);
            if (fresh == null) throw e;
            return whoopclient.get().uri(url)
                    .header("Authorization", "Bearer " + fresh)
                    .retrieve()
                    .body(JsonNode.class);
        }
    }

    private String refreshAccessToken(String oldAccessToken) {
        // найдём connection по accessToken и обновим
        var connOpt = connections.findByAccessToken(oldAccessToken);
        if (connOpt == null) return null;
        var c = connOpt;


        String form = "grant_type=refresh_token"
                + "&refresh_token=" + enc(c.getRefreshToken())
                + "&client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret);

        var token = whoopclient.post().uri("/oauth/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResp.class);

        c.setAccessToken(token.access_token);
        if (token.refresh_token != null) c.setRefreshToken(token.refresh_token);
        if (token.expires_in != null) c.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(token.expires_in));
        c.setTokenType("Bearer");
        connections.save(c);
        return token.access_token;
    }

    @Transactional
    protected void upsertTokens(Long pid, TokenResp t) {
        var c = connections.findByPatientIdAndProvider(pid, Provider.WHOOP).orElseGet(Connection::new);
        if (c.getId() == null) { c.setPatientId(pid); c.setProvider(Provider.WHOOP); }
        c.setAccessToken(t.access_token);
        if (t.refresh_token != null) c.setRefreshToken(t.refresh_token);
        c.setScope(t.scope);
        if (t.expires_in != null) c.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(t.expires_in));
        c.setTokenType("Bearer");
        connections.save(c);
    }
    // daily (LocalDate)
    @Override public int syncSleepDaily(Long id, LocalDate s, LocalDate e) {
        return syncDaily(id, s.atStartOfDay().atOffset(ZoneOffset.UTC),
                e.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
    }
    @Override public int syncActivityDaily(Long id, LocalDate s, LocalDate e) {
        return syncDaily(id, s.atStartOfDay().atOffset(ZoneOffset.UTC),
                e.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
    }
    @Override public int syncReadinessDaily(Long id, LocalDate s, LocalDate e) {
        return syncDaily(id, s.atStartOfDay().atOffset(ZoneOffset.UTC),
                e.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
    }

    // sessions (OffsetDateTime)
    @Override public int syncSleepSessions(Long id, OffsetDateTime from, OffsetDateTime to) {
        return syncSessions(id, from, to);
    }
    @Override public int syncActivitySessions(Long id, OffsetDateTime from, OffsetDateTime to) {
        return syncSessions(id, from, to);
    }
    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    private static OffsetDateTime parseTs(JsonNode n, String... keys) {
        for (var k : keys) if (n.hasNonNull(k)) {
            try { return OffsetDateTime.parse(n.get(k).asText()); } catch (Exception ignored) {}
        }
        return null;
    }
    private static Integer valInt(JsonNode n, String key) { return n != null && n.hasNonNull(key) ? n.get(key).asInt() : null; }
    private static Long    valLong(JsonNode n, String key){ return n != null && n.hasNonNull(key) ? n.get(key).asLong() : 0L; }
    private static Double  valDbl(JsonNode n, String key) { return n != null && n.hasNonNull(key) ? n.get(key).asDouble() : null; }
    private static String  valStr(JsonNode n, String key) { return n != null && n.hasNonNull(key) ? n.get(key).asText() : null; }

    private static class TokenResp {
        public String access_token;
        public String refresh_token;
        public Long   expires_in;
        public String token_type;
        public String scope;
    }
}

