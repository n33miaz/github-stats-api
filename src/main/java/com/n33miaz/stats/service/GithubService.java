package com.n33miaz.stats.service;

import com.n33miaz.stats.dto.GithubContributionResponse;
import com.n33miaz.stats.dto.GithubResponse;
import com.n33miaz.stats.dto.GithubStatsDto;
import com.n33miaz.stats.dto.StreakStatsDto;
import com.n33miaz.stats.dto.TotalCommitsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class GithubService {

  @Autowired
  private WebClient webClient;

  private final WebClient restWebClient;

  public GithubService(WebClient.Builder webClientBuilder,
      @org.springframework.beans.factory.annotation.Value("${github.token}") String token) {
    this.restWebClient = webClientBuilder
        .baseUrl("https://api.github.com")
        .defaultHeader("Authorization", "Bearer " + token)
        .defaultHeader("Accept", "application/vnd.github.cloak-preview")
        .build();
  }

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

  public Mono<StatsData> fetchUserStats(String username) {
    Mono<GithubStatsDto> graphQlData = fetchGraphQlStats(username);

    Mono<Integer> totalCommitsData = fetchTotalCommits(username);

    return Mono.zip(graphQlData, totalCommitsData)
        .map(tuple -> {
          var user = tuple.getT1().data().user();
          int totalCommits = tuple.getT2();

          // total de estrelas
          int totalStars = user.repositories().nodes().stream()
              .mapToInt(node -> node.stargazers().totalCount())
              .sum();

          // calcula Rank
          Rank rank = calculateRank(
              true,
              totalCommits,
              user.pullRequests().totalCount(),
              user.issues().totalCount(),
              0,
              user.repositories().nodes().size(),
              totalStars,
              user.followers().totalCount());

          return new StatsData(
              totalCommits,
              user.repositoriesContributedTo().totalCount(),
              user.pullRequests().totalCount(),
              user.issues().totalCount(),
              rank);
        });
  }

  private Mono<GithubStatsDto> fetchGraphQlStats(String username) {
    String query = """
        query($login: String!) {
          user(login: $login) {
            pullRequests { totalCount }
            issues { totalCount }
            repositoriesContributedTo { totalCount }
            followers { totalCount }
            repositories(first: 100, ownerAffiliations: OWNER, orderBy: {direction: DESC, field: STARGAZERS}) {
              nodes { stargazers { totalCount } }
            }
          }
        }
        """;
    return webClient.post()
        .bodyValue(Map.of("query", query, "variables", Map.of("login", username)))
        .retrieve()
        .bodyToMono(GithubStatsDto.class);
  }

  private Mono<Integer> fetchTotalCommits(String username) {
    return restWebClient.get()
        .uri("/search/commits?q=author:" + username)
        .retrieve()
        .bodyToMono(TotalCommitsDto.class)
        .map(TotalCommitsDto::totalCount)
        .onErrorReturn(0);
  }

  // --- LÓGICA DE RANK ---

  public record StatsData(int commits, int contributedTo, int prs, int issues, Rank rank) {
  }

  public record Rank(String level, double percentile) {
  }

  private Rank calculateRank(boolean allCommits, int commits, int prs, int issues, int reviews, int repos, int stars,
      int followers) {
    double COMMITS_MEDIAN = allCommits ? 100 : 50;
    double COMMITS_WEIGHT = 2;
    double PRS_MEDIAN = 5;
    double PRS_WEIGHT = 3;
    double ISSUES_MEDIAN = 5;
    double ISSUES_WEIGHT = 1;
    double REVIEWS_MEDIAN = 1;
    double REVIEWS_WEIGHT = 1;
    double STARS_MEDIAN = 5;
    double STARS_WEIGHT = 4;
    double FOLLOWERS_MEDIAN = 2;
    double FOLLOWERS_WEIGHT = 1;

    double TOTAL_WEIGHT = COMMITS_WEIGHT + PRS_WEIGHT + ISSUES_WEIGHT + REVIEWS_WEIGHT + STARS_WEIGHT
        + FOLLOWERS_WEIGHT;

    double rankScore = 1 - (COMMITS_WEIGHT * exponentialCdf(commits / COMMITS_MEDIAN) +
        PRS_WEIGHT * exponentialCdf(prs / PRS_MEDIAN) +
        ISSUES_WEIGHT * exponentialCdf(issues / ISSUES_MEDIAN) +
        REVIEWS_WEIGHT * exponentialCdf(reviews / REVIEWS_MEDIAN) +
        STARS_WEIGHT * logNormalCdf(stars / STARS_MEDIAN) +
        FOLLOWERS_WEIGHT * logNormalCdf(followers / FOLLOWERS_MEDIAN)) / TOTAL_WEIGHT;

    double percentile = rankScore * 100;

    String level;
    if (percentile <= 1)
      level = "S";
    else if (percentile <= 12.5)
      level = "A+";
    else if (percentile <= 25)
      level = "A";
    else if (percentile <= 37.5)
      level = "A-";
    else if (percentile <= 50)
      level = "B+";
    else if (percentile <= 62.5)
      level = "B";
    else if (percentile <= 75)
      level = "B-";
    else if (percentile <= 87.5)
      level = "C+";
    else
      level = "C";

    return new Rank(level, percentile);
  }

  private double exponentialCdf(double x) {
    return 1 - Math.pow(2, -x);
  }

  private double logNormalCdf(double x) {
    return x / (1 + x);
  }

  public Mono<StreakStatsDto> fetchStreakStats(String username) {
    return fetchContributions(username)
        .map(this::calculateStreak);
  }

  private StreakStatsDto calculateStreak(GithubContributionResponse response) {
    List<GithubContributionResponse.ContributionDay> allDays = new ArrayList<>();

    if (response.data() != null && response.data().user() != null) {
      response.data().user().contributionsCollection().contributionCalendar().weeks()
          .forEach(week -> allDays.addAll(week.contributionDays()));
    }

    allDays.sort(Comparator.comparing(GithubContributionResponse.ContributionDay::date));

    LocalDate today = LocalDate.now();
    int currentYear = today.getYear();
    DateTimeFormatter rangeFmt = DateTimeFormatter.ofPattern("MMM dd", Locale.US);

    int totalCommitsYear = allDays.stream()
        .filter(d -> LocalDate.parse(d.date()).getYear() == currentYear)
        .mapToInt(GithubContributionResponse.ContributionDay::contributionCount)
        .sum();

    int currentStreak = 0;
    int longestStreak = 0;
    int tempStreak = 0;

    LocalDate currentStreakStart = null;
    LocalDate currentStreakEnd = null;

    LocalDate longestStreakStart = null;
    LocalDate longestStreakEnd = null;
    LocalDate tempStart = null;

    for (GithubContributionResponse.ContributionDay day : allDays) {
      if (day.contributionCount() > 0) {
        if (tempStreak == 0) {
          tempStart = LocalDate.parse(day.date());
        }
        tempStreak++;

        if (tempStreak > longestStreak) {
          longestStreak = tempStreak;
          longestStreakStart = tempStart;
          longestStreakEnd = LocalDate.parse(day.date());
        }
      } else {
        tempStreak = 0;
        tempStart = null;
      }
    }

    boolean streakActive = false;
    int size = allDays.size();

    if (size > 0) {
      var lastDay = allDays.get(size - 1);
      LocalDate lastDate = LocalDate.parse(lastDay.date());

      if ((lastDate.isEqual(today) || lastDate.isEqual(today.minusDays(1))) && lastDay.contributionCount() > 0) {
        streakActive = true;
      }
      else if (size > 1) {
        var yesterday = allDays.get(size - 2);
        if (LocalDate.parse(yesterday.date()).isEqual(today.minusDays(1)) && yesterday.contributionCount() > 0) {
          streakActive = true;
        }
      }
    }

    if (streakActive) {
      for (int i = size - 1; i >= 0; i--) {
        var day = allDays.get(i);
        if (day.contributionCount() > 0) {
          currentStreak++;
          currentStreakStart = LocalDate.parse(day.date());
          if (currentStreakEnd == null)
            currentStreakEnd = LocalDate.parse(day.date());
        } else {
          LocalDate d = LocalDate.parse(day.date());
          if (!d.isEqual(today)) {
            break;
          }
        }
      }
    }

    String currentRange = formatRange(currentStreakStart, currentStreakEnd, rangeFmt);
    String longestRange = formatRange(longestStreakStart, longestStreakEnd, rangeFmt);

    return new StreakStatsDto(totalCommitsYear, currentStreak, currentRange, longestStreak, longestRange);
  }

  private String formatRange(LocalDate start, LocalDate end, DateTimeFormatter fmt) {
    if (start == null || end == null)
      return "No Activity";
    if (start.isEqual(end))
      return start.format(fmt);
    return start.format(fmt) + " - " + end.format(fmt);
  }
}