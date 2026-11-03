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

import dev.cosgy.jmusicbot.i18n.L;

import com.jagrosh.jmusicbot.audio.PlayerManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spotify source for Lavaplayer.
 *
 * <p>Since Spotify does not provide audio files, this is a "mirror" source:
 * track info (name, artist, duration, ISRC) is read from Spotify, and when its turn comes
 * the same track is found on YouTube Music / YouTube and played from there.</p>
 *
 * <p>Thanks to this source, Spotify links work everywhere in the bot: /play, !play, /spotify, saved playlist files
 * and the cache.</p>
 */
public class SpotifySourceManager implements AudioSourceManager {
    private static final Logger log = LoggerFactory.getLogger("Spotify");

    /** A YouTube Music result above this score is accepted directly. */
    private static final double GOOD_SCORE = 70;
    /** If no result reaches this score, there is no match. */
    private static final double MIN_SCORE = 25;

    private static final java.util.regex.Pattern PODCAST = java.util.regex.Pattern.compile(
            "^(?:https?://)?open\\.spotify\\.com/(?:intl-[a-z]{2}(?:[-_][a-z]{2,4})?/)?(?:show|episode)/|^spotify:(?:show|episode):",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    private final PlayerManager playerManager;
    private final SpotifyClient client;

    /** Spotify track -> YouTube link cache (to avoid searching the same track again). */
    private final Map<String, String> matchCache = Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 5000;
        }
    });

    private final ExecutorService prefetcher = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "spotify-onyukleme-" + PREFETCH_COUNTER.incrementAndGet());
        t.setDaemon(true);
        return t;
    });
    private static final AtomicInteger PREFETCH_COUNTER = new AtomicInteger();

    public SpotifySourceManager(PlayerManager playerManager, SpotifyClient client) {
        this.playerManager = playerManager;
        this.client = client;
    }

    public SpotifyClient getClient() {
        return client;
    }

    @Override
    public String getSourceName() {
        return "spotify";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        String identifier = SpotifyLink.clean(reference.identifier);
        try {
            if (identifier.toLowerCase(Locale.ROOT).startsWith("spsearch:")) {
                return search(identifier.substring("spsearch:".length()).trim());
            }

            if (PODCAST.matcher(identifier).find()) {
                throw new SpotifyException(L.t("spotify_source_manager.spotify_podcast_episodes_are_not"));
            }

            SpotifyLink link = SpotifyLink.parse(identifier);
            if (link == null && SpotifyLink.isShortLink(identifier)) {
                link = client.resolveShortLink(identifier);
            }
            if (link == null) return null; // Not a Spotify link, let other sources try

            SpotifyCollection collection = client.load(link);
            log.info(L.t("spotify_source_manager.spotify_loaded_tracks_source"), link.type.turkish(), collection.name,
                    collection.tracks.size(), collection.source);

            if (link.type == SpotifyLink.Type.TRACK) {
                SpotifyTrackData data = collection.tracks.get(0);
                prefetch(data);
                return createTrack(data);
            }

            List<AudioTrack> tracks = new ArrayList<>();
            for (SpotifyTrackData data : collection.tracks) {
                tracks.add(createTrack(data.withImageIfMissing(collection.imageUrl)));
            }
            // Match the first two tracks in the background so playback starts quickly
            for (int i = 0; i < Math.min(2, collection.tracks.size()); i++) prefetch(collection.tracks.get(i));
            return new SpotifyAudioPlaylist(collection, tracks, false);
        } catch (SpotifyException e) {
            throw new FriendlyException(e.getMessage(), FriendlyException.Severity.COMMON, e);
        }
    }

    private AudioItem search(String query) throws SpotifyException {
        if (query.isEmpty()) return AudioReference.NO_TRACK;
        List<SpotifyTrackData> results = client.search(query, 5);
        if (results.isEmpty()) return AudioReference.NO_TRACK;
        List<AudioTrack> tracks = new ArrayList<>();
        for (SpotifyTrackData d : results) tracks.add(createTrack(d));
        SpotifyCollection c = new SpotifyCollection(null, L.t("spotify_source_manager.spotify_search") + query, "", null, results, results.size(), "api");
        return new SpotifyAudioPlaylist(c, tracks, true);
    }

    SpotifyMirrorTrack createTrack(SpotifyTrackData data) {
        String url = data.url() != null ? data.url() : "spotify:" + data.cacheKey();
        AudioTrackInfo info = new AudioTrackInfo(
                data.title,
                data.artists,
                data.durationMs, // 0 if unknown (the YouTube duration is used while playing)
                url,
                false,
                url,
                data.imageUrl,
                data.isrc);
        return new SpotifyMirrorTrack(info, data, this);
    }

    // =====================================================================
    //  YouTube matching
    // =====================================================================

    private void prefetch(SpotifyTrackData data) {
        if (matchCache.containsKey(data.cacheKey())) return;
        try {
            prefetcher.submit(() -> {
                try {
                    findPlayable(data);
                } catch (Exception e) {
                    log.debug(L.t("spotify_source_manager.preload_failed"), e.getMessage());
                }
            });
        } catch (Exception ignored) {
            // the pool may have been shut down
        }
    }

    /**
     * Finds the playable YouTube track that matches the Spotify track.
     *
     * @return the loaded AudioTrack, or null if not found
     */
    AudioTrack findPlayable(SpotifyTrackData data) {
        String cached = matchCache.get(data.cacheKey());
        if (cached != null) {
            AudioTrack t = loadSingle(cached);
            if (t != null) return t;
            matchCache.remove(data.cacheKey());
        }

        String title = SpotifyMatcher.cleanTitle(data.title);
        String artist = data.primaryArtist;
        String base = (artist.isEmpty() ? "" : artist + " - ") + title;

        AudioTrack best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        // 1) YouTube Music search by ISRC: the most reliable way to get the right recording
        if (data.isrc != null) {
            for (AudioTrack c : searchCandidates("ytmsearch:\"" + data.isrc + "\"", 5)) {
                if (data.durationMs > 0 && SpotifyMatcher.durationDiff(c.getDuration(), data) > 5_000) continue;
                double s = SpotifyMatcher.score(c.getInfo().title, c.getInfo().author, c.getDuration(), data) + 15;
                if (s > bestScore) {
                    best = c;
                    bestScore = s;
                }
            }
            if (best != null && bestScore >= GOOD_SCORE) return remember(data, best, bestScore, "ISRC");
        }

        // 2) "artist - title" search on YouTube Music (official audio recordings)
        for (AudioTrack c : searchCandidates("ytmsearch:" + base, 8)) {
            double s = SpotifyMatcher.score(c.getInfo().title, c.getInfo().author, c.getDuration(), data);
            if (s > bestScore) {
                best = c;
                bestScore = s;
            }
        }
        if (best != null && bestScore >= GOOD_SCORE) return remember(data, best, bestScore, "YouTube Music");

        // 3) Normal YouTube search
        for (AudioTrack c : searchCandidates("ytsearch:" + base + " audio", 8)) {
            double s = SpotifyMatcher.score(c.getInfo().title, c.getInfo().author, c.getDuration(), data) - 3;
            if (s > bestScore) {
                best = c;
                bestScore = s;
            }
        }
        if (best != null && bestScore >= MIN_SCORE) return remember(data, best, bestScore, "YouTube");

        log.warn(L.t("spotify_source_manager.no_youtube_match_found_spotify"), data, bestScore);
        return null;
    }

    private AudioTrack remember(SpotifyTrackData data, AudioTrack track, double score, String via) {
        String url = playableUrl(track);
        if (url != null) matchCache.put(data.cacheKey(), url);
        log.info(L.t("spotify_source_manager.matched_score"), via, Math.round(score), data, track.getInfo().title, url);
        return track;
    }

    /** Reloadable link of the track from the search. */
    static String playableUrl(AudioTrack track) {
        String src = track.getSourceManager() == null ? "" : track.getSourceManager().getSourceName();
        if (src.toLowerCase(Locale.ROOT).contains("youtube")) {
            return "https://www.youtube.com/watch?v=" + track.getIdentifier();
        }
        return track.getInfo().uri;
    }

    private List<AudioTrack> searchCandidates(String query, int max) {
        try {
            AudioItem item = playerManager.loadItemSync(query);
            if (item instanceof AudioPlaylist) {
                List<AudioTrack> list = ((AudioPlaylist) item).getTracks();
                return list.subList(0, Math.min(max, list.size()));
            }
            if (item instanceof AudioTrack) return Collections.singletonList((AudioTrack) item);
        } catch (Exception e) {
            log.debug(L.t("spotify_source_manager.search_failed"), query, e.getMessage());
        }
        return Collections.emptyList();
    }

    private AudioTrack loadSingle(String identifier) {
        try {
            AudioItem item = playerManager.loadItemSync(identifier);
            if (item instanceof AudioTrack) return (AudioTrack) item;
            if (item instanceof AudioPlaylist) {
                AudioPlaylist p = (AudioPlaylist) item;
                if (p.getSelectedTrack() != null) return p.getSelectedTrack();
                if (!p.getTracks().isEmpty()) return p.getTracks().get(0);
            }
        } catch (Exception e) {
            log.debug(L.t("youtube_mix_expander.could_not_load"), identifier, e.getMessage());
        }
        return null;
    }

    /** If the YouTube stream can't be opened, downloads it with yt-dlp and plays the local file. */
    AudioTrack loadViaYtDlp(String youtubeUrl) {
        if (youtubeUrl == null || !playerManager.isYtDlpAvailable()) return null;
        try {
            Path file = playerManager.downloadViaYtDlp(youtubeUrl);
            if (file == null || !Files.isRegularFile(file)) return null;
            return loadSingle(file.toAbsolutePath().toString());
        } catch (Exception e) {
            log.warn(L.t("spotify_source_manager.could_not_download_with_yt"), e.getMessage());
            return null;
        }
    }

    // =====================================================================
    //  AudioSourceManager
    // =====================================================================

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return false;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) {
        throw new UnsupportedOperationException(L.t("spotify_source_manager.spotify_tracks_cannot_encoded"));
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) {
        return null;
    }

    @Override
    public void shutdown() {
        prefetcher.shutdownNow();
    }
}
