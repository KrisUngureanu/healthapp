package com.sportfd.healthapp.integration.oura;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportfd.healthapp.integration.ProviderClient;
import com.sportfd.healthapp.model.*;
import com.sportfd.healthapp.model.enums.Provider;
import com.sportfd.healthapp.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component

public class OuraClient implements ProviderClient {


    private final RestClient ouraRest;
    private final ConnectionRepository connections;
    private final SleepDailyRepository sleepDailyRepo;
    private final Spo2SampleRepository spo2SampleRepository;

    public OuraClient(@Qualifier("ouraRestClient") RestClient ouraRest, ConnectionRepository connections, SleepDailyRepository sleepDailyRepo, Spo2SampleRepository spo2SampleRepository, ActivityDailyRepository activityDailyRepository, ReadinessDailyRepository readinessDailyRepo, SleepSessionRepository sleepSessionRepo, ActivitySessionRepository activitySessionRepo, HrSampleRepository hrSampleRepo) {
        this.ouraRest = ouraRest;

        this.connections = connections;
        this.sleepDailyRepo = sleepDailyRepo;
        this.spo2SampleRepository = spo2SampleRepository;
        this.activityDailyRepository = activityDailyRepository;
        this.readinessDailyRepo = readinessDailyRepo;
        this.sleepSessionRepo = sleepSessionRepo;
        this.activitySessionRepo = activitySessionRepo;
        this.hrSampleRepo = hrSampleRepo;
    }

    public record DaySteps(LocalDate day, Integer steps) {}
    public final ActivityDailyRepository activityDailyRepository;
    private final ReadinessDailyRepository readinessDailyRepo;
    private final SleepSessionRepository sleepSessionRepo;
    private final ActivitySessionRepository activitySessionRepo;
    private final HrSampleRepository hrSampleRepo;
    @Value("${app.oura.client-id}")     private String clientId;
    @Value("${app.oura.client-secret}") private String clientSecret;
    @Value("${app.oura.redirect-uri}")  private String redirectUri;

    @Override public Provider provider() { return Provider.OURA; }

    // ---------- OAuth ----------

