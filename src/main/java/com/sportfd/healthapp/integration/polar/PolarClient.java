package com.sportfd.healthapp.integration.polar;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportfd.healthapp.dto.polar.*;
import com.sportfd.healthapp.integration.ProviderClient;
import com.sportfd.healthapp.model.*;
import com.sportfd.healthapp.model.enums.Provider;
import com.sportfd.healthapp.repo.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import static com.sportfd.healthapp.util.TimeUtil.parseFlexible;
import static com.sportfd.healthapp.util.TimeUtil.parseFlexibleAdditional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class PolarClient implements ProviderClient {

    private static final String POLAR_AUTH_BASE = "https://flow.polar.com/oauth2/authorization";
    private static final String POLAR_TOKEN_URL = "https://polarremote.com/v2/oauth2/token";
    private static final String DEFAULT_SCOPE = "accesslink.read_all";
    private final ObjectMapper om = new ObjectMapper();
    @Qualifier("polarRestClient")
    private final RestClient polar; // можно использовать для API Polar (не для авторизации)
    private final ConnectionRepository connections;
    private final PatientRepository patientRepository;
    @Qualifier("polarAccessLink")
    private final RestClient accessLink;
    @Value("${app.polar.client-id}")
    private String clientId;
    @Value("${app.polar.client-secret}")
    private String clientSecret;
    @Value("${app.polar.redirect-uri}")
    private String redirectUri;
    private final PolarSleepRepository polarSleepRepository;
    private final PolarActivitiesRepository polarActivitiesRepository;
    private final PolarCardioRepository polarCardioRepository;
    private final PolarExercisesRepository polarExercisesRepository;
    private final PolarHeartRateRepository polarHeartRateRepository;
    private final PolarNightRechargeRepository polarNightRechargeRepository;
    private final PolarSpoRepository polarSpoRepository;
    private final PolarTemperatureRepository polarTemperatureRepository;
    private final PolarTestECGRepository polarTestECGRepository;
    private final PolarUserInfoRepository polarUserInfoRepository;
    private final PolarTemperatureSampleRepository polarTemperatureSampleRepository;
    private final PolarHypnogramRepository polarHypnogramRepository;
    private final PolarHeartRateSamplesSleepRepository polarHeartRateSamplesSleepRepository;


    public PolarClient(@Qualifier("polarRestClient") RestClient polar, @Qualifier("polarAccessLink") RestClient accessLink,
                       ConnectionRepository connections,
                       PatientRepository patientRepository, PolarSleepRepository polarSleepRepository, PolarActivitiesRepository polarActivitiesRepository, PolarCardioRepository polarCardioRepository, PolarExercisesRepository polarExercisesRepository, PolarHeartRateRepository polarHeartRateRepository, PolarNightRechargeRepository polarNightRechargeRepository, PolarSpoRepository polarSpoRepository, PolarTemperatureRepository polarTemperatureRepository, PolarTestECGRepository polarTestECGRepository, PolarUserInfoRepository polarUserInfoRepository, PolarTemperatureSampleRepository polarTemperatureSampleRepository, PolarHypnogramRepository polarHypnogramRepository, PolarHeartRateSamplesSleepRepository polarHeartRateSamplesSleepRepository) {
        this.polar = polar;
        this.accessLink = accessLink;
        this.connections = connections;
        this.patientRepository = patientRepository;
        this.polarSleepRepository = polarSleepRepository;
        this.polarActivitiesRepository = polarActivitiesRepository;
        this.polarCardioRepository = polarCardioRepository;
        this.polarExercisesRepository = polarExercisesRepository;
        this.polarHeartRateRepository = polarHeartRateRepository;
        this.polarNightRechargeRepository = polarNightRechargeRepository;
        this.polarSpoRepository = polarSpoRepository;
        this.polarTemperatureRepository = polarTemperatureRepository;
        this.polarTestECGRepository = polarTestECGRepository;
        this.polarUserInfoRepository = polarUserInfoRepository;
        this.polarTemperatureSampleRepository = polarTemperatureSampleRepository;
        this.polarHypnogramRepository = polarHypnogramRepository;

        this.polarHeartRateSamplesSleepRepository = polarHeartRateSamplesSleepRepository;
    }

    private record TempPoint(OffsetDateTime ts, Float value, String unit) {
    }

    private record HymnoPoint(String time, long value) {
    }


    @Override
    public Provider provider() {
        return Provider.POLAR;
    }

    private static String stageName(long id) {
        return switch ((int) id) {
            case 0 -> "AWAKE";
            case 1 -> "LIGHT";
            case 2 -> "DEEP";
            case 3 -> "REM";
            case 4 -> "UNKNOWN";
            default -> "UNKNOWN";
        };
    }

    @Override
    public String buildAuthorizeUrl(String state, String scopes, String redirect) {
        String ru = (redirect != null && !redirect.isBlank()) ? redirect : redirectUri;


        return UriComponentsBuilder
                .fromHttpUrl(POLAR_AUTH_BASE)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", ru)


                .queryParam("state", Objects.toString(state, ""))
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    @Override
    @Transactional
    public void exchangeCodeAndSave(Long patientId, String code) {
        String basic = "Basic " + Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

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

            throw new RuntimeException("Polar token exchange failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        }

        if (token == null || token.access_token == null) {
            throw new RuntimeException("Polar token exchange returned empty token");
        }


        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));
        patient.setStatus("active");
        patient.setDevice("polar");


        // Сохраним/обновим токены в нашей таблице connections
        upsertTokens(patientId, token);
    }


    private String requireToken(Long pid) {
        Connection c = connections.findByPatientIdAndProvider(pid, Provider.POLAR)
                .orElseThrow(() -> new IllegalStateException("Polar not connected for patient " + pid));
        String token = c.getAccessToken();
        if (token == null || token.isBlank())
            throw new IllegalStateException("Polar access token is null for patient " + pid);
        return token;
    }

    private static Long parsePolarUserId(String polarUserUrl) {
        if (polarUserUrl == null || polarUserUrl.isBlank()) return null;
        int i = polarUserUrl.lastIndexOf('/');
        try {
            return Long.parseLong(polarUserUrl.substring(i + 1));
        } catch (Exception e) {
            return null;
        }
    }

    private void upsertHeartRateSample(Long pid, String date, Long polarUserId,
                                       String sampleTime, int bpm) {

        PolarHeartRate existing = polarHeartRateRepository.findByPatientIdAndDateAndSampleTime(pid, LocalDate.parse(date), LocalTime.parse(sampleTime));
        if (existing == null) {
            existing = new PolarHeartRate();
            existing.setPatientId(pid);
            existing.setDate(LocalDate.parse(date));
            existing.setSampleTime(LocalTime.parse(sampleTime));
            existing.setBpm(bpm);
            existing.setPolarUserId(polarUserId);

            polarHeartRateRepository.save(existing);
        }


    }

    public List<PolarSleepAvailable.Item> sleepAvailable(Long pid) {
        String token = requireToken(pid);

        ResponseEntity<PolarSleepAvailable> resp;
        try {
            resp = RestClient.create()
                    .get()
                    .uri("https://www.polaraccesslink.com/v3/users/sleep/available")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(PolarSleepAvailable.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new IllegalStateException("Polar: consents missing (403) — ask user to re-authorize.");
            }
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new IllegalStateException("Polar: token invalid/expired (401) — reconnect Polar.");
            }
            throw new IllegalStateException("Polar error " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        }
        PolarSleepAvailable body = resp.getBody();
        return body == null ? List.of() : body.available();
    }

    public PolarSleepDto sleepByDate(Long pid, LocalDate date) {
        String token = requireToken(pid);
        ResponseEntity<PolarSleepDto> resp;
        try {
            resp = RestClient.create()
                    .get()
                    .uri("https://www.polaraccesslink.com/v3/users/sleep/{date}", date.toString())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(PolarSleepDto.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null; // на эту дату нет данных
            }
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new IllegalStateException("Polar: consents missing (403).");
            }
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new IllegalStateException("Polar: token invalid/expired (401).");
            }
            throw new IllegalStateException("Polar error " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        }
        return resp.getBody();
    }

    @Override
    public void syncSleep(Long pid, OffsetDateTime from, OffsetDateTime to) {
        LocalDate fromD = from.toLocalDate();
        LocalDate toD = to.toLocalDate();

        List<PolarSleepAvailable.Item> available = sleepAvailable(pid);
        List<LocalDate> dates = available.stream()
                .map(PolarSleepAvailable.Item::date)
                .filter(Objects::nonNull)
                .map(LocalDate::parse)
                .filter(d -> !d.isBefore(fromD) && !d.isAfter(toD))
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        for (LocalDate d : dates) {
            PolarSleepDto dto = sleepByDate(pid, d);
            if (dto == null) continue;

            PolarSleep existing = polarSleepRepository.findByPatientIdAndDate(pid, d);
            if (existing == null) {
                PolarSleep ps = new PolarSleep();
                ps.setPatientId(pid);
                ps.setDate(dto.date());
                ps.setSleepStartTime(dto.sleepStartTime());
                ps.setSleepEndTime(dto.sleepEndTime());
                ps.setDeviceId(dto.deviceId());

                ps.setContinuity(dto.continuity());
                ps.setContinuityClass(dto.continuityClass());

                ps.setLightSleep(dto.lightSleep());
                ps.setDeepSleep(dto.deepSleep());
                ps.setRemSleep(dto.remSleep());
                ps.setUnrecognizedSleepStage(dto.unrecognizedSleepStage());

                ps.setSleepScore(dto.sleepScore());
                ps.setSleepGoal(dto.sleepGoal());
                ps.setSleepRating(dto.sleepRating());

                ps.setTotalInterruptionDuration(dto.totalInterruptionDuration());
                ps.setShortInterruptionDuration(dto.shortInterruptionDuration());
                ps.setLongInterruptionDuration(dto.longInterruptionDuration());

                ps.setSleepCycles(dto.sleepCycles());
                ps.setGroupDurationScore(dto.groupDurationScore());
                ps.setGroupSolidityScore(dto.groupSolidityScore());
                ps.setGroupRegenerationScore(dto.groupRegenerationScore());


                polarSleepRepository.save(ps);

                if (dto.hypnogram() != null && !dto.hypnogram().isNull()) {
                    List<HymnoPoint> hypno = extractPoints(dto.hypnogram());


                    List<PolarHypnogram> rows = new ArrayList<>(hypno.size());
                    for (HymnoPoint p : hypno) {

                        List<PolarHypnogram> hexist = polarHypnogramRepository.findAllByPatientIdAndSleepId(pid, ps.getId());
                        if (hexist == null) {
                            PolarHypnogram h = new PolarHypnogram();
                            h.setPatientId(pid);
                            h.setSleepId(ps.getId());
                            h.setSleepTime(p.time());
                            h.setTypeId(p.value());
                            h.setTypeName(stageName(p.value()));
                            h.setUserPolar(dto.polarUserUrl());
                            rows.add(h);
                        }

                    }
                    if (!rows.isEmpty()) polarHypnogramRepository.saveAll(rows);
                }


                if (dto.heartRateSamples() != null && !dto.heartRateSamples().isNull()) {
                    List<HymnoPoint> hr = extractPoints(dto.heartRateSamples());


                    List<PolarHeartRateSamplesSleep> rows = new ArrayList<>(hr.size());
                    for (HymnoPoint p : hr) {

                        List<PolarHeartRateSamplesSleep> rex = polarHeartRateSamplesSleepRepository.findAllByPatientIdAndSleepId(pid, ps.getId());
                        if (rex == null) {
                            PolarHeartRateSamplesSleep r = new PolarHeartRateSamplesSleep();
                            r.setPatientId(pid);
                            r.setSleepId(ps.getId());
                            r.setSleepTime(p.time());
                            r.setValueHr(p.value());
                            r.setUserPolar(dto.polarUserUrl());
                            rows.add(r);
                        }

                    }
                    if (!rows.isEmpty()) polarHeartRateSamplesSleepRepository.saveAll(rows);
                }
            }




        }


    }

    @Override
    public void syncActivityDaily(Long pid, OffsetDateTime from, OffsetDateTime to) {
        String token = requireToken(pid);
        Connection c = connections.findByPatientIdAndProvider(pid, Provider.POLAR)
                .orElseThrow(() -> new IllegalStateException("Polar not connected for patient " + pid));
        String userIdStr = c.getExternalUserId();
        ResponseEntity<PolarActivitiesDto> resp;
        LocalDate date = to.toLocalDate();
        String ds = date.toString();

        try {
            resp = RestClient.create()
                    .get()
                    .uri("https://www.polaraccesslink.com/v3/users/activities/{date}", ds)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(PolarActivitiesDto.class);
        } catch (HttpClientErrorException e) {

            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new IllegalStateException("Polar user " + userIdStr + " not found (404).");
            }
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new IllegalStateException("Polar user info: 403 (user consents missing).");
            }
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new IllegalStateException("Polar user info: 401 (invalid/expired token).");
            }
            throw new IllegalStateException("Polar user info error " + e.getStatusCode().value()
                    + ": " + e.getResponseBodyAsString());
        }

        PolarActivitiesDto dto = resp.getBody();

        if (dto == null) return;


        PolarActivities existing = polarActivitiesRepository.findByPatientIdAndStart_time(pid, from).orElseGet(PolarActivities::new);
        existing.setPatientId(pid);
        existing.setStart_time(parseFlexible(dto.start_time(), null));
        existing.setEnd_time(parseFlexible(dto.end_time(), null));
        existing.setActive_duration(dto.active_duration());
        existing.setInactive_duration(dto.inactive_duration());
        existing.setDaily_activity(dto.daily_activity());
        existing.setCalories(dto.calories());
        existing.setActive_calories(dto.active_calories());
        existing.setSteps(dto.steps());
        existing.setInactivity_alert_count(dto.inactivity_alert_count());
        existing.setDistance_from_steps(dto.distance_from_steps());

        polarActivitiesRepository.save(existing);

    }

    @Override
    public void syncWorkout(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        var conn = connections.findByPatientIdAndProvider(patientId, Provider.POLAR)
                .orElseThrow(() -> new IllegalStateException("Polar not connected for patient " + patientId));
        String token = Objects.requireNonNull(conn.getAccessToken(), "Polar access token is null");

        ResponseEntity<JsonNode> resp;
        try {
            resp = RestClient.create()
                    .get()
                    .uri("https://www.polaraccesslink.com/v3/exercises/")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(JsonNode.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN)
                throw new IllegalStateException("Polar exersises: 403 (user consents missing).");
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED)
                throw new IllegalStateException("Polar exersises: 401 (invalid/expired token).");
            throw new IllegalStateException("Polar exersises error " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        }

        JsonNode body = resp.getBody();
        if (body.isArray()) {
            for (JsonNode n : body) {
                saveExcersiseNode(patientId, n);
            }
        } else if (body.isObject()) {
            saveExcersiseNode(patientId, body);
        }
    }


    private void saveExcersiseNode(Long patientId, JsonNode n) {

        String recordId = n.get("id").asText();
        var existing = polarExercisesRepository
                .findByRecordId(recordId)
                .orElseGet(PolarExercises::new);
        String start = n.get("start_time").asText();
        existing.setPatientId(patientId);
        existing.setStart_time(parseFlexible(start, null));

        if (n.has("id")) existing.setRecord_id(n.get("id").asText(null));
        if (n.has("upload_time")) existing.setUpload_time(OffsetDateTime.parse(n.get("upload_time").asText()));
        if (n.has("device")) existing.setDevice(n.get("device").asText());
        if (n.has("start_time_utc_offset")) existing.setStart_time_utc_offset(n.get("start_time_utc_offset").asInt());
        if (n.has("duration")) existing.setDuration(n.get("duration").asText());
        if (n.has("distance")) existing.setDistance((float) n.get("distance").asDouble(0d));
        if (n.has("sport")) existing.setSport(n.get("sport").asText());
        if (n.has("has_route")) existing.setHas_route(n.get("has_route").asBoolean());
        if (n.has("detailed_sport_info")) existing.setDetailed_sport_info(n.get("detailed_sport_info").asText());


        JsonNode hr = n.path("heart_rate");
        if (hr.isObject()) {
            if (hr.hasNonNull("average")) existing.setAverage_heart_rate(hr.get("average").asInt());
            if (hr.hasNonNull("maximum")) existing.setMax_heart_rate(hr.get("maximum").asInt());
        }


        if (n.hasNonNull("calories")) existing.setCalories(n.get("calories").asInt());
        if (n.hasNonNull("fat_percentage")) existing.setFat_percentage(n.get("fat_percentage").asInt());
        if (n.hasNonNull("carbohydrate_percentage"))
            existing.setCarbohydrate_percentage(n.get("carbohydrate_percentage").asInt());
        if (n.hasNonNull("protein_percentage")) existing.setProtein_percentage(n.get("protein_percentage").asInt());


        existing.setPatientId(patientId);
        polarExercisesRepository.save(existing);
    }


    @Override
    public void syncCardio(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        var conn = connections.findByPatientIdAndProvider(patientId, Provider.POLAR)
                .orElseThrow(() -> new IllegalStateException("Polar not connected for patient " + patientId));
        String token = Objects.requireNonNull(conn.getAccessToken(), "Polar access token is null");

        LocalDate date = LocalDate.now();
        String ds = date.toString();
        String url = UriComponentsBuilder
                .fromHttpUrl("https://www.polaraccesslink.com/v3/users/cardio-load/date")
                .queryParam("from", ds)
                .queryParam("to", ds)
                .build(true)
                .toUriString();

        ResponseEntity<String> resp;
        try {
            resp = RestClient.create()
                    .get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NO_CONTENT || e.getStatusCode() == HttpStatus.NOT_FOUND) return;
            if (e.getStatusCode() == HttpStatus.FORBIDDEN)
                throw new IllegalStateException("Polar cardio: 403 (user consents missing).");
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED)
                throw new IllegalStateException("Polar cardio: 401 (invalid/expired token).");
            throw new IllegalStateException("Polar cardio error " + e.getStatusCode().value()
                    + ": " + e.getResponseBodyAsString());
        }

        String bodyStr = resp.getBody();
        if (bodyStr == null || bodyStr.isBlank()) return;

        try {
            JsonNode root = om.readTree(bodyStr);
            if (root.isArray()) {
                for (JsonNode n : root) saveCardioNode(patientId, n);
            } else if (root.isObject()) {
                saveCardioNode(patientId, root);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse Polar cardio JSON: " + ex.getMessage(), ex);
        }
    }

    private void saveCardioNode(Long patientId, JsonNode n) {
        String dateStr = n.get("date").asText();
        OffsetDateTime date = parseFlexibleAdditional(dateStr, null);


        var existing = polarCardioRepository
                .findByPatientIdAndDate(patientId, date)
                .orElseGet(PolarCardio::new);

        existing.setPatientId(patientId);
        existing.setDate(date);
        if (n.has("cardio_load_status")) existing.setCardio_load_status(n.get("cardio_load_status").asText());
        if (n.has("cardio_load_ratio")) existing.setCardio_load_ratio((float) n.get("cardio_load_ratio").asDouble(0d));
        if (n.has("cardio_load")) existing.setCardio_load((float) n.get("cardio_load").asDouble(0d));
        if (n.has("strain")) existing.setStrain((float) n.get("strain").asDouble(0d));
        if (n.has("tolerance")) existing.setTolerance((float) n.get("tolerance").asDouble(0d));
        existing.setPatientId(patientId);

        polarCardioRepository.save(existing);
    }


    @Override
    public void syncUserInfo(Long pid, OffsetDateTime from, OffsetDateTime to) {
        String token = requireToken(pid);
        Connection c = connections.findByPatientIdAndProvider(pid, Provider.POLAR)
                .orElseThrow(() -> new IllegalStateException("Polar not connected for patient " + pid));
        String userIdStr = c.getExternalUserId();
        ResponseEntity<PolarUserInfoDto> resp;
        try {
            resp = RestClient.create()
                    .get()
                    .uri("https://www.polaraccesslink.com/v3/users/{userId}", userIdStr)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(PolarUserInfoDto.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new IllegalStateException("Polar user " + userIdStr + " not found (404).");
            }
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new IllegalStateException("Polar user info: 403 (user consents missing).");
            }
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new IllegalStateException("Polar user info: 401 (invalid/expired token).");
            }
            throw new IllegalStateException("Polar user info error " + e.getStatusCode().value()
                    + ": " + e.getResponseBodyAsString());
        }

        PolarUserInfoDto dto = resp.getBody();
        if (dto == null) return;


        PolarUserInfo e = polarUserInfoRepository.findByPatientId(pid).orElseGet(PolarUserInfo::new);
        e.setPatientId(pid);


        e.setPolarUserId(Objects.requireNonNull(dto.polarUserId(), "polar-user-id is null"));

        e.setMemberId(dto.memberId());
        e.setRegistrationDate(dto.registrationDate());
        e.setFirstName(dto.firstName());
        e.setLastName(dto.lastName());
        e.setBirthdate(dto.birthdate());
        e.setGender(dto.gender());
        e.setWeightKg(dto.weight());
        e.setHeightCm(dto.height());

        polarUserInfoRepository.save(e);
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
        c.setExternalUserId("63391117");
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
        public Long expires_in;
        public String scope;
    }


    @Override
    public void syncAll(Long pid, OffsetDateTime from, OffsetDateTime to) {
        try {
            syncHeartRate(pid, from, to);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            syncSleep(pid, from, to);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            syncUserInfo(pid, from, to);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            syncTemperature(pid, from, to);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            syncNightRecharge(pid, from, to);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            syncCardio(pid, from, to);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            syncWorkout(pid, from, to);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            syncActivityDaily(pid, from, to);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            syncTestEcg(pid, from, to);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            syncSpO2(pid, from, to);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public void syncHeartRate(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        String token = requireToken(patientId);
        ResponseEntity<PolarHeartRateDto> resp;
        LocalDate date = from.toLocalDate();
        String ds = date.toString();
        try {
            resp = RestClient.create()
                    .get()
                    .uri("https://www.polaraccesslink.com/v3/users/continuous-heart-rate/{date}", ds)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(PolarHeartRateDto.class);
        } catch (HttpClientErrorException e) {

            if (e.getStatusCode() == HttpStatus.FORBIDDEN)
                throw new IllegalStateException("Polar CHR: 403 (user consents missing).");
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED)
                throw new IllegalStateException("Polar CHR: 401 (invalid/expired token).");
            throw new IllegalStateException("Polar CHR error " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        }

        PolarHeartRateDto body = resp.getBody();


        Long polarUserId = parsePolarUserId(body.polarUser());

        for (var s : body.heartRateSamples()) {
            if (s.sampleTime() == null) continue;
            upsertHeartRateSample(patientId, body.date(), polarUserId, s.sampleTime(), s.heartRate());

        }

    }

    @Override
    public void syncNightRecharge(Long pid, OffsetDateTime from, OffsetDateTime to) {
        String token = requireToken(pid);
        LocalDate date = from.toLocalDate().minusDays(1);
        ResponseEntity<PolarNightRechargeDto> resp = null;
        boolean isOk = true;
        try {
            resp = RestClient.create()
                    .get()
                    .uri("https://www.polaraccesslink.com/v3/users/nightly-recharge/{date}", date.toString())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(PolarNightRechargeDto.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN)
                throw new IllegalStateException("Polar Nightly Recharge: 403 (user consents missing).");
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED)
                throw new IllegalStateException("Polar Nightly Recharge: 401 (invalid/expired token).");
//            throw new IllegalStateException("Polar Nightly Recharge error " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
            isOk = false;
        }

        if (isOk) {
            var body = resp.getBody();


            OffsetDateTime dayUtc = parseFlexibleAdditional(date.toString(), null);

            var entity = polarNightRechargeRepository.findByPatientIdAndDate(pid, dayUtc)
                    .orElseGet(PolarNightRecharge::new);

            entity.setPatientId(pid);
            entity.setDate(dayUtc);
            if (body.heart_rate_avg() != null) entity.setHeart_rate_avg(body.heart_rate_avg());
            if (body.beat_to_beat_avg() != null) entity.setBeat_to_beat_avg(body.beat_to_beat_avg());
            if (body.heart_rate_variability_avg() != null)
                entity.setHeart_rate_variability_avg(body.heart_rate_variability_avg());
            if (body.breathing_rate_avg() != null) entity.setBreathing_rate_avg(body.breathing_rate_avg());

            polarNightRechargeRepository.save(entity);
        }


    }

    @Override
    public void syncSpO2(Long pid, OffsetDateTime from, OffsetDateTime to) {
        LocalDate fromD = from.toLocalDate();
        LocalDate toD = to.toLocalDate();
        if (toD.isBefore(fromD)) return;

        final int MAX_WINDOW = 28;
        String token = requireToken(pid);

        LocalDate cursor = fromD;
        while (!cursor.isAfter(toD)) {
            LocalDate wndEnd = cursor.plusDays(MAX_WINDOW - 1);
            if (wndEnd.isAfter(toD)) wndEnd = toD;

            String url = UriComponentsBuilder
                    .fromHttpUrl("https://www.polaraccesslink.com/v3/users/biosensing/spo2")
                    .queryParam("from", cursor.toString())
                    .queryParam("to", wndEnd.toString())
                    .build(true).toUriString();

            ResponseEntity<PolarSpoDto[]> resp;
            try {
                resp = RestClient.create()
                        .get()
                        .uri(url)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .toEntity(PolarSpoDto[].class);
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NO_CONTENT) {
                    cursor = wndEnd.plusDays(1);
                    continue;
                }
                if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                    throw new IllegalStateException("Polar SpO2: 403 (user consents missing).");
                }
                if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    throw new IllegalStateException("Polar SpO2: 401 (invalid/expired token).");
                }
                throw new IllegalStateException("Polar SpO2 error " + e.getStatusCode().value()
                        + ": " + e.getResponseBodyAsString());
            }

            PolarSpoDto[] items = resp.getBody();
            if (items != null) {
                for (PolarSpoDto d : items) {

                    var entity = polarSpoRepository.findByPatientIdAndTestTime(pid, d.test_time())
                            .orElseGet(PolarSpo::new);

                    entity.setPatientId(pid);
                    entity.setSource_device_id(d.source_device_id());
                    entity.setTest_time(d.test_time());                    // epoch seconds (UTC)
                    entity.setTime_zone_offset(d.time_zone_offset());      // минуты
                    entity.setTest_status(d.test_status());
                    entity.setBlood_oxygen_percent(d.blood_oxygen_percent());
                    entity.setSpo2_class(d.spo2_class());
                    entity.setSpo2_value_deviation_from_baseline(d.spo2_value_deviation_from_baseline());
                    entity.setSpo2_quality_average_percent(d.spo2_quality_average_percent());
                    entity.setAverage_heart_rate_bpm(d.average_heart_rate_bpm());
                    entity.setHeart_rate_variability_ms(d.heart_rate_variability_ms());
                    entity.setSpo2_hrv_deviation_from_baseline(d.spo2_hrv_deviation_from_baseline());
                    entity.setAltitude_meters(d.altitude_meters());
                    entity.setStart_time(parseFlexible(d.start_time(), null));                  // ISO-8601 (UTC по доке)
                    entity.setEnd_time(parseFlexible(d.end_time(), null));

                    polarSpoRepository.save(entity);
                }
            }

            cursor = wndEnd.plusDays(1);
        }
    }


    @Override
    public void syncTemperature(Long pid, OffsetDateTime from, OffsetDateTime to) {
        LocalDate fromD = from.toLocalDate();
        LocalDate toD = to.toLocalDate();
        if (toD.isBefore(fromD)) return;

        final int MAX_WINDOW = 28;
        String token = requireToken(pid);

        LocalDate cursor = fromD;
        while (!cursor.isAfter(toD)) {
            LocalDate wndEnd = cursor.plusDays(MAX_WINDOW - 1);
            if (wndEnd.isAfter(toD)) wndEnd = toD;

            String ds = cursor.toString();        // "YYYY-MM-DD"
            String df = wndEnd.toString();

            String url = UriComponentsBuilder
                    .fromHttpUrl("https://www.polaraccesslink.com/v3/users/biosensing/bodytemperature")
                    .queryParam("from", ds)
                    .queryParam("to", df)
                    .build(true).toUriString();

            ResponseEntity<PolarTemperatureDto[]> resp;
            try {
                resp = RestClient.create()
                        .get()
                        .uri(url)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .toEntity(PolarTemperatureDto[].class);
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NO_CONTENT) { // 204 — нет данных
                    cursor = wndEnd.plusDays(1);
                    continue;
                }
                if (e.getStatusCode() == HttpStatus.FORBIDDEN)
                    throw new IllegalStateException("Polar BodyTemperature: 403 (user consents missing).");
                if (e.getStatusCode() == HttpStatus.UNAUTHORIZED)
                    throw new IllegalStateException("Polar BodyTemperature: 401 (invalid/expired token).");
                throw new IllegalStateException("Polar BodyTemperature error " + e.getStatusCode().value()
                        + ": " + e.getResponseBodyAsString());
            }

            PolarTemperatureDto[] items = resp.getBody();
            if (items != null) {
                for (PolarTemperatureDto d : items) {
                    var startTs = parseFlexibleAdditional(d.startTime(), null); // твой гибкий парсер
                    PolarTemperature entity = polarTemperatureRepository
                            .findByPatientIdAndStart_time(pid, startTs);
                    if (entity == null){
                        entity = new PolarTemperature();
                        entity.setPatientId(pid);
                        entity.setSource_device_id(d.sourceDeviceId());
                        entity.setStartTime(startTs);
                        entity.setEndTime(parseFlexibleAdditional(d.endTime(), null));
                        entity.setMeasurement_type(d.measurementType());
                        entity.setSensor_location(d.sensorLocation());

                        if (d.samples() != null && !d.samples().isNull()) {

                            entity.setSamples(d.samples().toString());
                        }
                        polarTemperatureRepository.save(entity);

                        OffsetDateTime baseStart = parseFlexibleAdditional(d.startTime(), null);


                        var pts = extractTempSamplesDelta(d.samples(), baseStart, "C");


                        if (!pts.isEmpty()) {
                            List<PolarTemperatureSample> rows = new ArrayList<>(pts.size());
                            for (var p : pts) {
                                PolarTemperatureSample row = new PolarTemperatureSample();
                                row.setPatientId(pid);
                                row.setTemperatureId(entity.getId());
                                row.setSampleTime(p.ts());
                                row.setValue(p.value());
                                row.setUnit(p.unit());
                                rows.add(row);
                            }
                            polarTemperatureSampleRepository.saveAll(rows);
                        }
                    }

                }
            }

            cursor = wndEnd.plusDays(1);
        }
    }

    @Override
    public void syncTestEcg(Long pid, OffsetDateTime from, OffsetDateTime to) {
        LocalDate fromD = from.toLocalDate();
        LocalDate toD = to.toLocalDate();
        if (toD.isBefore(fromD)) return;

        final int MAX_WINDOW_DAYS = 28; // типичное ограничение окна у biosensing
        String token = requireToken(pid);

        LocalDate cursor = fromD;
        String ds = cursor.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        while (!cursor.isAfter(toD)) {
            LocalDate wndEnd = cursor.plusDays(MAX_WINDOW_DAYS - 1);
            if (wndEnd.isAfter(toD)) wndEnd = toD;
            String df = wndEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://www.polaraccesslink.com/v3/users/biosensing/ecg")
                    .queryParam("from", ds)
                    .queryParam("to", df)
                    .build(true)
                    .toUriString();

            ResponseEntity<PolarEcgDto[]> resp;
            try {
                resp = RestClient.create()
                        .get()
                        .uri(url)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .toEntity(PolarEcgDto[].class);
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NO_CONTENT) { // данных нет в окне
                    cursor = wndEnd.plusDays(1);
                    continue;
                }
                if (e.getStatusCode() == HttpStatus.FORBIDDEN)
                    throw new IllegalStateException("Polar ECG: 403 (user consents missing).");
                if (e.getStatusCode() == HttpStatus.UNAUTHORIZED)
                    throw new IllegalStateException("Polar ECG: 401 (invalid/expired token).");
                throw new IllegalStateException("Polar ECG error " + e.getStatusCode().value()
                        + ": " + e.getResponseBodyAsString());
            }

            var items = resp.getBody();
            if (items != null) {
                for (PolarEcgDto d : items) {
                    // upsert по patient_id + test_time (epoch seconds)
                    var entity = polarTestECGRepository
                            .findByPatientIdAndTestTime(pid, d.test_time())
                            .orElseGet(PolarTestECG::new);

                    entity.setPatientId(pid);
                    entity.setSource_device_id(d.source_device_id());
                    entity.setTest_time(d.test_time());
                    entity.setTime_zone_offset(d.time_zone_offset());
                    entity.setAverage_heart_rate_bpm(d.average_heart_rate_bpm());
                    entity.setHeart_rate_variability_ms(d.heart_rate_variability_ms());
                    entity.setHeart_rate_variability_level(d.heart_rate_variability_level());
                    entity.setRri_ms(d.rri_ms());
                    entity.setPulse_transit_time_systolic_ms(d.pulse_transit_time_systolic_ms());
                    entity.setPulse_transit_time_diastolic_ms(d.pulse_transit_time_diastolic_ms());
                    entity.setPulse_transit_time_quality_index(d.pulse_transit_time_quality_index());
                    ZoneOffset offset = ZoneOffset.ofTotalSeconds(d.time_zone_offset() * 60);
                    entity.setStart_time(d.start_time().atOffset(offset));
                    entity.setEnd_time(d.end_time().atOffset(offset));

                    polarTestECGRepository.save(entity);
                }
            }

            cursor = wndEnd.plusDays(1);
        }
    }


    @Transactional
    public PolarRegisterResult registerUser(Long patientId) {
        var conn = connections.findByPatientIdAndProvider(patientId, Provider.POLAR)
                .orElseThrow(() -> new IllegalStateException("No Polar connection for patientId=" + patientId));

        String token = Objects.requireNonNull(conn.getAccessToken(), "Polar access token is null");

        // Можно использовать patientId или сохранённый x_user_id как member-id
        record RegisterBody(@JsonProperty("member-id") String memberId) {
        }
        RegisterBody body = new RegisterBody(String.valueOf(patientId));

        ResponseEntity<JsonNode> resp;
        try {
            resp = RestClient.create()
                    .post()
                    .uri("https://www.polaraccesslink.com/v3/users")       // Абсолютный URL!
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON) // JSON проще
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)                                            // Если получали 400 — это вылечит
                    .retrieve()
                    .toEntity(JsonNode.class);
        } catch (HttpClientErrorException e) {
            int code = e.getStatusCode().value();
            String err = e.getResponseBodyAsString();
            if (code == 409) return new PolarRegisterResult(PolarRegisterStatus.ALREADY_REGISTERED, null, err);
            if (code == 403) return new PolarRegisterResult(PolarRegisterStatus.FORBIDDEN, null, err);
            if (code == 401) return new PolarRegisterResult(PolarRegisterStatus.OTHER, null,
                    "401 (скорее не Polar). Проверьте, что URL именно polaraccesslink.com и Bearer-токен верный. Body: " + err);
            return new PolarRegisterResult(PolarRegisterStatus.OTHER, null, "Error " + code + ": " + err);
        }

        int code = resp.getStatusCode().value();
        if (code == 200) {

            long polarUserId = resp.getBody().path("polar-user-id").asLong(0);
            if (polarUserId > 0) conn.setExternalUserId(String.valueOf(polarUserId));
            connections.save(conn);
            return new PolarRegisterResult(PolarRegisterStatus.CREATED, null, null);
        }
        if (code == 409) return new PolarRegisterResult(PolarRegisterStatus.ALREADY_REGISTERED, null, null);
        if (code == 403) return new PolarRegisterResult(PolarRegisterStatus.FORBIDDEN, null,
                "User consents missing (403): пользователь не принял все обязательные согласия.");
        return new PolarRegisterResult(PolarRegisterStatus.OTHER, null, "Unexpected response: " + code);
    }

    public enum PolarRegisterStatus {CREATED, ALREADY_REGISTERED, FORBIDDEN, OTHER}

    private static List<TempPoint> extractTempSamplesDelta(
            JsonNode samplesNode,
            OffsetDateTime baseStart,   // обязателен для delta_ms
            String unit                 // "C"
    ) {
        ArrayList<TempPoint> out = new ArrayList<>();
        if (samplesNode == null || samplesNode.isNull() || !samplesNode.isArray()) return out;

        for (JsonNode it : samplesNode) {
            if (!it.hasNonNull("recording_time_delta_milliseconds")) continue;
            long deltaMs = it.get("recording_time_delta_milliseconds").asLong();
            Float val = it.hasNonNull("temperature_celsius") ? (float) it.get("temperature_celsius").asDouble() : null;

            if (baseStart != null && val != null) {
                OffsetDateTime ts = baseStart.plus(Duration.ofMillis(deltaMs));
                out.add(new TempPoint(ts, val, unit));
            }
        }
        return out;
    }


    private static List<HymnoPoint> extractPoints(JsonNode node) {
        List<HymnoPoint> out = new ArrayList<>();
        if (node == null || node.isNull()) return out;

        if (node.isObject()) {
            node.fields().forEachRemaining(e -> {
                String t = e.getKey();
                JsonNode v = e.getValue();
                if (t != null && v != null && v.isNumber()) {
                    out.add(new HymnoPoint(t, v.asLong()));
                }
            });
        } else if (node.isArray()) {
            for (JsonNode it : node) {
                if (it != null && it.isObject()) {
                    String t = it.path("time").asText(null);
                    JsonNode v = it.get("value");
                    if (t != null && v != null && v.isNumber()) {
                        out.add(new HymnoPoint(t, v.asLong()));
                    }
                }
            }
        }

        // сортировка по времени HH:mm
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        out.sort(Comparator.comparing(p -> LocalTime.parse(p.time(), fmt)));
        return out;
    }


    private static OffsetDateTime parseSampleTime(JsonNode n, Integer offsetMinutes) {
        // "time": string ISO / без смещения / с Z
        if (n.hasNonNull("time")) {
            return parseFlexibleStringTs(n.get("time").asText(), offsetMinutes);
        }
        // "timestamp": число epoch (сек/мс)
        if (n.hasNonNull("timestamp")) {
            JsonNode t = n.get("timestamp");
            if (t.isNumber()) {
                long epoch = t.asLong();
                // эвристика: > 10^12 → миллисекунды
                if (epoch > 1_000_000_000_000L) epoch = epoch / 1000;
                return OffsetDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneOffset.UTC);
            }
            if (t.isTextual()) {
                return parseFlexibleStringTs(t.asText(), offsetMinutes);
            }
        }
        return null;
    }

    private static OffsetDateTime parseFlexibleStringTs(String text, Integer offsetMinutes) {
        if (text == null || text.isBlank()) return null;
        // 1) пробуем как OffsetDateTime (есть Z/±HH:MM)
        try {
            return OffsetDateTime.parse(text);
        } catch (Exception ignored) {
        }
        // 2) как LocalDateTime без смещения
        try {
            LocalDateTime ldt = LocalDateTime.parse(text);
            ZoneOffset off = offsetMinutes != null ? ZoneOffset.ofTotalSeconds(offsetMinutes * 60) : ZoneOffset.UTC;
            return ldt.atOffset(off);
        } catch (Exception ignored) {
        }
        // 3) чистая дата
        try {
            LocalDate d = LocalDate.parse(text);
            return d.atStartOfDay().atOffset(ZoneOffset.UTC);
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    @Transactional
    public void deleteUser(Long pid, boolean alsoDisconnectLocally) {
        String token = requireToken(pid);
        String userId = requireExternalUserId(pid);

        ResponseEntity<Void> resp = null;
        try {
            resp = RestClient.create()
                    .delete()
                    .uri("https://www.polaraccesslink.com/v3/users/{userId}", userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            System.out.println("KRIS_ERR " + e);
        }


        if (resp != null && resp.getStatusCode().is2xxSuccessful()) {

            if (alsoDisconnectLocally) {
                connections.findByPatientIdAndProvider(pid, Provider.POLAR).ifPresent(c -> {
                    c.setExternalUserId(null);
                    c.setAccessToken(null);
                    c.setRefreshToken(null);
                    c.setExpiresAt(null);
                    connections.save(c);
                });


                connections.deleteByPatientIdAndProvider(pid, Provider.POLAR);
                polarHeartRateRepository.deleteByPatientId(pid);
                polarNightRechargeRepository.deleteByPatientId(pid);
                polarSpoRepository.deleteByPatientId(pid);
                polarTemperatureRepository.deleteByPatientId(pid);
                polarTemperatureSampleRepository.deleteByPatientId(pid);
                polarTestECGRepository.deleteByPatientId(pid);
                polarActivitiesRepository.deleteByPatientId(pid);
                polarExercisesRepository.deleteByPatientId(pid);
                polarCardioRepository.deleteByPatientId(pid);
                polarUserInfoRepository.deleteByPatientId(pid);
                patientRepository.deleteById(pid);
            }

        }


    }

    private String requireExternalUserId(Long pid) {
        var c = connections.findByPatientIdAndProvider(pid, Provider.POLAR)
                .orElseThrow(() -> new IllegalStateException("Polar not connected for patient " + pid));
        String ext = c.getExternalUserId();
        if (ext == null || ext.isBlank())
            throw new IllegalStateException("externalUserId is empty; user not registered?");
        int slash = ext.lastIndexOf('/');
        return slash >= 0 ? ext.substring(slash + 1) : ext; // поддержка URL и «чистого» id
    }

    public record PolarRegisterResult(PolarRegisterStatus status, String location, String message) {
    }
}
