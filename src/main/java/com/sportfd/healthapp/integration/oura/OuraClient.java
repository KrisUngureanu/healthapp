package com.sportfd.healthapp.integration.oura;

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
import org.springframework.web.util.UriComponentsBuilder;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;

import java.util.function.Consumer;

@Component

public class OuraClient implements ProviderClient {



    private final RestClient ouraRest;
    private final ConnectionRepository connections;
    private final PatientRepository patientRepository;
    private final OuraSleepRepository ouraSleepRepository;
    private final OuraDaylySleepRepository ouraDaylySleepRepository;
    private final OuraHeartRateRepository ouraHeartRateRepository;
    private final OuraSpoRepository ouraSpoRepository;
    private final OuraReadinessRepository ouraReadinessRepository;
    private final OuraActivityRepository ouraActivityRepository;
    public OuraClient(@Qualifier("ouraRestClient") RestClient ouraRest, ConnectionRepository connections, PatientRepository patientRepository, OuraSleepRepository ouraSleepRepository, OuraDaylySleepRepository ouraDaylySleepRepository, OuraHeartRateRepository ouraHeartRateRepository, OuraSpoRepository ouraSpoRepository, OuraReadinessRepository ouraReadinessRepository, OuraActivityRepository ouraActivityRepository) {
        this.ouraRest = ouraRest;

        this.connections = connections;

        this.patientRepository = patientRepository;
        this.ouraSleepRepository = ouraSleepRepository;
        this.ouraDaylySleepRepository = ouraDaylySleepRepository;
        this.ouraHeartRateRepository = ouraHeartRateRepository;
        this.ouraSpoRepository = ouraSpoRepository;
        this.ouraReadinessRepository = ouraReadinessRepository;
        this.ouraActivityRepository = ouraActivityRepository;


    }




    @Value("${app.oura.client-id}")     private String clientId;
    @Value("${app.oura.client-secret}") private String clientSecret;
    @Value("${app.oura.redirect-uri}")  private String redirectUri;
    private static final String OURA_API_BASE = "https://api.ouraring.com";
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





