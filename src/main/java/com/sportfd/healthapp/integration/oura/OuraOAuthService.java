package com.sportfd.healthapp.integration.oura;

import com.sportfd.healthapp.integration.oura.dto.OuraTokenResponse;
import com.sportfd.healthapp.model.OuraConnection;
import com.sportfd.healthapp.model.Users;
import com.sportfd.healthapp.repo.OuraConnectionRepository;
import com.sportfd.healthapp.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OuraOAuthService {

    private final WebClient ouraAuthClient;
    private final WebClient ouraWebClient;
    private final OuraConnectionRepository connRepo;
    private final UserRepository usersRepo;

    @Value("${app.oura.client-id}")     private String clientId;
    @Value("${app.oura.client-secret}")  private String clientSecret;
    @Value("${app.oura.redirect-uri}")   private String redirectUri;


    @Transactional
    public void exchangeCodeAndSave(Long userId, String code) {
        OuraTokenResponse res = ouraAuthClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("grant_type","authorization_code")
                        .with("code", code)
                        .with("redirect_uri", redirectUri)
                        .with("client_id", clientId)
                        .with("client_secret", clientSecret))
                .retrieve()
                .bodyToMono(OuraTokenResponse.class)
                .block();

        saveTokens(userId, res);
    }

    @Transactional
    public void refreshIfNeeded(Long userId) {
        OuraConnection c = connRepo.findByUserId(userId).orElseThrow();
        if (c.getExpiresAt().isAfter(LocalDateTime.now().plusSeconds(60))) return; // ещё жив

        OuraTokenResponse res = ouraAuthClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("grant_type","refresh_token")
                        .with("refresh_token", c.getRefreshToken())
                        .with("client_id", clientId)
                        .with("client_secret", clientSecret))
                .retrieve()
                .bodyToMono(OuraTokenResponse.class)
                .block();

        saveTokens(userId, res);
    }



    private void saveTokens(Long userId, OuraTokenResponse res) {
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(res.getExpiresIn());
        OuraConnection c = connRepo.findByUserId(userId).orElse(new OuraConnection());
        c.setUserId(userId);
        c.setAccessToken(res.getAccessToken());
        c.setRefreshToken(res.getRefreshToken());
        c.setExpiresAt(expiresAt);
        c.setTokenType(res.getTokenType());
        c.setScope(res.getScope());
        connRepo.save(c);
    }

    /** пример: получить daily_sleep за последние 7 дней */
    public String getDailySleepJson(Long userId) {
        refreshIfNeeded(userId);
        OuraConnection c = connRepo.findByUserId(userId).orElseThrow();
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(7);

        return ouraWebClient.get()
                .uri(uri -> uri.path("/usercollection/daily_sleep")
                        .queryParam("start_date", start)
                        .queryParam("end_date", end)
                        .build())
                .headers(h -> h.setBearerAuth(c.getAccessToken()))
                .retrieve()
                .bodyToMono(String.class)   // для примера вернём сырой JSON
                .block();
    }
}