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
package dev.cosgy.jmusicbot.lyrics;

import dev.cosgy.jmusicbot.i18n.L;

import com.jagrosh.jlyrics.LyricsClient;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import dev.cosgy.jmusicbot.spotify.SpotifyMatcher;
import dev.cosgy.jmusicbot.spotify.SpotifyMirrorTrack;
import dev.cosgy.jmusicbot.spotify.SpotifyTrackData;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Finds lyrics.
 *
 * <p>Primary source is <b>LRCLIB</b> (https://lrclib.net): free, needs no key and
 * has many songs in many languages. If nothing is found, the old method (JLyrics: MusicMatch / AZLyrics etc.)
 * is tried as a fallback.</p>
 */
public class LyricsService {
    private static final Logger log = LoggerFactory.getLogger("Lyrics");
    private static final String API = "https://lrclib.net/api";
    private static final String USER_AGENT = "JMusicBot-ML (Discord music bot, based on JMusicBot-JP)";

    /** Unneeded suffixes in YouTube titles: (Official Video), [Lyrics], (Klip) etc. */
    private static final Pattern NOISE = Pattern.compile(
            "\\s*[(\\[{][^)\\]}]*(official|offical|video|audio|lyric|lyrics|klip|clip|music|m/v|mv|hd|4k|visualizer|resmi|şarkı sözleri|sözleri|canlı|live|remaster|prod)[^)\\]}]*[)\\]}]",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FEAT = Pattern.compile("\\s+(feat\\.?|ft\\.?|featuring)\\s+.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHANNEL_SUFFIX = Pattern.compile("\\s*(-\\s*topic|vevo|official|resmi)\\s*$", Pattern.CASE_INSENSITIVE);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final LyricsClient fallback = new LyricsClient();

    /** Lyrics that were found. */
    public static final class Result {
        public final String title;
        public final String artist;
        public final String lyrics;
        public final boolean instrumental;
        public final String source;
        public final String url;

        Result(String title, String artist, String lyrics, boolean instrumental, String source, String url) {
            this.title = title == null ? "" : title;
            this.artist = artist == null ? "" : artist;
            this.lyrics = lyrics == null ? "" : lyrics.trim();
            this.instrumental = instrumental;
            this.source = source;
            this.url = url;
        }
    }

    /** Search query for the current track. */
    public static final class Query {
        public final String title;
        public final String artist;
        public final long durationMs;

        public Query(String title, String artist, long durationMs) {
            this.title = title == null ? "" : title.trim();
            this.artist = artist == null ? "" : artist.trim();
            this.durationMs = durationMs;
        }

        public String display() {
            return artist.isEmpty() ? title : artist + " - " + title;
        }
    }

    /** Builds the best query from the current track (uses Spotify info if available, cleans the YouTube title). */
    public static Query fromTrack(AudioTrack track) {
        if (track instanceof SpotifyMirrorTrack) {
            SpotifyTrackData d = ((SpotifyMirrorTrack) track).getSpotifyData();
            return new Query(SpotifyMatcher.cleanTitle(d.title), d.primaryArtist, d.durationMs);
        }
        AudioTrackInfo info = track.getInfo();
        return fromText(info.title, info.author, track.getDuration());
    }

    /** Builds a query from free text (e.g. a YouTube title). */
    public static Query fromText(String rawTitle, String rawAuthor, long durationMs) {
        String title = rawTitle == null ? "" : rawTitle;
        title = NOISE.matcher(title).replaceAll("");
        title = title.replace('|', '-').replaceAll("\\s+", " ").trim();
        String artist = "";
        int dash = indexOfDash(title);
        if (dash > 0) {
            artist = title.substring(0, dash).trim();
            title = title.substring(dash + 1).replaceFirst("^[-–—]\\s*", "").trim();
        } else if (rawAuthor != null) {
            artist = CHANNEL_SUFFIX.matcher(rawAuthor).replaceAll("").trim();
        }
        title = FEAT.matcher(title).replaceAll("").replaceAll("[\"“”]", "").trim();
        // "Xir & Şam" / "Xir, Şam" -> first artist
        String primary = artist.split("\\s*(,|&| x | X | ve | feat\\.? | ft\\.? )\\s*")[0].trim();
        return new Query(title, primary, durationMs);
    }

    private static int indexOfDash(String s) {
        for (String d : new String[]{" - ", " – ", " — "}) {
            int i = s.indexOf(d);
            if (i > 0) return i + 1;
        }
        return -1;
    }

    public CompletableFuture<Result> find(Query q) {
        return CompletableFuture.supplyAsync(() -> {
            Result r = null;
            try {
                r = lrclib(q);
            } catch (Exception e) {
                log.warn(L.t("lyrics_service.lrclib_search_failed"), e.toString());
            }
            if (r != null) return r;
            try {
                com.jagrosh.jlyrics.Lyrics l = fallback.getLyrics(q.display()).get(15, TimeUnit.SECONDS);
                if (l != null && l.getContent() != null && !l.getContent().isBlank()) {
                    return new Result(l.getTitle(), l.getAuthor(), l.getContent(), false, l.getSource(), l.getURL());
                }
            } catch (Exception e) {
                log.debug(L.t("lyrics_service.fallback_lyrics_source_failed"), e.toString());
            }
            return null;
        });
    }

    private Result lrclib(Query q) throws Exception {
        // 1) Exact match (artist + track + duration)
        if (!q.artist.isEmpty() && !q.title.isEmpty()) {
            StringBuilder url = new StringBuilder(API + "/get?track_name=").append(enc(q.title))
                    .append("&artist_name=").append(enc(q.artist));
            long sec = Math.round(q.durationMs / 1000.0);
            if (sec >= 1 && sec <= 3600) url.append("&duration=").append(sec);
            String body = get(url.toString());
            if (body != null && body.startsWith("{")) {
                Result r = toResult(new JSONObject(body));
                if (r != null) return r;
            }
        }
        // 2) Free search and pick the best result
        String query = (q.artist + " " + q.title).trim();
        if (query.isEmpty()) return null;
        String body = get(API + "/search?q=" + enc(query));
        if ((body == null || !body.startsWith("[") || body.equals("[]")) && !q.artist.isEmpty()) {
            body = get(API + "/search?q=" + enc(q.title)); // retry without the artist
        }
        if (body == null || !body.startsWith("[")) return null;
        JSONArray arr = new JSONArray(body);
        JSONObject best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            double s = score(o, q);
            if (s > bestScore) {
                bestScore = s;
                best = o;
            }
        }
        if (best == null || bestScore < 20) return null;
        return toResult(best);
    }

    static double score(JSONObject o, Query q) {
        String t = SpotifyMatcher.normalize(o.optString("trackName", ""));
        String a = SpotifyMatcher.normalize(o.optString("artistName", ""));
        String qt = SpotifyMatcher.normalize(q.title);
        String qa = SpotifyMatcher.normalize(q.artist);
        double s = 0;
        Set<String> qTok = new HashSet<>(Arrays.asList(qt.split(" ")));
        qTok.remove("");
        if (!qTok.isEmpty()) {
            Set<String> tTok = new HashSet<>(Arrays.asList(t.split(" ")));
            int hit = 0;
            for (String tok : qTok) if (tTok.contains(tok)) hit++;
            s += 40.0 * hit / qTok.size();
        }
        if (t.equals(qt)) s += 20;
        if (!qa.isEmpty() && (a.contains(qa) || qa.contains(a) && !a.isEmpty())) s += 25;
        double dur = o.optDouble("duration", 0);
        if (q.durationMs > 0 && dur > 0) {
            double diff = Math.abs(dur - q.durationMs / 1000.0);
            if (diff <= 3) s += 20;
            else if (diff <= 10) s += 8;
            else if (diff > 30) s -= 20;
        }
        if (o.optString("plainLyrics", "").isBlank() && !o.optBoolean("instrumental", false)) s -= 50;
        return s;
    }

    private static Result toResult(JSONObject o) {
        boolean instrumental = o.optBoolean("instrumental", false);
        String plain = o.optString("plainLyrics", "");
        if ((plain == null || plain.isBlank() || "null".equals(plain)) && !instrumental) {
            String synced = o.optString("syncedLyrics", "");
            if (synced == null || synced.isBlank() || "null".equals(synced)) return null;
            plain = synced.replaceAll("(?m)^\\[[0-9:.]+]\\s?", "");
        }
        return new Result(o.optString("trackName", ""), o.optString("artistName", ""), plain, instrumental,
                "LRCLIB", null);
    }

    private String get(String url) throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(12))
                    .GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() == 200) return res.body();
            if (res.statusCode() == 404) return null;
            if (res.statusCode() == 503 || res.statusCode() == 429) {
                Thread.sleep(1200);
                continue;
            }
            log.debug("LRCLIB HTTP {}: {}", res.statusCode(), url);
            return null;
        }
        return null;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** Splits long lyrics into parts according to the Discord embed limit (at most ~3900 characters). */
    public static java.util.List<String> split(String text, int max) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        String content = text.trim();
        while (content.length() > max) {
            int index = content.lastIndexOf("\n\n", max);
            if (index <= 0) index = content.lastIndexOf("\n", max);
            if (index <= 0) index = content.lastIndexOf(" ", max);
            if (index <= 0) index = max;
            parts.add(content.substring(0, index).trim());
            content = content.substring(index).trim();
        }
        if (!content.isEmpty()) parts.add(content);
        return parts;
    }

    /** Converts the result into one or more embeds (each at most ~3900 characters). */
    public static java.util.List<net.dv8tion.jda.api.entities.MessageEmbed> toEmbeds(Result r, java.awt.Color color) {
        java.util.List<net.dv8tion.jda.api.entities.MessageEmbed> out = new java.util.ArrayList<>();
        String header = (r.artist.isEmpty() ? "" : r.artist + " - ") + r.title;
        if (header.length() > 250) header = header.substring(0, 249) + "…";
        String body = r.instrumental ? L.t("lyrics_service.track_instrumental_has_no_lyrics") : r.lyrics;
        java.util.List<String> parts = split(body, 3900);
        for (int i = 0; i < parts.size(); i++) {
            net.dv8tion.jda.api.EmbedBuilder eb = new net.dv8tion.jda.api.EmbedBuilder()
                    .setColor(color)
                    .setDescription(parts.get(i));
            if (i == 0) eb.setTitle("\uD83C\uDFA4 " + header, r.url);
            if (i == parts.size() - 1) {
                eb.setFooter(L.t("lyrics_service.source") + r.source + (parts.size() > 1 ? L.t("lyrics_service.page") + (i + 1) + "/" + parts.size() : ""));
            } else if (parts.size() > 1) {
                eb.setFooter(L.t("lyrics_service.page_2") + (i + 1) + "/" + parts.size());
            }
            out.add(eb.build());
        }
        return out;
    }
}
