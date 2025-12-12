package com.n33miaz.stats.dto;

import java.util.List;

public record GithubStatsDto(Data data) {
    public record Data(User user) {
    }

    public record User(
            String name,
            String login,
            PullRequests pullRequests,
            Issues issues,
            RepositoriesContributedTo repositoriesContributedTo,
            Followers followers,
            Repositories repositories) {
    }

    public record PullRequests(int totalCount) {
    }

    public record Issues(int totalCount) {
    }

    public record RepositoriesContributedTo(int totalCount) {
    }

    public record Followers(int totalCount) {
    }

    public record Repositories(List<Node> nodes) {
    }

    public record Node(Stargazers stargazers) {
    }

    public record Stargazers(int totalCount) {
    }
}