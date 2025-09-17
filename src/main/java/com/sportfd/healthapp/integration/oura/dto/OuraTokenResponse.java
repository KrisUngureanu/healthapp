package com.sportfd.healthapp.integration.oura.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OuraTokenResponse {
    @JsonProperty("access_token")  private String accessToken;
    @JsonProperty("refresh_token") private String refreshToken;
    @JsonProperty("expires_in")    private long expiresIn;  // сек
    @JsonProperty("token_type")    private String tokenType;
    private String scope; // пробел-разделённый список
}