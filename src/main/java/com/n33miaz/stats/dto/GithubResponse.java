package com.n33miaz.stats.dto;

import java.util.Map;

public record GithubResponse(Data data) {
    public record Data(Repository repository) {
    }

    public record Repository(
            String name,
            String description,
            int stargazerCount,
            int forkCount,
            PrimaryLanguage primaryLanguage) {
    }

    public record PrimaryLanguage(
            String name,
            String color) {
    }
}

class GraphqlRequestBody {
    public String query;
    public Map<String, Object> variables;

    public GraphqlRequestBody(String query, Map<String, Object> variables) {
        this.query = query;
        this.variables = variables;
    }
}