package com.sportfd.healthapp.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HttpClientsConfig {

    @Bean
    public WebClient ouraWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.ouraring.com")
                .build();
    }

    @Bean
    public WebClient ouraAuthClient() {
        return WebClient.builder()
                .baseUrl("https://cloud.ouraring.com")
                .build();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder b) { return b.build(); }

    @Bean
    public RestClient ouraRestClient() {
        return RestClient.builder().baseUrl("https://api.ouraring.com").build();
    }
    @Bean(name = "whoopRestClient")
    public RestClient whoopRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.prod.whoop.com")
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean(name = "polarRestClient")
    public RestClient polarRestClient() {
        return RestClient.builder()
                .baseUrl("https://www.polaraccesslink.com")
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    @Qualifier("polarAccessLink")
    public RestClient polarAccessLinkRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl("https://www.polaraccesslink.com/v3")
                .build();
    }

    @Bean(name = "garminRestClient")
    public RestClient garminRestClient(
            @Value("${app.garmin.api-base:https://apis.garmin.com}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean(name = "garminHealthRestClient")
    public RestClient garminHealthRestClient(
            @Value("${app.garmin.health-base:https://apis.garmin.com/health-api}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }


    @Bean(name = "garminActivityRestClient")
    public RestClient garminActivityRestClient(
            @Value("${app.garmin.activity-base:https://apis.garmin.com/activity-api}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean(name = "garminAuthClient")
    public RestClient garminAuthClient(
            @Value("${app.garmin.auth-base}") String baseUrl
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl) // <-- ДОЛЖЕН быть https://diauth.garmin.com
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

}