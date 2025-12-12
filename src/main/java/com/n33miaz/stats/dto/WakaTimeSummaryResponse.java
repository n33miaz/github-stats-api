package com.n33miaz.stats.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WakaTimeSummaryResponse(List<Summary> data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summary(
            @JsonProperty("grand_total") GrandTotal grandTotal,
            Range range) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GrandTotal(
            @JsonProperty("total_seconds") double totalSeconds,
            @JsonProperty("text") String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Range(String date) {
    }
}