package com.n33miaz.stats.service;

import com.n33miaz.stats.dto.LastFmResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class LastFmService {

    @Value("${lastfm.api-key}")
    private String apiKey;

    private final WebClient webClient;

    private static final String PLACEHOLDER_HASH = "2a96cbd8b46e442fc41c2b86b821562f";

    public LastFmService() {
        this.webClient = WebClient.builder()
                .baseUrl("http://ws.audioscrobbler.com/2.0/")
                .build();
    }

    public Mono<MusicDashboardData> getDashboardData(String username, String period) {
        System.out.println(">>> Iniciando busca de dados para usuário: " + username + " | Periodo: " + period);

        Mono<TrackInfo> recentTrackMono = getRecentTrack(username)
                .flatMap(track -> getTrackPlayCount(username, track));

        Mono<List<SimpleItem>> topArtistsMono = getTopArtists(username, period);
        Mono<List<SimpleItem>> topAlbumsMono = getTopAlbums(username, period);

        return Mono.zip(recentTrackMono, topArtistsMono, topAlbumsMono)
                .map(tuple -> new MusicDashboardData(tuple.getT1(), tuple.getT2(), tuple.getT3()))
                .doOnSuccess(data -> System.out.println(">>> Dashboard montado com sucesso!"));
    }

    // --- ARTISTAS ---
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
                    String artistName = artist.name();
                    String initialImgUrl = getImageUrl(artist.images());

                    Mono<String> imageDownloadMono;

                    if (isInvalidImage(initialImgUrl)) {
                        imageDownloadMono = fetchArtistTopAlbumImage(artistName)
                                .flatMap(this::downloadImageAsBase64);
                    } else {
                        imageDownloadMono = downloadImageAsBase64(initialImgUrl);
                    }

                    return imageDownloadMono
                            .map(base64 -> new SimpleItem(artistName, artist.playcount() + " plays", base64));
                })
                .collectList();
    }

    private Mono<String> fetchArtistTopAlbumImage(String artistName) {
        return webClient.get()
                .uri(uri -> uri.queryParam("method", "artist.gettopalbums")
                        .queryParam("artist", artistName)
                        .queryParam("api_key", apiKey)
                        .queryParam("format", "json")
                        .queryParam("autocorrect", "1")
                        .queryParam("limit", "1").build())
                .retrieve()
                .bodyToMono(LastFmResponse.class)
                .map(response -> {
                    if (response.topalbums() != null && response.topalbums().album() != null
                            && !response.topalbums().album().isEmpty()) {
                        String albumCover = getImageUrl(response.topalbums().album().get(0).images());
                        return albumCover;
                    }
                    return "";
                })
                .onErrorResume(e -> {
                    return Mono.just("");
                });
    }

    // --- RECENT TRACK ---
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

                    if (isInvalidImage(imageUrl)) {
                        System.out.println("   [Track] Capa da música atual inválida. Tentando fallback...");
                    }

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

    // --- ALBUMS ---
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
                .flatMapSequential(album -> {
                    String imgUrl = getImageUrl(album.images());

                    return downloadImageAsBase64(imgUrl)
                            .map(base64 -> new SimpleItem(album.name(), album.artist().name(), base64,
                                    album.playcount() + " plays"));
                })
                .collectList();
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
                        } catch (Exception e) {
                        }
                    }
                    return new TrackInfo(track.name(), track.artist(), track.album(), track.imageBase64(),
                            track.isPlaying(), track.timeAgo(), plays);
                })
                .onErrorResume(e -> Mono.just(track));
    }

    // --- UTILS ---

    private boolean isInvalidImage(String url) {
        return url == null || url.isEmpty() || url.contains(PLACEHOLDER_HASH);
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

    private Mono<String> downloadImageAsBase64(String url) {
        if (isInvalidImage(url))
            return Mono.just("");

        System.out.println("      -> Baixando imagem: " + url);
        return webClient.get().uri(url).retrieve().bodyToMono(byte[].class)
                .map(bytes -> "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes))
                .onErrorResume(e -> {
                    System.out.println("      [Erro Download] Falha ao baixar imagem: " + url);
                    return Mono.just("");
                });
    }

    // --- RECORDS ---
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