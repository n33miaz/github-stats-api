package com.n33miaz.stats.dto;

public record StreakStatsDto(
        int currentYearCommits,
        int currentStreak,
        String currentStreakRange,
        int longestStreak,
        String longestStreakRange) {
}