package com.sportfd.healthapp.integration.whoop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sportfd.healthapp.integration.ProviderClient;
import com.sportfd.healthapp.model.*;
import com.sportfd.healthapp.model.enums.Provider;
import com.sportfd.healthapp.repo.*;
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
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Component

public class WhoopClient implements ProviderClient {

    @Qualifier("whoopRestClient")
    private final RestClient whoopclient;
    private final ConnectionRepository connections;

    private final PatientRepository patientRepository;
    private final WhoopRecoveryRepository whoopRecoveryRepository;
    private final WhoopSleepRepository whoopSleepRepository;
    private final WhoopWorkoutRepository whoopWorkoutRepository;
    private final WhoopCycleRepository whoopCycleRepository;
    @Value("${app.whoop.client-id}")
    private String clientId;
    @Value("${app.whoop.client-secret}")
    private String clientSecret;
    @Value("${app.whoop.redirect-uri}")
    private String redirectUri;

    public WhoopClient(@Qualifier("whoopRestClient") RestClient whoopclient, ConnectionRepository connections, PatientRepository patientRepository, WhoopRecoveryRepository whoopRecoveryRepository, WhoopSleepRepository whoopSleepRepository, WhoopWorkoutRepository whoopWorkoutRepository, WhoopCycleRepository whoopCycleRepository) {
        this.whoopclient = whoopclient;
        this.connections = connections;

        this.patientRepository = patientRepository;
        this.whoopRecoveryRepository = whoopRecoveryRepository;
        this.whoopSleepRepository = whoopSleepRepository;
        this.whoopWorkoutRepository = whoopWorkoutRepository;
        this.whoopCycleRepository = whoopCycleRepository;
    }

    @Override
    public Provider provider() {
        return Provider.WHOOP;
    }

