package com.n33miaz.stats.service;

import com.n33miaz.stats.dto.LastFmResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Service
public class LastFmService {

    @Value("${lastfm.api-key}")
    private String apiKey;

    private final WebClient webClient;

    public LastFmService() {
        this.webClient = WebClient.builder()
                .baseUrl("http://ws.audioscrobbler.com/2.0/")
                .build();
    }

    public Mono<MusicDashboardData> getDashboardData(String username, String period) {
        Mono<TrackInfo> recentTrackMono = getRecentTrack(username)
                .flatMap(track -> getTrackPlayCount(username, track));

        Mono<List<SimpleItem>> topArtistsMono = getTopArtists(username, period);
        Mono<List<SimpleItem>> topAlbumsMono = getTopAlbums(username, period);

        return Mono.zip(recentTrackMono, topArtistsMono, topAlbumsMono)
                .map(tuple -> new MusicDashboardData(tuple.getT1(), tuple.getT2(), tuple.getT3()));
    }

    private Mono<TrackInfo> getRecentTrack(String username) {
        return webClient.get()
                .uri(uri -> uri.queryParam("method", "user.getrecenttracks")
                        .queryParam("user", username)
                        .queryParam("api_key", apiKey)
                        .queryParam("format", "json")
                        .queryParam("limit", "1").build())
                .retrieve()
                .bodyToMono(LastFmResponse.class)
                .flatMap(response -> {
                    if (response.recenttracks() == null || response.recenttracks().track().isEmpty())
                        return Mono.just(new TrackInfo("No Track", "Unknown", "", "", false, "Never", 0));

                    var track = response.recenttracks().track().get(0);
                    boolean isPlaying = track.attr() != null && "true".equals(track.attr().nowplaying());
                    String imageUrl = getImageUrl(track.images());
                    String timeAgo = isPlaying ? "Now Playing" : calculateTimeAgo(track.date());

                    return downloadImageAsBase64(imageUrl)
                            .map(imgBase64 -> new TrackInfo(
                                    track.name(),
                                    track.artist().name(),
                                    track.album().name(),
                                    imgBase64,
                                    isPlaying,
                                    timeAgo,
                                    0));
                });
    }

    private Mono<TrackInfo> getTrackPlayCount(String username, TrackInfo track) {
        if (track.name().equals("No Track"))
            return Mono.just(track);

        return webClient.get()
                .uri(uri -> uri.queryParam("method", "track.getInfo")
                        .queryParam("user", username)
                        .queryParam("artist", track.artist())
                        .queryParam("track", track.name())
                        .queryParam("api_key", apiKey)
                        .queryParam("format", "json").build())
                .retrieve()
                .bodyToMono(LastFmResponse.class)
                .map(response -> {
                    int plays = 0;
                    if (response.track() != null && response.track().userplaycount() != null) {
                        try {
                            plays = Integer.parseInt(response.track().userplaycount());
                        } catch (NumberFormatException e) {
                        }
                    }
                    return new TrackInfo(track.name(), track.artist(), track.album(), track.imageBase64(),
                            track.isPlaying(), track.timeAgo(), plays);
                })
                .onErrorResume(e -> Mono.just(track));
    }

    private Mono<List<SimpleItem>> getTopArtists(String username, String period) {
        return webClient.get()
                .uri(uri -> uri.queryParam("method", "user.gettopartists")
                        .queryParam("user", username)
                        .queryParam("api_key", apiKey)
                        .queryParam("period", period)
                        .queryParam("format", "json")
                        .queryParam("limit", "3").build())
                .retrieve()
                .bodyToMono(LastFmResponse.class)
                .flatMapMany(response -> Flux.fromIterable(response.topartists().artist()))
                .concatMap(artist -> {
                    String imgUrl = getImageUrl(artist.images());
                    return downloadImageAsBase64(imgUrl)
                            .map(base64 -> new SimpleItem(artist.name(), artist.playcount() + " plays", base64));
                })
                .collectList();
    }

    private Mono<List<SimpleItem>> getTopAlbums(String username, String period) {
        return webClient.get()
                .uri(uri -> uri.queryParam("method", "user.gettopalbums")
                        .queryParam("user", username)
                        .queryParam("api_key", apiKey)
                        .queryParam("period", period)
                        .queryParam("format", "json")
                        .queryParam("limit", "3").build())
                .retrieve()
                .bodyToMono(LastFmResponse.class)
                .flatMapMany(response -> Flux.fromIterable(response.topalbums().album()))
                .flatMap(album -> {
                    String imgUrl = getImageUrl(album.images());
                    return downloadImageAsBase64(imgUrl)
                            .map(base64 -> new SimpleItem(album.name(), album.artist().name(), base64,
                                    album.playcount() + " plays"));
                })
                .collectList();
    }

    private String calculateTimeAgo(LastFmResponse.Date date) {
        if (date == null || date.uts() == null)
            return "";
        try {
            long uts = Long.parseLong(date.uts());
            Instant playedAt = Instant.ofEpochSecond(uts);
            Duration diff = Duration.between(playedAt, Instant.now());

            if (diff.toMinutes() < 1)
                return "Just now";
            if (diff.toMinutes() < 60)
                return diff.toMinutes() + " mins ago";
            if (diff.toHours() < 24)
                return diff.toHours() + " hours ago";
            return diff.toDays() + " days ago";
        } catch (Exception e) {
            return "";
        }
    }

    private String getImageUrl(List<LastFmResponse.Image> images) {
        if (images == null)
            return "";
        return images.stream()
                .filter(img -> "extra-large".equals(img.size()) || "large".equals(img.size()))
                .findFirst()
                .map(LastFmResponse.Image::url)
                .orElse("");
    }

    private Mono<String> downloadImageAsBase64(String url) {
        if (url == null || url.isEmpty())
            return Mono.just("");
        return webClient.get().uri(url).retrieve().bodyToMono(byte[].class)
                .map(bytes -> "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes))
                .onErrorReturn("");
    }

    public record TrackInfo(String name, String artist, String album, String imageBase64, boolean isPlaying,
            String timeAgo, int userPlayCount) {
    }

    public record SimpleItem(String title, String subtitle, String imageBase64, String extraInfo) {
        public SimpleItem(String title, String subtitle, String imageBase64) {
            this(title, subtitle, imageBase64, null);
        }
    }

    public record MusicDashboardData(TrackInfo currentTrack, List<SimpleItem> topArtists, List<SimpleItem> topAlbums) {
    }
}