package com.sportfd.healthapp.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class InviteTokenUtil {

    @Value("${app.invite.secret}")
    private String secret;

    // формируем t = base64url(HMAC_SHA256(pid.expiry))
    public String sign(long patientId, Instant expiresAt) {
        String payload = patientId + "." + expiresAt.getEpochSecond();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
            return payload + "." + token;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign invite", e);
        }
    }

    // парсим и проверяем t, возвращаем patientId если ок
    public Long verifyAndGetPatientId(String t) {
        String[] parts = t.split("\\.");
        if (parts.length != 3) return null;
        long pid = Long.parseLong(parts[0]);
        long exp = Long.parseLong(parts[1]);
        String sig = parts[2];

        String payload = parts[0] + "." + parts[1];
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(expected);
            if (!expectedB64.equals(sig)) return null;
            if (Instant.now().getEpochSecond() > exp) return null;
            return pid;
        } catch (Exception e) {
            return null;
        }
    }

    // готовый URL, который врач отдаёт пациенту
    public String buildInviteUrl(String baseUrl, long patientId, String provider ,long ttlMinutes) {
        Instant exp = Instant.now().plusSeconds(ttlMinutes * 60);
        String t = sign(patientId, exp);
        String encT = URLEncoder.encode(t, StandardCharsets.UTF_8);
        String partUrl = " ";
        if (provider.equals("oura")){
            partUrl = "/oauth/oura/start?t=";
        } else if (provider.equals("whoop")) {
            partUrl = "/oauth/whoop/start?t=";
        } else if (provider.equals("polar")) {
            partUrl = "/oauth/polar/start?t=";
        } else if (provider.equals("garmin")){
            partUrl = "/oauth/garmin/start?t=";
        }
        return baseUrl + partUrl + encT;
    }
}