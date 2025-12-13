package com.n33miaz.stats.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WakaTimeAllTimeResponse(Data data) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(String text, double total_seconds) {
    }
}