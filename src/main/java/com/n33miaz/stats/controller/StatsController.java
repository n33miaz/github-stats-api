package com.n33miaz.stats.controller;

import com.n33miaz.stats.service.GithubService;
import com.n33miaz.stats.service.SvgService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatsController {

    @Autowired
    private SvgService svgService;

    @Autowired
    private GithubService githubService;

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
            @RequestParam(defaultValue = "false") boolean hide_border) {
        // parâmetros de estilo
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
                    String svg = svgService.generateRepoCard(repository, colors, hide_border);
                    return createSvgResponse(svg, 1800); // 30 min cache
                })
                .onErrorResume(e -> {
                    e.printStackTrace();
                    String errorSvg = svgService.generateTestSvg("Error: " + e.getMessage());
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