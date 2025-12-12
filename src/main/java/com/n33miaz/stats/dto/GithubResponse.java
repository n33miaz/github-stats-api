package com.n33miaz.stats.dto;

public record GithubResponse(Data data) {
    public record Data(Repository repository) {
    }

    public record Repository(
            String name,
            String description,
            int stargazerCount,
            int forkCount,
            PrimaryLanguage primaryLanguage,
            ObjectData object) {
    }

    public record PrimaryLanguage(
            String name,
            String color) {
    }

    public record ObjectData(History history) {
    }

    public record History(int totalCount) {
    }
}