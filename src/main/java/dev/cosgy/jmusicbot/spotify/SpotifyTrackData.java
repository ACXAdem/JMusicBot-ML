/*
 *  Copyright 2026 JMusicBot-ML contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package dev.cosgy.jmusicbot.spotify;

/**
 * Info of a single track read from Spotify.
 */
public final class SpotifyTrackData {
    /** Spotify track ID (null if unknown). */
    public final String id;
    public final String title;
    /** All artists, comma-separated. */
    public final String artists;
    /** First (main) artist. */
    public final String primaryArtist;
    /** Duration in milliseconds (0 if unknown). */
    public final long durationMs;
    /** ISRC kodu (bilinmiyorsa null). */
    public final String isrc;
    public final String imageUrl;

    public SpotifyTrackData(String id, String title, String artists, String primaryArtist,
                            long durationMs, String isrc, String imageUrl) {
        this.id = emptyToNull(id);
        this.title = title == null ? "" : title.trim();
        this.artists = artists == null ? "" : artists.trim();
        this.primaryArtist = primaryArtist == null || primaryArtist.isBlank() ? firstArtist(this.artists) : primaryArtist.trim();
        this.durationMs = Math.max(0, durationMs);
        this.isrc = emptyToNull(isrc);
        this.imageUrl = emptyToNull(imageUrl);
    }

    public String url() {
        return id == null ? null : "https://open.spotify.com/track/" + id;
    }

    /** Cache key (to avoid searching the same track again). */
    public String cacheKey() {
        if (id != null) return "id:" + id;
        if (isrc != null) return "isrc:" + isrc;
        return "q:" + primaryArtist.toLowerCase() + "|" + title.toLowerCase() + "|" + (durationMs / 1000);
    }

    public SpotifyTrackData withImageIfMissing(String fallbackImage) {
        if (imageUrl != null || fallbackImage == null) return this;
        return new SpotifyTrackData(id, title, artists, primaryArtist, durationMs, isrc, fallbackImage);
    }

    private static String firstArtist(String artists) {
        if (artists == null) return "";
        int i = artists.indexOf(',');
        int j = artists.indexOf(" & ");
        int cut = i < 0 ? j : (j < 0 ? i : Math.min(i, j));
        return (cut > 0 ? artists.substring(0, cut) : artists).trim();
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    @Override
    public String toString() {
        return (artists.isEmpty() ? "" : artists + " - ") + title;
    }
}