    @Override
    public String buildAuthorizeUrl(String state, String scopes, String redirect) {
        String ru = (redirect != null && !redirect.isBlank()) ? redirect : redirectUri;
        return UriComponentsBuilder
                .fromHttpUrl("https://cloud.ouraring.com/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scopes)
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    @Override
    @Transactional
    public void exchangeCodeAndSave(Long patientId, String code) {
        var form = "grant_type=authorization_code"
                + "&code=" + enc(code)
                + "&redirect_uri=" + enc(redirectUri)
                + "&client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret);

        var token = ouraRest.post().uri("/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResp.class);

        upsertTokens(patientId, token);
    }

    @Override
    @Transactional
    public void disconnect(Long patientId) {
        connections.deleteByPatientIdAndProvider(patientId, Provider.OURA);
    }

    // ---------- Sync: daily_sleep → sleep_daily ----------

    @Override
    @Transactional
    public int syncSleepDaily(Long patientId, LocalDate start, LocalDate end) {
        Connection c = requireConn(patientId);
        String url = UriComponentsBuilder.fromPath("/v2/usercollection/daily_sleep")
                .queryParam("start_date", start.toString())
                .queryParam("end_date", end.toString())
                .build().encode().toUriString();

        JsonNode root = getJson(url, c.getAccessToken());
        JsonNode items = root != null && root.has("data") ? root.get("data") : root;
        if (items == null || !items.isArray()) return 0;

        int saved = 0;
        for (JsonNode n : items) {
            // безопасные извлечения (Oura отдает day, score и др.)
            LocalDate day = n.hasNonNull("day") ? LocalDate.parse(n.get("day").asText()) : null;
            if (day == null) continue;

            Integer score = n.hasNonNull("score") ? n.get("score").asInt() : null;
            Integer totalSleep = pickInt(n, "total_sleep_duration", "total_sleep_time", "duration");
            Integer timeInBed  = pickInt(n, "time_in_bed", "time_in_bed_duration");
            Short rhr = pickShort(n, "average_bpm", "rhr");
            Short hrv = pickShort(n, "average_hrv", "rmssd");

            SleepDaily rec = sleepDailyRepo.findByPatientIdAndProviderAndDay(patientId, Provider.OURA, day)
                    .orElseGet(SleepDaily::new);

            if (rec.getId() == null) {
                rec.setPatientId(patientId);
                rec.setProvider(Provider.OURA);
                rec.setDay(day);
            }
            rec.setScore(score);
            rec.setTotalSleepSec(totalSleep);
            rec.setTimeInBedSec(timeInBed);
            rec.setRhrBpm(rhr);
            rec.setHrvAvgMs(hrv);
            rec.setRaw(n.toString());

            sleepDailyRepo.save(rec);
            saved++;
        }
        return saved;
    }

    // ---------- helpers ----------

    private Connection requireConn(Long pid) {
        return connections.findByPatientIdAndProvider(pid, Provider.OURA)
                .orElseThrow(() -> new IllegalStateException("Oura not connected for patient " + pid));
    }

    private JsonNode getJson(String path, String accessToken) {
        return ouraRest.get().uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);
    }

    @Transactional
    protected String refreshIf401(Connection c) {
        var form = "grant_type=refresh_token"
                + "&refresh_token=" + enc(c.getRefreshToken())
                + "&client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret);

        var token = ouraRest.post().uri("/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResp.class);

        upsertTokens(c.getPatientId(), token);
        return token.access_token;
    }

    @Transactional
    protected void upsertTokens(Long pid, TokenResp t) {
        Connection c = connections.findByPatientIdAndProvider(pid, Provider.OURA)
                .orElseGet(Connection::new);
        if (c.getId() == null) {
            c.setPatientId(pid);
            c.setProvider(Provider.OURA);
        }
        c.setAccessToken(t.access_token);
        if (t.refresh_token != null) c.setRefreshToken(t.refresh_token);
        c.setScope(t.scope);
        if (t.expires_in != null) {
            c.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(t.expires_in));
        }
        connections.save(c);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // минимальная модель ответа токена
    private static class TokenResp {
        public String access_token;
        public String refresh_token;
        public Long   expires_in;
        public String token_type;
        public String scope;
    }

    private static Integer pickInt(JsonNode n, String... keys) {
        for (var k : keys) if (n.hasNonNull(k)) return n.get(k).asInt();
        return null;
    }
    private static Short pickShort(JsonNode n, String... keys) {
        for (var k : keys) if (n.hasNonNull(k)) return (short)n.get(k).asInt();
        return null;
    }

    public List<DaySteps> getDailySteps(Long patientId, LocalDate start, LocalDate end) {
        Connection c = requireConn(patientId);

        String url = UriComponentsBuilder.fromPath("/v2/usercollection/daily_activity")
                .queryParam("start_date", start.toString())
                .queryParam("end_date", end.toString())
                .build().encode().toUriString();

        JsonNode root = getJson(url, c.getAccessToken());
        JsonNode data = (root != null && root.has("data")) ? root.get("data") : root;

        // Если токен протух — попробуем обновить и повторить один раз
        if ((data == null || !data.isArray()) && c.getRefreshToken() != null) {
            String newAt = refreshIf401(c);
            root = getJson(url, newAt);
            data = (root != null && root.has("data")) ? root.get("data") : root;
        }

        List<DaySteps> out = new ArrayList<>();
        if (data == null || !data.isArray()) return out;

        for (JsonNode n : data) {
            LocalDate day = n.hasNonNull("day") ? LocalDate.parse(n.get("day").asText()) : null;
            Integer steps  = n.hasNonNull("steps") ? n.get("steps").asInt() : null; // ключ у Oura — steps
            if (day != null) out.add(new DaySteps(day, steps));
        }
        // отсортируем на всякий
        out.sort(Comparator.comparing(DaySteps::day));
        return out;
    }

    @Override
    @Transactional
    public int syncReadinessDaily(Long patientId, LocalDate start, LocalDate end) {
        Connection c = requireConn(patientId);
        String url = UriComponentsBuilder.fromPath("/v2/usercollection/daily_readiness")
                .queryParam("start_date", start.toString())
                .queryParam("end_date", end.toString())
                .build().encode().toUriString();

        JsonNode root = getJson(url, c.getAccessToken());
        JsonNode data = (root != null && root.has("data")) ? root.get("data") : root;
        if ((data == null || !data.isArray()) && c.getRefreshToken() != null) {
            String at = refreshIf401(c);
            root = getJson(url, at);
            data = (root != null && root.has("data")) ? root.get("data") : root;
        }
        if (data == null || !data.isArray()) return 0;

        int saved = 0;
        for (JsonNode n : data) {
            LocalDate day = n.hasNonNull("day") ? LocalDate.parse(n.get("day").asText()) : null;
            if (day == null) continue;

            var rec = readinessDailyRepo
                    .findByPatientIdAndProviderAndDay(patientId, Provider.OURA, day)
                    .orElseGet(ReadinessDaily::new);

            if (rec.getId() == null) {
                rec.setPatientId(patientId);
                rec.setProvider(Provider.OURA);
                rec.setDay(day);
            }


            rec.setScore(n.hasNonNull("score") ? n.get("score").asInt() : null);
            rec.setRhrBpm(pickShort(n, "resting_heart_rate", "average_bpm"));
            rec.setHrvAvgMs(pickShort(n, "rmssd", "average_hrv"));
            rec.setTempDeviationC(pickDecimal(n, "temperature_deviation"));
            rec.setRaw(n.toString());

            readinessDailyRepo.save(rec);
            saved++;
        }
        return saved;
    }



    @Override
    @Transactional
    public int syncActivityDaily(Long patientId, LocalDate start, LocalDate end) {
        Connection c = requireConn(patientId);
        String url = UriComponentsBuilder.fromPath("/v2/usercollection/daily_activity")
                .queryParam("start_date", start.toString())
                .queryParam("end_date", end.toString())
                .build().encode().toUriString();

        JsonNode root = getJson(url, c.getAccessToken());
        JsonNode data = (root != null && root.has("data")) ? root.get("data") : root;
        if ((data == null || !data.isArray()) && c.getRefreshToken() != null) {
            String at = refreshIf401(c);
            root = getJson(url, at);
            data = (root != null && root.has("data")) ? root.get("data") : root;
        }
        if (data == null || !data.isArray()) return 0;

        int saved = 0;
        for (JsonNode n : data) {
            LocalDate day = n.hasNonNull("day") ? LocalDate.parse(n.get("day").asText()) : null;
            if (day == null) continue;

            var rec = activityDailyRepository
                    .findByPatientIdAndProviderAndDay(patientId, Provider.OURA, day)
                    .orElseGet(ActivityDaily::new);

            if (rec.getId() == null) {
                rec.setPatientId(patientId);
                rec.setProvider(Provider.OURA);
                rec.setDay(day);
            }

            rec.setSteps(pickInt(n, "steps"));
            rec.setCaloriesActive(pickInt(n, "active_calories", "calories_active"));
            // у Oura бывает daily_movement (эквивалент метров) или distance
            rec.setDistanceM(pickInt(n, "daily_movement", "distance"));
            rec.setTrainingLoad(pickInt(n, "training_load"));
            rec.setRaw(n.toString());

            activityDailyRepository.save(rec);
            saved++;
        }
        return saved;
    }
    @Override
    @Transactional
    public int syncSleepSessions(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        Connection c = requireConn(patientId);
        LocalDate start = from.toLocalDate();
        LocalDate end   = to.toLocalDate();

        String url = UriComponentsBuilder.fromPath("/v2/usercollection/sleep")
                .queryParam("start_date", start.toString())
                .queryParam("end_date",   end.toString())
                .build().encode().toUriString();

        JsonNode root = getJson(url, c.getAccessToken());
        JsonNode data = (root != null && root.has("data")) ? root.get("data") : root;
        if ((data == null || !data.isArray()) && c.getRefreshToken() != null) {
            String at = refreshIf401(c);
            root = getJson(url, at);
            data = (root != null && root.has("data")) ? root.get("data") : root;
        }
        if (data == null || !data.isArray()) return 0;

        int saved = 0;
        for (JsonNode n : data) {
            String sid = pickText(n, "id", "uuid");
            if (sid == null) continue;

            OffsetDateTime st = pickTs(n, "start_datetime");
            OffsetDateTime en = pickTs(n, "end_datetime");
            if (st == null || en == null) continue;

            var rec = sleepSessionRepo
                    .findByPatientIdAndProviderAndSourceId(patientId, Provider.OURA, sid)
                    .orElseGet(SleepSession::new);

            if (rec.getId() == null) {
                rec.setPatientId(patientId);
                rec.setProvider(Provider.OURA);
                rec.setSourceId(sid);
            }
            rec.setStartTime(st);
            rec.setEndTime(en);
            rec.setDurationSec(pickInt(n, "total_sleep_duration", "duration"));
            rec.setScore(pickInt(n, "score"));
            rec.setEfficiency(pickShort(n, "efficiency"));
            rec.setIsNap(pickBool(n, "is_nap"));
            rec.setHrAvg(pickShort(n, "average_bpm"));
            rec.setHrMin(pickShort(n, "lowest_bpm"));
            rec.setHrvAvgMs(pickShort(n, "rmssd"));
            rec.setRaw(n.toString());

            sleepSessionRepo.save(rec);
            saved++;
        }
        return saved;
    }

    @Override
    @Transactional
    public int syncActivitySessions(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        Connection c = requireConn(patientId);
        String url = UriComponentsBuilder.fromPath("/v2/usercollection/workout")
                .queryParam("start_datetime", from.toString())
                .queryParam("end_datetime",   to.toString())
                .build().encode().toUriString();

        JsonNode root = getJson(url, c.getAccessToken());
        JsonNode data = (root != null && root.has("data")) ? root.get("data") : root;
        if ((data == null || !data.isArray()) && c.getRefreshToken() != null) {
            String at = refreshIf401(c);
            root = getJson(url, at);
            data = (root != null && root.has("data")) ? root.get("data") : root;
        }
        if (data == null || !data.isArray()) return 0;

        int saved = 0;
        for (JsonNode n : data) {
            String sid = pickText(n, "id", "uuid");
            OffsetDateTime st = pickTs(n, "start_datetime");
            OffsetDateTime en = pickTs(n, "end_datetime");
            if (sid == null || st == null || en == null) continue;

            var rec = activitySessionRepo
                    .findByPatientIdAndProviderAndSourceId(patientId, Provider.OURA, sid)
                    .orElseGet(ActivitySession::new);

            if (rec.getId() == null) {
                rec.setPatientId(patientId);
                rec.setProvider(Provider.OURA);
                rec.setSourceId(sid);
            }
            rec.setStartTime(st);
            rec.setEndTime(en);
            rec.setSportType(pickText(n, "sport", "class", "type"));
            rec.setCalories(pickInt(n, "calories", "kilocalories"));
            rec.setDistanceM(pickInt(n, "distance", "distance_m"));
            rec.setAvgHr(pickShort(n, "average_heart_rate", "average_bpm"));
            rec.setMaxHr(pickShort(n, "max_heart_rate", "max_bpm"));
            rec.setLoad(pickInt(n, "training_load", "load"));
            rec.setRaw(n.toString());

            activitySessionRepo.save(rec);
            saved++;
        }
        return saved;
    }

    @Transactional
    @Override
    public int syncHeartRate(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        Connection c = requireConn(patientId);
        String url = UriComponentsBuilder.fromPath("/v2/usercollection/heartrate")
                .queryParam("start_datetime", from.toString())
                .queryParam("end_datetime",   to.toString())
                .build().encode().toUriString();

        JsonNode root = getJson(url, c.getAccessToken());
        JsonNode data = (root != null && root.has("data")) ? root.get("data") : root;
        if ((data == null || !data.isArray()) && c.getRefreshToken() != null) {
            String at = refreshIf401(c);
            root = getJson(url, at);
            data = (root != null && root.has("data")) ? root.get("data") : root;
        }
        if (data == null || !data.isArray()) return 0;

        int saved = 0;
        for (JsonNode n : data) {
            OffsetDateTime ts = pickTs(n, "timestamp", "time");
            Short bpm = n.hasNonNull("bpm") ? (short)n.get("bpm").asInt() : null;
            if (ts == null || bpm == null) continue;

            if (hrSampleRepo.findByPatientIdAndProviderAndTs(patientId, Provider.OURA, ts).isEmpty()) {
                var rec = new HrSample();
                rec.setPatientId(patientId);
                rec.setProvider(Provider.OURA);
                rec.setTs(ts);
                rec.setBpm(bpm);
                hrSampleRepo.save(rec);
                saved++;
            }
        }
        return saved;
    }
    @Transactional
    @Override
    public int syncSpO2(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        Connection c = requireConn(patientId);
        String url = UriComponentsBuilder.fromPath("/v2/usercollection/daily_spo2")
                .queryParam("start_datetime", from.toString())
                .queryParam("end_datetime",   to.toString())
                .build().encode().toUriString();

        JsonNode root = getJson(url, c.getAccessToken());
        JsonNode data = (root != null && root.has("data")) ? root.get("data") : root;
        if ((data == null || !data.isArray()) && c.getRefreshToken() != null) {
            String at = refreshIf401(c);
            root = getJson(url, at);
            data = (root != null && root.has("data")) ? root.get("data") : root;
        }
        if (data == null || !data.isArray()) return 0;

        int saved = 0;
        for (JsonNode n : data) {
            OffsetDateTime ts = pickTs(n, "timestamp", "time");
            var spo2 = pickDecimal(n, "spo2", "percentage", "spo2_percentage"); // берём первое найденное поле
            if (ts == null || spo2 == null) continue;

            if (spo2SampleRepository.findByPatientIdAndProviderAndTs(patientId, Provider.OURA, ts).isEmpty()) {
                var rec = new Spo2Sample();
                rec.setPatientId(patientId);
                rec.setProvider(Provider.OURA);
                rec.setTs(ts);
                rec.setSpo2Pct(spo2);
                spo2SampleRepository.save(rec);
                saved++;
            }
        }
        return saved;
    }

    private static String pickText(JsonNode n, String... keys) {
        for (var k : keys) if (n.hasNonNull(k)) return n.get(k).asText();
        return null;
    }
    private static Boolean pickBool(JsonNode n, String... keys) {
        for (var k : keys) if (n.hasNonNull(k)) return n.get(k).asBoolean();
        return null;
    }
    private static BigDecimal pickDecimal(JsonNode n, String... keys) {
        for (var k : keys) if (n.hasNonNull(k)) return new BigDecimal(n.get(k).asText());
        return null;
    }
    private static OffsetDateTime pickTs(JsonNode n, String... keys) {
        for (var k : keys) if (n.hasNonNull(k)) {
            try { return OffsetDateTime.parse(n.get(k).asText()); }
            catch (Exception ignored) {}
        }
        return null;
    }
}