    @Override
    public int syncReadinessDaily(Long pid, OffsetDateTime from, OffsetDateTime to) {
        String base = UriComponentsBuilder.fromPath("/v2/usercollection/daily_activity")
                .queryParam("start_date", from.toLocalDate().toString())
                .queryParam("end_date",   to.toLocalDate().toString())
                .build().encode().toUriString();
        Connection c = requireConn(pid);
        return paged(base, from, to, c, node -> {
            var data = node.path("data");
            for (JsonNode record : data) {
                String record_id = record.path("id").asText();
                JsonNode contributors = record.path("contributors");
                int active_calories = record.path("active_calories").asInt();
                float average_met_minutes = (float) record.path("average_met_minutes").asDouble(0d);
                int meet_daily_targets = contributors.path("meet_daily_targets").asInt();
                int move_every_hour = contributors.path("move_every_hour").asInt();
                int recovery_time = contributors.path("recovery_time").asInt();
                int stay_active = contributors.path("stay_active").asInt();
                int training_frequency = contributors.path("training_frequency").asInt();
                int training_volume = contributors.path("training_volume").asInt();

                String day = record.path("day").asText();
                int equivalent_walking_distance = record.path("equivalent_walking_distance").asInt();
                int high_activity_met_minutes = record.path("high_activity_met_minutes").asInt();
                int high_activity_time = record.path("high_activity_time").asInt();
                int inactivity_alerts = record.path("inactivity_alerts").asInt();
                int low_activity_met_minutes = record.path("low_activity_met_minutes").asInt();
                int low_activity_time = record.path("low_activity_time").asInt();
                int medium_activity_met_minutes = record.path("medium_activity_met_minutes").asInt();
                int medium_activity_time = record.path("medium_activity_time").asInt();

                int meters_to_target = record.path("meters_to_target").asInt();
                int non_wear_time = record.path("non_wear_time").asInt();
                int resting_time = record.path("resting_time").asInt();
                int score = record.path("score").asInt();
                int sedentary_met_minutes = record.path("sedentary_met_minutes").asInt();
                int sedentary_time = record.path("sedentary_time").asInt();
                int steps = record.path("steps").asInt();
                int target_calories = record.path("target_calories").asInt();
                int target_meters = record.path("target_meters").asInt();

                int total_calories = record.path("total_calories").asInt();

                OffsetDateTime timeRecord = OffsetDateTime.parse(record.path("timestamp").asText());
                var existingOpt = ouraActivityRepository.findByRecordId(record_id);
                OuraActivity ouraActivity = existingOpt.orElseGet(OuraActivity::new);
                ouraActivity.setRecord_id(record_id);
                ouraActivity.setActive_calories(active_calories);
                ouraActivity.setAverage_met_minutes(average_met_minutes);
                ouraActivity.setMeet_daily_targets(meet_daily_targets);
                ouraActivity.setMove_every_hour(move_every_hour);
                ouraActivity.setRecovery_time(recovery_time);
                ouraActivity.setStay_active(stay_active);
                ouraActivity.setTraining_frequency(training_frequency);
                ouraActivity.setTraining_volume(training_volume);

                ouraActivity.setDay(day);
                ouraActivity.setEquivalent_walking_distance(equivalent_walking_distance);
                ouraActivity.setHigh_activity_met_minutes(high_activity_met_minutes);
                ouraActivity.setHigh_activity_time(high_activity_time);
                ouraActivity.setInactivity_alerts(inactivity_alerts);
                ouraActivity.setLow_activity_met_minutes(low_activity_met_minutes);
                ouraActivity.setLow_activity_time(low_activity_time);
                ouraActivity.setMedium_activity_met_minutes(medium_activity_met_minutes);
                ouraActivity.setMedium_activity_time(medium_activity_time);

                ouraActivity.setMeters_to_target(meters_to_target);
                ouraActivity.setNon_wear_time(non_wear_time);
                ouraActivity.setResting_time(resting_time);
                ouraActivity.setScore(score);
                ouraActivity.setSedentary_met_minutes(sedentary_met_minutes);
                ouraActivity.setSedentary_time(sedentary_time);
                ouraActivity.setSteps(steps);
                ouraActivity.setTarget_calories(target_calories);
                ouraActivity.setTarget_meters(target_meters);

                ouraActivity.setTotal_calories(total_calories);
                ouraActivity.setTimeRecord(timeRecord);
                ouraActivity.setPatient_id(pid);
                ouraActivityRepository.save(ouraActivity);


            }
        });
    }




    @Override
    public int syncActivityDaily(Long pid, OffsetDateTime from, OffsetDateTime to) {
        String base = UriComponentsBuilder.fromPath("/v2/usercollection/daily_readiness")
                .queryParam("start_date", from.toLocalDate().toString())
                .queryParam("end_date",   to.toLocalDate().toString())
                .build().encode().toUriString();
        Connection c = requireConn(pid);
        return paged(base, from, to, c, node -> {
            var data = node.path("data");
            for (JsonNode record : data) {
                String record_id = record.path("id").asText();
                JsonNode contributors = record.path("contributors");
                int activity_balance = contributors.path("activity_balance").asInt();
                int body_temperature= contributors.path("body_temperature").asInt();
                int hrv_balance= contributors.path("hrv_balance").asInt();
                int previous_day_activity= contributors.path("previous_day_activity").asInt();
                int previous_night= contributors.path("previous_night").asInt();
                int recovery_index= contributors.path("recovery_index").asInt();
                int resting_heart_rate= contributors.path("resting_heart_rate").asInt();
                int sleep_balance= contributors.path("sleep_balance").asInt();
                int sleep_regularity= contributors.path("sleep_regularity").asInt();
                String day = record.path("day").asText(null);
                int score= record.path("activity_balance").asInt();
                OffsetDateTime timeRecord = OffsetDateTime.parse(record.path("timestamp").asText());


                var existingOpt = ouraReadinessRepository.findByRecordId(record_id);
                OuraReadiness ouraReadiness = existingOpt.orElseGet(OuraReadiness::new);
                ouraReadiness.setRecord_id(record_id);

                ouraReadiness.setActivity_balance(activity_balance);
                ouraReadiness.setBody_temperature(body_temperature);
                ouraReadiness.setHrv_balance(hrv_balance);
                ouraReadiness.setPrevious_day_activity(previous_day_activity);
                ouraReadiness.setPrevious_night(previous_night);
                ouraReadiness.setRecovery_index(recovery_index);
                ouraReadiness.setResting_heart_rate(resting_heart_rate);
                ouraReadiness.setSleep_balance(sleep_balance);
                ouraReadiness.setSleep_regularity(sleep_regularity);
                ouraReadiness.setDay(day);
                ouraReadiness.setScore(score);
                ouraReadiness.setTimeRecord(timeRecord);
                ouraReadiness.setPatient_id(pid);
                ouraReadinessRepository.save(ouraReadiness);
            }
        });
    }