    @Override
    public String buildAuthorizeUrl(String state, String scopes, String overrideRedirectUri) {
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


    @Override
    public void syncWorkout(Long pid, OffsetDateTime from, OffsetDateTime to) {
        Connection c = requireConn(pid);
        String base = "/developer/v2/activity/workout";
        paged(base, from, to, c, node -> {
            var records = node.path("records");
            for (JsonNode record : records) {
                String record_id = record.path("id").asText(null);
                int user_id = record.path("user_id").asInt();
                OffsetDateTime created_at = OffsetDateTime.parse(record.path("created_at").asText());
                OffsetDateTime updated_at = OffsetDateTime.parse(record.path("updated_at").asText(null));

                OffsetDateTime start = OffsetDateTime.parse(record.path("start").asText(null));
                OffsetDateTime enddate = null;
                if (record.hasNonNull("end")) {
                    enddate = OffsetDateTime.parse(record.path("end").asText());
                }
                String timezone_offset = record.path("timezone_offset").asText("Z");
                String score_state = record.path("score_state").asText("SCORED");
                String sport_name = record.path("sport_name").asText(null);

                JsonNode score = record.path("score");
                float strain = score.path("strain").floatValue();
                int average_heart_rate = score.path("average_heart_rate").asInt();
                int max_heart_rate = score.path("max_heart_rate").asInt();
                float kilojoule = (float) score.path("kilojoule").asDouble(0d);
                float percent_recorded = (float) score.path("percent_recorded").asDouble(0d);
                float distance_meter = (float) score.path("distance_meter").asDouble(0d);
                float altitude_gain_meter = (float) score.path("altitude_gain_meter").asDouble(0d);
                float altitude_change_meter = (float) score.path("altitude_change_meter").asDouble(0d);

                //проверим на уникальность

                Optional<WhoopWorkout> workoutOld = whoopWorkoutRepository.findByRecordId(record_id);
                if (workoutOld.isEmpty()) {
                    WhoopWorkout workout = new WhoopWorkout();
                    workout.setRecord_id(record_id);
                    workout.setUserid(user_id);
                    workout.setPatient_id(pid);
                    workout.setCreated_at(created_at);
                    workout.setUpdated_at(updated_at);
                    workout.setStart(start);
                    if (enddate != null) {
                        workout.setEnddate(enddate);
                    }
                    workout.setTimezone_offset(timezone_offset);
                    workout.setScore_state(score_state);
                    workout.setSport_name(sport_name);
                    workout.setStrain(strain);
                    workout.setAverage_heart_rate(average_heart_rate);
                    workout.setMax_heart_rate(max_heart_rate);
                    workout.setKilojoule(kilojoule);
                    workout.setPercent_recorded(percent_recorded);
                    workout.setDistance_meter(distance_meter);
                    workout.setAltitude_gain_meter(altitude_gain_meter);
                    workout.setAltitude_change_meter(altitude_change_meter);

                    whoopWorkoutRepository.save(workout);

                }

            }
        });
    }

    @Override
    public void syncAll(Long pid, OffsetDateTime from, OffsetDateTime to) {
        syncRecovery(pid, from, to);
        syncCycles(pid, from, to);
        syncSleep(pid, from, to);
        syncWorkout(pid, from, to);

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
    public void syncCycles(Long pid, OffsetDateTime from, OffsetDateTime to) {
        Connection c = requireConn(pid);
        String base = "/developer/v2/cycle";
        paged(base, from, to, c, node -> {
            var records = node.path("records");
            for (JsonNode record : records) {
                int record_id = record.path("id").asInt();
                int user_id = record.path("user_id").asInt();
                OffsetDateTime created_at = OffsetDateTime.parse(record.path("created_at").asText());
                OffsetDateTime updated_at = OffsetDateTime.parse(record.path("updated_at").asText(null));

                OffsetDateTime start = OffsetDateTime.parse(record.path("start").asText(null));
                OffsetDateTime enddate = null;
                if (record.hasNonNull("end")) {
                    enddate = OffsetDateTime.parse(record.path("end").asText());
                }
                String timezone_offset = record.path("timezone_offset").asText("Z");
                String score_state = record.path("score_state").asText("SCORED");
                JsonNode score = record.path("score");
                float strain = score.path("strain").floatValue();
                float kilojoule = score.path("kilojoule").floatValue();
                int avgHr = score.path("average_heart_rate").asInt();
                int maxHr = score.path("max_heart_rate").asInt();

                //проверим на уникальность

                Optional<WhoopCycle> cycleOld = whoopCycleRepository.findByRecordId(record_id);
                if (cycleOld.isEmpty()) {
                    WhoopCycle cycle = new WhoopCycle();
                    cycle.setRecord_id((int) record_id);
                    cycle.setUserid(user_id);
                    cycle.setPatient_id(pid);
                    cycle.setCreated_at(created_at);
                    cycle.setUpdated_at(updated_at);
                    cycle.setScore_state(score_state);
                    cycle.setStart(start);
                    cycle.setEnddate(enddate);
                    cycle.setTimezone_offset(timezone_offset);
                    cycle.setStrain(strain);
                    cycle.setKilojoule(kilojoule);
                    cycle.setAverage_heart_rate(avgHr);
                    cycle.setMax_heart_rate(maxHr);

                    whoopCycleRepository.save(cycle);
                }

            }
        });

    }

    @Override
    public void syncSleep(Long pid, OffsetDateTime from, OffsetDateTime to) {
        String base = "/developer/v2/activity/sleep";
        Connection c = requireConn(pid);
        paged(base, from, to, c, node -> {
            var records = node.path("records");
            for (JsonNode record : records) {
                String record_id = record.path("id").asText();
                int cycle_id = record.path("cycle_id").asInt();
                int user_id = record.path("user_id").asInt();
                OffsetDateTime created_at = OffsetDateTime.parse(record.path("created_at").asText());
                OffsetDateTime updated_at = OffsetDateTime.parse(record.path("updated_at").asText(null));

                OffsetDateTime start = OffsetDateTime.parse(record.path("start").asText(null));
                OffsetDateTime enddate = null;
                if (record.hasNonNull("end")) {
                    enddate = OffsetDateTime.parse(record.path("end").asText());
                }
                String timezone_offset = record.path("timezone_offset").asText("Z");
                boolean nap = record.path("nap").asBoolean(false);
                String score_state = record.path("score_state").asText("SCORED");
                JsonNode score = record.path("score");
                JsonNode stage = score.path("stage_summary");
                JsonNode needed = score.path("sleep_needed");
                int inBed = stage.path("total_in_bed_time_milli").asInt(0);
                int awake = stage.path("total_awake_time_milli").asInt(0);
                int noData = stage.path("total_no_data_time_milli").asInt(0);
                int light = stage.path("total_light_sleep_time_milli").asInt(0);
                int sws = stage.path("total_slow_wave_sleep_time_milli").asInt(0);
                int rem = stage.path("total_rem_sleep_time_milli").asInt(0);
                int cycles = stage.path("sleep_cycle_count").asInt(0);
                int disturb = stage.path("disturbance_count").asInt(0);


                int baseline = needed.path("baseline_milli").asInt(0);
                int debt = needed.path("need_from_sleep_debt_milli").asInt(0);
                int strainNeed = needed.path("need_from_recent_strain_milli").asInt(0);
                int napNeed = needed.path("need_from_recent_nap_milli").asInt(0);

                float rr = (float) score.path("respiratory_rate").asDouble(0d);
                float perfPct = (float) score.path("sleep_performance_percentage").asDouble(0d);
                float consistencyPct = (float) score.path("sleep_consistency_percentage").asDouble(0d);
                float efficiencyPct = (float) score.path("sleep_efficiency_percentage").asDouble(0d);

                var existingOpt = whoopSleepRepository.findByRecordId(record_id);
                WhoopSleep e = existingOpt.orElseGet(WhoopSleep::new);

                e.setRecord_id(record_id);
                e.setUserid(user_id);
                e.setPatient_id(pid);
                e.setCycle_id((long) cycle_id);

                e.setCreated_at(created_at);
                e.setUpdated_at(updated_at);
                e.setStart(start);
                e.setEnddate(enddate);

                e.setTimezone_offset(timezone_offset);
                e.setScore_state(score_state);
                e.setNap(nap);

                e.setTotal_in_bed_time_milli(inBed);
                e.setTotal_awake_time_milli(awake);
                e.setTotal_no_data_time_milli(noData);
                e.setTotal_light_sleep_time_milli(light);
                e.setTotal_slow_wave_sleep_time_milli(sws);
                e.setTotal_rem_sleep_time_milli(rem);
                e.setSleep_cycle_count(cycles);
                e.setDisturbance_count(disturb);

                e.setBaseline_milli(baseline);
                e.setNeed_from_sleep_debt_milli(debt);
                e.setNeed_from_recent_strain_milli(strainNeed);
                e.setNeed_from_recent_nap_milli(napNeed);

                e.setRespiratory_rate(rr);
                e.setSleep_performance_percentage(perfPct);
                e.setSleep_consistency_percentage(consistencyPct);
                e.setSleep_efficiency_percentage(efficiencyPct);

                whoopSleepRepository.save(e);

            }
        });
    }

    @Override
    public void syncRecovery(Long pid, OffsetDateTime from, OffsetDateTime to) {
        String base = "/developer/v2/recovery";
        Connection c = requireConn(pid);
        paged(base, from, to, c, node -> {
            var records = node.path("records");
            for (JsonNode record : records) {
                String sleep_id = record.path("sleep_id").asText();
                int cycle_id = record.path("cycle_id").asInt();
                int user_id = record.path("user_id").asInt();
                OffsetDateTime created_at = OffsetDateTime.parse(record.path("created_at").asText());
                OffsetDateTime updated_at = OffsetDateTime.parse(record.path("updated_at").asText(null));
                String score_state = record.path("score_state").asText("SCORED");
                JsonNode score = record.path("score");
                boolean user_calibrating = score.path("user_calibrating").asBoolean(false);
                float recovery_score = (float) score.path("recovery_score").asDouble(0d);
                float resting_heart_rate = (float) score.path("resting_heart_rate").asDouble(0d);
                float hrv_rmssd_milli = (float) score.path("hrv_rmssd_milli").asDouble(0d);
                float spo2_percentage = (float) score.path("spo2_percentage").asDouble(0d);
                float skin_temp_celsius = (float) score.path("skin_temp_celsius").asDouble(0d);
                var existingOpt = whoopRecoveryRepository.findByRecordId(sleep_id);
                WhoopRecovery recovery = existingOpt.orElseGet(WhoopRecovery::new);
                recovery.setRecord_id(cycle_id);
                recovery.setSleep_id(sleep_id);
                recovery.setCycle_id(cycle_id);
                recovery.setUserid(user_id);
                recovery.setPatient_id(pid);
                recovery.setCreated_at(created_at);
                recovery.setUpdated_at(updated_at);
                recovery.setScore_state(score_state);
                recovery.setUser_calibrating(user_calibrating);
                recovery.setRecovery_score(recovery_score);
                recovery.setResting_heart_rate(resting_heart_rate);
                recovery.setHrv_rmssd_milli(hrv_rmssd_milli);
                recovery.setSpo2_percentage(spo2_percentage);
                recovery.setSkin_temp_celsius(skin_temp_celsius);

                whoopRecoveryRepository.save(recovery);
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
            saved++;
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
        if (c.getId() == null) {
            c.setPatientId(pid);
            c.setProvider(Provider.WHOOP);
        }
        c.setAccessToken(t.access_token);
        if (t.refresh_token != null) c.setRefreshToken(t.refresh_token);
        c.setScope(t.scope);
        if (t.expires_in != null) c.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(t.expires_in));
        c.setTokenType("Bearer");
        connections.save(c);
    }


    public void syncProfile(Long patientId) {
        Connection c = requireConn(patientId);
        JsonNode root = getJson("/developer/v2/user/profile/basic", c.getAccessToken());


        var p = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalStateException("Patient not found " + patientId));


        if (root.hasNonNull("birth_date")) {
            try {
                p.setBirthDate(java.time.LocalDate.parse(root.get("birth_date").asText()));
            } catch (Exception ignored) {
            }
        }
        if (root.hasNonNull("gender")) {
            p.setSex(root.get("gender").asText()); // или enum, если есть
        }

        if (root.isObject()) {
            ((ObjectNode) root).remove(List.of());
            p.setProfileJson(root.toString());
        }
        patientRepository.save(p);
    }

    public int syncBodyMeasurement(Long patientId) {
        Connection c = requireConn(patientId);
        JsonNode root = getJson("/developer/v2/user/measurement/body", c.getAccessToken());
        if (root == null || !root.isObject()) return 0;

        var p = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalStateException("Patient not found " + patientId));


        if (root.hasNonNull("height_meter")) {
            double meters = root.get("height_meter").asDouble();
            p.setHeightCm((int) Math.round(meters * 100.0));
        }
        if (root.hasNonNull("weight_kilogram")) p.setWeightKg(root.get("weight_kilogram").asDouble());


        p.setBodyMeasurementJson(root.toString());
        patientRepository.save(p);
        return 1;
    }


    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static OffsetDateTime parseTs(JsonNode n, String... keys) {
        for (var k : keys)
            if (n.hasNonNull(k)) {
                try {
                    return OffsetDateTime.parse(n.get(k).asText());
                } catch (Exception ignored) {
                }
            }
        return null;
    }


    private static String valStr(JsonNode n, String key) {
        return n != null && n.hasNonNull(key) ? n.get(key).asText() : null;
    }

    private static class TokenResp {
        public String access_token;
        public String refresh_token;
        public Long expires_in;
        public String token_type;
        public String scope;
    }
}

