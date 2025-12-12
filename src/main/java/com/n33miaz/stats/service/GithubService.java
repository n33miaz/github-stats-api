package com.n33miaz.stats.service;

import com.n33miaz.stats.dto.GithubContributionResponse;
import com.n33miaz.stats.dto.GithubResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class GithubService {

  @Autowired
  private WebClient webClient;

  public Mono<GithubResponse.Repository> fetchRepository(String owner, String name) {
    String query = """
        query($owner: String!, $name: String!) {
          repository(owner: $owner, name: $name) {
            name
            description
            stargazerCount
            forkCount
            primaryLanguage {
              name
              color
            }
            object(expression: "HEAD") {
              ... on Commit {
                history {
                  totalCount
                }
              }
            }
          }
        }
        """;

    Map<String, Object> variables = Map.of("owner", owner, "name", name);
    Map<String, Object> body = Map.of("query", query, "variables", variables);

    return webClient.post()
        .bodyValue(body)
        .retrieve()
        .bodyToMono(GithubResponse.class)
        .map(response -> {
          if (response.data() == null || response.data().repository() == null) {
            throw new RuntimeException("Repositório não encontrado");
          }
          return response.data().repository();
        });
  }

  public Mono<GithubContributionResponse> fetchContributions(String username) {
    String query = """
        query($username: String!) {
          user(login: $username) {
            contributionsCollection {
              contributionCalendar {
                weeks {
                  contributionDays {
                    date
                    contributionCount
                    color
                  }
                }
              }
            }
          }
        }
        """;

    Map<String, Object> variables = Map.of("username", username);
    Map<String, Object> body = Map.of("query", query, "variables", variables);

    return webClient.post()
        .bodyValue(body)
        .retrieve()
        .bodyToMono(GithubContributionResponse.class);
  }
}