    @Override
    public void syncSleep(Long pid, OffsetDateTime from, OffsetDateTime to) {
        String base = UriComponentsBuilder.fromPath("/v2/usercollection/sleep")
                .queryParam("start_date", from.toLocalDate().toString())
                .queryParam("end_date",   to.toLocalDate().toString())
                .build().encode().toUriString();
        Connection c = requireConn(pid);
        paged(base, from, to, c, node -> {
            var data = node.path("data");
            for (JsonNode record : data) {
                String record_id = record.path("id").asText();
                float average_breath = (float) record.path("average_breath").asDouble(0d);
                float average_heart_rate = (float) record.path("average_heart_rate").asDouble(0d);
                int average_hrv = record.path("average_hrv").asInt();
                int awake_time = record.path("awake_time").asInt();
                OffsetDateTime bedtime_end = OffsetDateTime.parse(record.path("bedtime_end").asText());
                OffsetDateTime bedtime_start = OffsetDateTime.parse(record.path("bedtime_start").asText());
                String day = record.path("day").asText();
                int deep_sleep_duration = record.path("deep_sleep_duration").asInt();
                int efficiency = record.path("efficiency").asInt();
                int latency = record.path("latency").asInt();
                int light_sleep_duration = record.path("light_sleep_duration").asInt();
                int lowest_heart_rate = record.path("lowest_heart_rate").asInt();
                int activity_balance = record.path("activity_balance").asInt();
                int body_temperature = record.path("body_temperature").asInt();
                int hrv_balance = record.path("hrv_balance").asInt();
                int previous_day_activity = record.path("previous_day_activity").asInt();
                int previous_night = record.path("previous_night").asInt();
                int recovery_index = record.path("recovery_index").asInt();
                int resting_heart_rate = record.path("resting_heart_rate").asInt();
                int sleep_balance = record.path("sleep_balance").asInt();
                int score = record.path("score").asInt();
                float temperature_deviation = (float) record.path("temperature_deviation").asDouble(0d);
                int readiness_score_delta = record.path("readiness_score_delta").asInt();
                int rem_sleep_duration = record.path("rem_sleep_duration").asInt();
                int restless_periods = record.path("restless_periods").asInt();
                int time_in_bed = record.path("time_in_bed").asInt();
                int total_sleep_duration = record.path("total_sleep_duration").asInt();
                String type = record.path("type").asText();

                var existingOpt = ouraSleepRepository.findByRecordId(record_id);
                OuraSleep ouraSleep = existingOpt.orElseGet(OuraSleep::new);
                ouraSleep.setRecord_id(record_id);
                ouraSleep.setAverage_breath(average_breath);
                ouraSleep.setAverage_heart_rate(average_heart_rate);
                ouraSleep.setAverage_hrv(average_hrv);
                ouraSleep.setAwake_time(awake_time);
                ouraSleep.setBedtime_end(bedtime_end);
                ouraSleep.setBedtime_start(bedtime_start);
                ouraSleep.setDay(day);
                ouraSleep.setDeep_sleep_duration(deep_sleep_duration);
                ouraSleep.setEfficiency(efficiency);
                ouraSleep.setLatency(latency);
                ouraSleep.setLight_sleep_duration(light_sleep_duration);
                ouraSleep.setLowest_heart_rate(lowest_heart_rate);
                ouraSleep.setActivity_balance(activity_balance);
                ouraSleep.setBody_temperature(body_temperature);
                ouraSleep.setHrv_balance(hrv_balance);
                ouraSleep.setPrevious_day_activity(previous_day_activity);
                ouraSleep.setPrevious_night(previous_night);
                ouraSleep.setRecovery_index(recovery_index);
                ouraSleep.setResting_heart_rate(resting_heart_rate);
                ouraSleep.setSleep_balance(sleep_balance);
                ouraSleep.setScore(score);
                ouraSleep.setTemperature_deviation(temperature_deviation);
                ouraSleep.setReadiness_score_delta(readiness_score_delta);
                ouraSleep.setRem_sleep_duration(rem_sleep_duration);
                ouraSleep.setRestless_periods(restless_periods);
                ouraSleep.setTime_in_bed(time_in_bed);
                ouraSleep.setTotal_sleep_duration(total_sleep_duration);
                ouraSleep.setType(type);
                ouraSleep.setPatient_id(pid);
                ouraSleepRepository.save(ouraSleep);
            }
        });
    }




