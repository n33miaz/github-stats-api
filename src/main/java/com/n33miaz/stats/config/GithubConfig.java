package com.n33miaz.stats.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GithubConfig {

    @Value("${github.token}")
    private String githubToken;

    @Bean
    public WebClient githubWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.github.com/graphql")
                .defaultHeader("Authorization", "Bearer " + githubToken)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}