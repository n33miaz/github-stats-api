package com.n33miaz.stats.dto;

import java.util.List;

public record GithubContributionResponse(Data data) {
    public record Data(User user) {
    }

    public record User(ContributionsCollection contributionsCollection) {
    }

    public record ContributionsCollection(ContributionCalendar contributionCalendar) {
    }

    public record ContributionCalendar(List<Week> weeks) {
    }

    public record Week(List<ContributionDay> contributionDays) {
    }

    public record ContributionDay(String date, int contributionCount, String color) {
    }
}