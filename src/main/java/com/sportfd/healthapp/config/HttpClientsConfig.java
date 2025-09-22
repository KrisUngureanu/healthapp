package com.sportfd.healthapp.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
}