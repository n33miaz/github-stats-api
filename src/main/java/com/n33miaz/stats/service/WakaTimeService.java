package com.n33miaz.stats.service;

import com.n33miaz.stats.dto.WakaTimeAllTimeResponse;
import com.n33miaz.stats.dto.WakaTimeSummaryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class WakaTimeService {

    @Value("${wakatime.api-key}")
    private String apiKey;

    private final WebClient webClient;

    public WakaTimeService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://wakatime.com/api/v1")
                .build();
    }

    public Mono<WakaTimeSummaryResponse> getDailySummaries(String username, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return webClient.get()
                .uri(uri -> uri.path("/users/{user}/summaries")
                        .queryParam("api_key", apiKey)
                        .queryParam("start", start.format(fmt))
                        .queryParam("end", end.format(fmt))
                        .build(username))
                .retrieve()
                .bodyToMono(WakaTimeSummaryResponse.class)
                .onErrorResume(e -> {
                    System.err.println("Erro WakaTime: " + e.getMessage());
                    return Mono.empty();
                });
    }

    public Mono<WakaTimeAllTimeResponse> getAllTimeStats(String username) {
        return webClient.get()
                .uri(uri -> uri.path("/users/{user}/all_time_since_today")
                        .queryParam("api_key", apiKey)
                        .build(username))
                .retrieve()
                .bodyToMono(WakaTimeAllTimeResponse.class)
                .onErrorResume(e -> {
                    System.err.println("Erro WakaTime All Time: " + e.getMessage());
                    return Mono.empty();
                });
    }
}