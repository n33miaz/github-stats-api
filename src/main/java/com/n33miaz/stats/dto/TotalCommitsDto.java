package com.n33miaz.stats.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TotalCommitsDto(@JsonProperty("total_count") int totalCount) {
}