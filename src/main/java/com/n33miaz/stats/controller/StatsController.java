package com.n33miaz.stats.controller;

import com.n33miaz.stats.service.GithubService;
import com.n33miaz.stats.service.SvgService;
import com.n33miaz.stats.service.WakaTimeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatsController {

    @Autowired
    private SvgService svgService;

    @Autowired
    private GithubService githubService;

    @Autowired
    private WakaTimeService wakaTimeService;

    @Autowired
    private com.n33miaz.stats.service.LastFmService lastFmService;

    @GetMapping("/test")
    public ResponseEntity<String> getTestSvg(@RequestParam(defaultValue = "Hello n33miaz") String text) {
        String svg = svgService.generateTestSvg(text);
        return createSvgResponse(svg, 0);
    }

    @GetMapping("/pin")
    public Mono<ResponseEntity<String>> getRepoPin(
            @RequestParam String username,
            @RequestParam String repo,
            @RequestParam(required = false) String title_color,
            @RequestParam(required = false) String icon_color,
            @RequestParam(required = false) String text_color,
            @RequestParam(required = false) String bg_color,
            @RequestParam(required = false) String border_color,
            @RequestParam(defaultValue = "false") boolean hide_border,
            @RequestParam(defaultValue = "true") boolean show_description) {
        Map<String, String> colors = new HashMap<>();
        if (title_color != null)
            colors.put("title_color", title_color);
        if (icon_color != null)
            colors.put("icon_color", icon_color);
        if (text_color != null)
            colors.put("text_color", text_color);
        if (bg_color != null)
            colors.put("bg_color", bg_color);
        if (border_color != null)
            colors.put("border_color", border_color);

        return githubService.fetchRepository(username, repo)
                .map(repository -> {
                    String svg = svgService.generateRepoCard(repository, colors, hide_border, show_description);
                    return createSvgResponse(svg, 1800);
                })
                .onErrorResume(e -> {
                    e.printStackTrace();
                    String errorSvg = svgService.generateTestSvg("Error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(errorSvg, HttpStatus.BAD_REQUEST));
                });
    }

    @GetMapping("/stats")
    public Mono<ResponseEntity<String>> getGithubStats(
            @RequestParam String username,
            @RequestParam(required = false) String title_color,
            @RequestParam(required = false) String icon_color,
            @RequestParam(required = false) String text_color,
            @RequestParam(required = false) String bg_color,
            @RequestParam(required = false) String border_color,
            @RequestParam(defaultValue = "false") boolean hide_border) {

        Map<String, String> colors = new HashMap<>();
        if (title_color != null)
            colors.put("title_color", title_color);
        if (icon_color != null)
            colors.put("icon_color", icon_color);
        if (text_color != null)
            colors.put("text_color", text_color);
        if (bg_color != null)
            colors.put("bg_color", bg_color);
        if (border_color != null)
            colors.put("border_color", border_color);

        return githubService.fetchUserStats(username)
                .map(stats -> {
                    String svg = svgService.generateStatsCard(stats, colors, hide_border);
                    return createSvgResponse(svg, 3600); // 1 hora de cache
                })
                .onErrorResume(e -> {
                    e.printStackTrace();
                    String errorSvg = svgService.generateTestSvg("Stats Error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(errorSvg, HttpStatus.BAD_REQUEST));
                });
    }

    @GetMapping("/streak")
    public Mono<ResponseEntity<String>> getStreakStats(
            @RequestParam String username,
            @RequestParam(required = false) String title_color,
            @RequestParam(required = false) String icon_color, // usado como fallback
            @RequestParam(required = false) String text_color,
            @RequestParam(required = false) String bg_color,
            @RequestParam(required = false) String border_color,
            @RequestParam(required = false) String ring,
            @RequestParam(required = false) String fire,
            @RequestParam(required = false) String currStreakNum,
            @RequestParam(required = false) String sideNums,
            @RequestParam(required = false) String sideLabels,
            @RequestParam(required = false) String dates,
            @RequestParam(defaultValue = "false") boolean hide_border) {

        Map<String, String> colors = new HashMap<>();

        // Cores base
        if (title_color != null)
            colors.put("title_color", title_color);
        if (text_color != null)
            colors.put("text_color", text_color);
        if (bg_color != null)
            colors.put("bg_color", bg_color);
        if (border_color != null)
            colors.put("border_color", border_color);

        // Cores específicas do streak (se não passar, usa title_color/text_color no
        // SVGService)
        if (ring != null)
            colors.put("ring", ring);
        else if (title_color != null)
            colors.put("ring", title_color);

        if (fire != null)
            colors.put("fire", fire);
        else if (title_color != null)
            colors.put("fire", title_color);

        if (currStreakNum != null)
            colors.put("currStreakNum", currStreakNum);
        else if (text_color != null)
            colors.put("currStreakNum", text_color);

        if (sideNums != null)
            colors.put("sideNums", sideNums);
        else if (text_color != null)
            colors.put("sideNums", text_color);

        if (sideLabels != null)
            colors.put("sideLabels", sideLabels);

        if (dates != null)
            colors.put("dates", dates);

        return githubService.fetchStreakStats(username)
                .map(stats -> {
                    String svg = svgService.generateStreakCard(stats, colors, hide_border);
                    return createSvgResponse(svg, 3600); // 1 hora cache
                })
                .onErrorResume(e -> {
                    e.printStackTrace();
                    String errorSvg = svgService.generateTestSvg("Streak Error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(errorSvg, HttpStatus.BAD_REQUEST));
                });
    }

    @GetMapping("/graph")
    public Mono<ResponseEntity<String>> getContributionGraph(
            @RequestParam String username,
            @RequestParam(required = false) String waka_user,
            @RequestParam(required = false) String title_color,
            @RequestParam(required = false) String icon_color,
            @RequestParam(required = false) String text_color,
            @RequestParam(required = false) String bg_color,
            @RequestParam(required = false) String border_color,
            @RequestParam(defaultValue = "false") boolean hide_border) {
        Map<String, String> colors = new HashMap<>();
        if (title_color != null)
            colors.put("title_color", title_color);
        if (icon_color != null)
            colors.put("icon_color", icon_color);
        if (text_color != null)
            colors.put("text_color", text_color);
        if (bg_color != null)
            colors.put("bg_color", bg_color);
        if (border_color != null)
            colors.put("border_color", border_color);

        String finalWakaUser = waka_user != null ? waka_user : username;

        return Mono.zip(
                githubService.fetchContributions(username),
                wakaTimeService.getDailySummaries(finalWakaUser, 7)
                        .defaultIfEmpty(new com.n33miaz.stats.dto.WakaTimeSummaryResponse(Collections.emptyList())))
                .map(tuple -> {
                    var githubData = tuple.getT1();
                    var wakaData = tuple.getT2();

                    String svg = svgService.generateContributionGraph(githubData, wakaData, colors, hide_border,
                            username);

                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Content-Type", "image/svg+xml");
                    headers.add("Cache-Control", "public, max-age=3600"); // 1 hora
                    return new ResponseEntity<>(svg, headers, HttpStatus.OK);
                });
    }

    @GetMapping("/music")
    public Mono<ResponseEntity<String>> getMusicCard(
            @RequestParam String user,
            @RequestParam(required = false, defaultValue = "7day") String period, // PERIODO
            @RequestParam(required = false) String title_color,
            @RequestParam(required = false) String icon_color,
            @RequestParam(required = false) String text_color,
            @RequestParam(required = false) String bg_color,
            @RequestParam(required = false) String border_color,
            @RequestParam(defaultValue = "false") boolean hide_border) {
        Map<String, String> colors = new HashMap<>();
        if (title_color != null)
            colors.put("title_color", title_color);
        if (icon_color != null)
            colors.put("icon_color", icon_color);
        if (text_color != null)
            colors.put("text_color", text_color);
        if (bg_color != null)
            colors.put("bg_color", bg_color);
        if (border_color != null)
            colors.put("border_color", border_color);

        String periodText = switch (period) {
            case "overall" -> "All Time";
            case "7day" -> "7 Days";
            case "1month" -> "1 Month";
            case "3month" -> "3 Months";
            case "12month" -> "1 Year";
            default -> "7 Days";
        };

        return lastFmService.getDashboardData(user, period)
                .map(data -> {
                    String svg = svgService.generateMusicDashboard(data, colors, hide_border, periodText);
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Content-Type", "image/svg+xml");
                    headers.add("Cache-Control", "public, max-age=60"); // Cache de 1 min
                    return new ResponseEntity<>(svg, headers, HttpStatus.OK);
                })
                .onErrorResume(e -> {
                    e.printStackTrace();
                    String errorSvg = svgService.generateTestSvg("Music Error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(errorSvg, HttpStatus.BAD_REQUEST));
                });
    }

    private ResponseEntity<String> createSvgResponse(String svg, int cacheAge) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "image/svg+xml");
        if (cacheAge > 0) {
            headers.add("Cache-Control", "public, max-age=" + cacheAge);
        } else {
            headers.add("Cache-Control", "no-cache");
        }
        return new ResponseEntity<>(svg, headers, HttpStatus.OK);
    }
}