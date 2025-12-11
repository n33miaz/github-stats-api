package com.n33miaz.stats.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LastFmResponse(
        RecentTracks recenttracks,
        TopArtists topartists,
        TopAlbums topalbums,
        TrackDetails track) {

    // --- Tracks ---
    public record RecentTracks(List<Track> track) {
    }

    public record Track(
            String name,
            Artist artist,
            Album album,
            @JsonProperty("image") List<Image> images,
            @JsonProperty("@attr") Attr attr,
            Date date) {
    }

    public record TrackDetails(
            String userplaycount,
            @JsonProperty("toptags") TopTags topTags) {
    }

    public record TopTags(List<Tag> tag) {
    }

    public record Tag(String name) {
    }

    // --- Top Artists ---
    public record TopArtists(List<ArtistInfo> artist) {
    }

    public record ArtistInfo(
            String name,
            @JsonProperty("image") List<Image> images,
            String playcount) {
    }

    // --- Top Albums ---
    public record TopAlbums(List<AlbumInfo> album) {
    }

    public record AlbumInfo(
            String name,
            Artist artist,
            @JsonProperty("image") List<Image> images,
            String playcount) {
    }

    // --- Common ---
    public record Artist(@JsonProperty("#text") String name) {
    }

    public record Album(@JsonProperty("#text") String name) {
    }

    public record Image(@JsonProperty("#text") String url, String size) {
    }

    public record Attr(String nowplaying) {
    }

    public record Date(String uts, @JsonProperty("#text") String text) {
    }
}