    @Override
    public void syncAll(Long pid, OffsetDateTime from, OffsetDateTime to) {
        System.out.println("nachaloooo 1");
            syncHeartRate(pid, from, to);
        System.out.println("nachaloooo 2");
            syncSleep(pid, from, to);
        System.out.println("nachaloooo 3");
            syncSleepSessions(pid, from, to);
        System.out.println("nachaloooo 4");
            syncSpO2(pid, from, to);
        System.out.println("nachaloooo 5");
            syncReadinessDaily(pid, from, to);
        System.out.println("nachaloooo 6");
            syncActivityDaily(pid, from, to);
        System.out.println("nachaloooo 7");
    }



    @Override
    @Transactional
    public int syncSleepSessions(Long pid, OffsetDateTime from, OffsetDateTime to) {
        String base = UriComponentsBuilder.fromPath("/v2/usercollection/daily_sleep")
                .queryParam("start_date", from.toLocalDate().toString())
                .queryParam("end_date",   to.toLocalDate().toString())
                .build().encode().toUriString();
        Connection c = requireConn(pid);
        return paged(base, from, to, c, node -> {
            var data = node.path("data");
            for (JsonNode record : data) {
                String record_id = record.path("id").asText();
                JsonNode contributors = record.path("contributors");
                int deep_sleep = contributors.path("deep_sleep").asInt();
                int efficiency = contributors.path("efficiency").asInt();
                int latency = contributors.path("latency").asInt();
                int rem_sleep = contributors.path("rem_sleep").asInt();
                int restfulness = contributors.path("restfulness").asInt();
                int timing = contributors.path("timing").asInt();
                int total_sleep = contributors.path("total_sleep").asInt();
                String day = record.path("day").asText();
                int score = record.path("score").asInt();


                OffsetDateTime timestamp = null;
                if (record.hasNonNull("timestamp")) {
                    timestamp = OffsetDateTime.parse(record.path("timestamp").asText());
                }
                var existingOpt = ouraDaylySleepRepository.findByRecordId(record_id);
                OuraDaylySleep ouraSleep = existingOpt.orElseGet(OuraDaylySleep::new);
                ouraSleep.setRecord_id(record_id);
                ouraSleep.setDeep_sleep(deep_sleep);
                ouraSleep.setEfficiency(efficiency);
                ouraSleep.setLatency(latency);
                ouraSleep.setRem_sleep(rem_sleep);
                ouraSleep.setRestfulness(restfulness);
                ouraSleep.setTiming(timing);
                ouraSleep.setTotal_sleep(total_sleep);
                ouraSleep.setDay(day);
                ouraSleep.setScore(score);
                ouraSleep.setTimeRecord(timestamp);
                ouraSleep.setPatient_id(pid);
                ouraDaylySleepRepository.save(ouraSleep);

            }
        });
    }



    @Override
    public int syncHeartRate(Long pid, OffsetDateTime from, OffsetDateTime to) {
        String base = UriComponentsBuilder.fromPath("/v2/usercollection/heartrate")
                .queryParam("start_date", from.toLocalDate().toString())
                .queryParam("end_date",   to.toLocalDate().toString())
                .build().encode().toUriString();
        Connection c = requireConn(pid);
        return paged(base, from, to, c, node -> {
            var data = node.path("data");
            for (JsonNode record : data) {
                int bpm = record.path("bpm").asInt();
                String source = record.path("source").asText();
                OffsetDateTime timestamp = null;
                if (record.hasNonNull("timestamp")) {
                    timestamp = OffsetDateTime.parse(record.path("timestamp").asText());
                }

                OuraHeartRate ouraHeartRate = new OuraHeartRate();
                ouraHeartRate.setBpm(bpm);
                ouraHeartRate.setSource(source);
                ouraHeartRate.setTimeRecord(timestamp);
                ouraHeartRate.setPatient_id(pid);
                ouraHeartRateRepository.save(ouraHeartRate);


            }
        });
    }

    @Override
    public int syncSpO2(Long pid, OffsetDateTime from, OffsetDateTime to) {
        String base = UriComponentsBuilder.fromPath("/v2/usercollection/daily_spo2")
                .queryParam("start_date", from.toLocalDate().toString())
                .queryParam("end_date",   to.toLocalDate().toString())
                .build().encode().toUriString();
        Connection c = requireConn(pid);
        return paged(base, from, to, c, node -> {
            var data = node.path("data");
            for (JsonNode record : data) {
                String record_id = record.path("id").asText();
                int breathing_disturbance_index = record.path("breathing_disturbance_index").asInt();
                String day = record.path("day").asText();
                JsonNode spo2_percentage = record.path("spo2_percentage");
                float average = (float) spo2_percentage.path("average").asDouble(0d);

                var existingOpt = ouraSpoRepository.findByRecordId(record_id);
                OuraSpo ouraSpo = existingOpt.orElseGet(OuraSpo::new);
                ouraSpo.setRecord_id(record_id);
                ouraSpo.setBreathing_disturbance_index(breathing_disturbance_index);
                ouraSpo.setDay(day);
                ouraSpo.setSpo2_percentage(average);

                ouraSpoRepository.save(ouraSpo);
            }
        });
    }


    @Override
    public void syncProfile(Long patientId) {
        Connection c = requireConn(patientId);
        JsonNode root = getJson("/v2/usercollection/personal_info", c.getAccessToken());


        Patient p = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalStateException("Patient not found " + patientId));


        if (root.hasNonNull("date_of_birth")) {
            try { p.setBirthDate(LocalDate.parse(root.get("date_of_birth").asText())); } catch (Exception ignored) {}
        }
        if (root.hasNonNull("biological_sex")) {
            p.setSex(root.get("biological_sex").asText()); // male/female/unknown
        }

        if (root.hasNonNull("height_cm")) p.setHeightCm(root.get("height_cm").asInt());
        if (root.hasNonNull("weight_kg")) p.setWeightKg(root.get("weight_kg").asDouble());

        if (root.isObject()) p.setProfileJson(root.toString());
        patientRepository.save(p);

    }




    private int paged(String basePath, OffsetDateTime from, OffsetDateTime to,
                      Connection c, Consumer<JsonNode> pageConsumer) {
        int pages = 0;
        String next = null;
        String token = c.getAccessToken();

        do {
            var b = org.springframework.web.util.UriComponentsBuilder
                    .fromHttpUrl(OURA_API_BASE + basePath)
                    // для sleep — именно start_date / end_date (YYYY-MM-DD)
                    .queryParam("start_date", from.toLocalDate().toString())
                    .queryParam("end_date",   to.toLocalDate().toString())
                    .queryParam("limit", 25);

            if (next != null && !next.isBlank()) {
                b.queryParam("next_token", next);   // snake_case!
            }

            String url = b.build(true).toUriString();



            JsonNode root = getJson(url, token);
            if (root == null) break;

            pageConsumer.accept(root);

            next = (root.hasNonNull("next_token")) ? root.get("next_token").asText() : null;
            pages++;
        } while (next != null && !next.isBlank());

        return pages;
    }


    private static class TokenResp {
        public String access_token;
        public String refresh_token;
        public Long expires_in;
        public String token_type;
        public String scope;
    }
}