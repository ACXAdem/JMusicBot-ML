/*
 * Copyright 2018-2020 Cosgy Dev
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.jagrosh.jmusicbot.audio;

import dev.cosgy.jmusicbot.i18n.L;

import com.jagrosh.jmusicbot.Bot;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import dev.cosgy.jmusicbot.spotify.SpotifyClient;
import dev.cosgy.jmusicbot.spotify.SpotifySourceManager;
import dev.cosgy.jmusicbot.util.YtDlpManager;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.*;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerManager extends DefaultAudioPlayerManager {
    private final Bot bot;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // yt-dlp
    private Path ytDlpPath;

    public PlayerManager(Bot bot) {
        this.bot = bot;
    }

    public void init() {
        try {
            Path botDir = Paths.get("").toAbsolutePath();
            YtDlpManager y = new YtDlpManager(botDir);
            this.ytDlpPath = y.prepare();
            y.startAutoUpdate(Duration.ofHours(6));
            logger.info(L.t("yt_dlp_manager.yt_dlp_ready"), ytDlpPath);
        } catch (Exception e) {
            logger.error(L.t("player_manager.could_not_start_yt_dlp"), e);
            this.ytDlpPath = null;
        }

        // ==== Source registration ====
        // The Spotify source must be registered FIRST; otherwise open.spotify.com links
        // are caught by the generic HTTP source and cannot be played.
        registerSourceManager(new SpotifySourceManager(this, new SpotifyClient(
                bot.getConfig().getSpotifyClientId(), bot.getConfig().getSpotifyClientSecret())));
        if (bot.getConfig().getSpotifyClientId().isEmpty() || bot.getConfig().getSpotifyClientSecret().isEmpty()) {
            logger.info(L.t("player_manager.spotify_no_api_keys_using"));
        } else {
            logger.info(L.t("player_manager.spotify_api_keys_found_full"));
        }

        if (bot.getConfig().isNicoNicoEnabled()) {
            registerSourceManager(
                    new com.sedmelluq.discord.lavaplayer.source.nico.NicoAudioSourceManager(
                            bot.getConfig().getNicoNicoEmailAddress(),
                            bot.getConfig().getNicoNicoPassword())
            );
        }

        YoutubeAudioSourceManager yt;
        if (bot.getConfig().isYouTubeOauth2Enabled()) {
            // OAuth only works with the TV client: TV is added at the END of the default clients.
            // So normal videos still play without an account; only when the others fail
            // (age restriction, "sign-in required") is the TV client tried with the Google account.
            List<dev.lavalink.youtube.clients.skeleton.Client> clients =
                    new ArrayList<>(Arrays.asList(new YoutubeAudioSourceManager(true).getClients()));
            clients.add(new Tv());
            yt = new YoutubeAudioSourceManager(true, clients.toArray(new dev.lavalink.youtube.clients.skeleton.Client[0]));
            YoutubeOauthStore.setup(yt, bot.getConfig().getYouTubeOauth2RefreshToken());
        } else {
            yt = new YoutubeAudioSourceManager(true);
        }
        yt.setPlaylistPageCount(10);
        registerSourceManager(yt);

        // Clean the cache according to the limit on startup
        Path cache = Paths.get("cache").toAbsolutePath();
        if (Files.isDirectory(cache)) trimCache(cache);

        AudioSourceManagers.registerRemoteSources(this);
        AudioSourceManagers.registerLocalSource(this);

        // Encoding and resampling quality
        if (getConfiguration().getOpusEncodingQuality() != 10) {
            logger.debug(L.t("player_manager.opusencodingquality_set_10_previous"), getConfiguration().getOpusEncodingQuality());
            getConfiguration().setOpusEncodingQuality(10);
        }
        if (getConfiguration().getResamplingQuality() != AudioConfiguration.ResamplingQuality.HIGH) {
            logger.debug(L.t("player_manager.resamplingquality_set_high_previous"), getConfiguration().getResamplingQuality().name());
            getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);
        }
    }

    public Bot getBot() { return bot; }

    public boolean hasHandler(Guild guild) {
        return guild.getAudioManager().getSendingHandler() != null;
    }

    public AudioHandler setUpHandler(Guild guild) {
        AudioHandler handler;
        if (guild.getAudioManager().getSendingHandler() == null) {
            AudioPlayer player = createPlayer();
            player.setVolume(bot.getSettingsManager().getSettings(guild).getVolume());
            handler = new AudioHandler(this, guild, player);
            player.addListener(handler);

            // Switch to the yt-dlp fallback if an error occurs during playback
            player.addListener(new YtDlpExceptionListener(this, player, handler));

            guild.getAudioManager().setSendingHandler(handler);
        } else {
            handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
        }
        return handler;
    }

    // =========================
    //  Loading stage: switch to the yt-dlp fallback if Lavaplayer fails
    // =========================
    @Override
    public Future<Void> loadItemOrdered(Object orderingKey, String identifier, AudioLoadResultHandler handler) {
        return super.loadItemOrdered(orderingKey, identifier, new AudioLoadResultHandler() {
            @Override public void trackLoaded(AudioTrack track) { handler.trackLoaded(track); }
            @Override public void playlistLoaded(AudioPlaylist playlist) { handler.playlistLoaded(playlist); }

            @Override
            public void noMatches() {
                if (shouldFallbackToYtDlp(identifier)) {
                    tryFallbackDownload(orderingKey, identifier, handler, null);
                } else handler.noMatches();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                if (shouldFallbackToYtDlp(identifier)) {
                    tryFallbackDownload(orderingKey, identifier, handler, exception);
                } else handler.loadFailed(exception);
            }
        });
    }

    boolean shouldFallbackToYtDlp(String identifier) {
        if (ytDlpPath == null || identifier == null) return false;
        String id = identifier.toLowerCase(Locale.ROOT);
        if (id.startsWith("ytsearch:")) return false;
        if (id.startsWith("http://") || id.startsWith("https://")) {
            return id.contains("youtube.com/") || id.contains("youtu.be/");
        }
        return id.matches("^[a-zA-Z0-9_-]{11}$"); // plain YouTube video ID (11 characters)
    }

    private void tryFallbackDownload(Object orderingKey,
                                     String identifier,
                                     AudioLoadResultHandler handler,
                                     FriendlyException cause) {
        logger.warn(L.t("player_manager.could_not_load_from_youtube"), identifier);
        try {
            Path out = downloadViaYtDlp(identifier);
            if (out == null || !Files.isRegularFile(out))
                throw new IllegalStateException(L.t("player_manager.yt_dlp_output_not_found") + out);

            // LocalSource expects an absolute file path instead of file://
            super.loadItemOrdered(orderingKey, out.toAbsolutePath().toString(), handler);
        } catch (Exception ex) {
            logger.error(L.t("player_manager.yt_dlp_fallback_failed"), ex.toString());
            if (cause != null) {
                handler.loadFailed(new FriendlyException(
                        L.t("player_manager.could_not_load_from_youtube_2") + ex.getMessage(),
                        FriendlyException.Severity.SUSPICIOUS, cause));
            } else {
                handler.loadFailed(new FriendlyException(
                        L.t("player_manager.not_found_youtube_yt_dlp") + ex.getMessage(),
                        FriendlyException.Severity.SUSPICIOUS, ex));
            }
        }
    }

    /** Path of the yt-dlp file (null if not ready). */
    public Path getYtDlpPath() {
        return ytDlpPath;
    }

    /** Is yt-dlp available? */
    public boolean isYtDlpAvailable() {
        return ytDlpPath != null;
    }

    public Path downloadViaYtDlp(String input) throws Exception {
        Path botRoot = Paths.get("").toAbsolutePath().normalize();
        Path cacheDir = botRoot.resolve("cache");
        Files.createDirectories(cacheDir);

        String url = toYoutubeUrl(input);
        // Don't download again if the file was downloaded before
        String cachedId = tryExtractYoutubeId(url);
        if (cachedId != null) {
            for (String ext : new String[]{"webm", "m4a", "opus", "mp4", "mp3"}) {
                Path cached = cacheDir.resolve(cachedId + "." + ext);
                if (Files.isRegularFile(cached) && Files.size(cached) > 0) {
                    logger.info(L.t("player_manager.using_file_from_yt_dlp"), cached);
                    try { Files.setLastModifiedTime(cached, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis())); } catch (Exception ignored) {}
                    return cached;
                }
            }
        }
        logger.info(L.t("player_manager.downloading_with_yt_dlp"), url);

        List<String> cmd = new ArrayList<>();
        cmd.add(ytDlpPath.toString());

        // Prefer formats Lavaplayer can read without conversion (webm/opus → m4a/aac → others)
        Collections.addAll(cmd,
                "--no-playlist",
                "--ignore-config",
                "--no-progress",
                "--newline",
                "--restrict-filenames",
                "--force-overwrites",
                "-f", "bestaudio[ext=webm][acodec=opus]/bestaudio[ext=m4a]/bestaudio",
                "--no-post-overwrites",
                "--output", cacheDir.resolve("%(id)s.%(ext)s").toString(),
                "--print", "after_move:filepath"
        );

        // If there is a cookie file (cookies.txt, Netscape format), yt-dlp signs in to YouTube with that account:
        // age-restricted / "sign-in required" videos can be downloaded too.
        Path cookies = cookiesFile();
        if (cookies != null) {
            Collections.addAll(cmd, "--cookies", cookies.toString());
        }

        cmd.add(url);

        try {
            Path out = runYtDlpDownload(cmd, botRoot, url, cacheDir);
            trimCache(cacheDir);
            return out;
        } catch (YtDlpCrash e) {
            // 255 = yt-dlp could not start at all (usually the temp folder is full). Clean up leftovers and try once more.
            logger.warn(L.t("player_manager.yt_dlp_could_not_start"), e.getMessage());
            YtDlpManager.cleanStaleTemp(true);
            Path out = runYtDlpDownload(cmd, botRoot, url, cacheDir);
            trimCache(cacheDir);
            return out;
        }
    }

    /** yt-dlp could not be started (exit code 255). */
    private static final class YtDlpCrash extends RuntimeException {
        YtDlpCrash(String m) { super(m); }
    }

    /** If the cache folder exceeds cachelimitmb in config.txt, the least recently played tracks are deleted. */
    void trimCache(Path cacheDir) {
        final long CACHE_LIMIT = bot.getConfig().getCacheLimitMb() * 1024 * 1024;
        try (var st = Files.list(cacheDir)) {
            List<Path> files = new ArrayList<>();
            for (Path p : (Iterable<Path>) st::iterator) if (Files.isRegularFile(p)) files.add(p);
            long total = 0;
            for (Path p : files) total += Files.size(p);
            if (total <= CACHE_LIMIT) return;
            files.sort(Comparator.comparingLong(p -> { try { return Files.getLastModifiedTime(p).toMillis(); } catch (Exception e) { return 0L; } }));
            int removed = 0;
            for (Path p : files) {
                if (total <= CACHE_LIMIT * 8 / 10) break;
                long sz = Files.size(p);
                if (Files.deleteIfExists(p)) { total -= sz; removed++; }
            }
            logger.info(L.t("player_manager.cache_limit_mb_exceeded_deleted"),
                    CACHE_LIMIT / (1024 * 1024), removed, total / (1024 * 1024));
        } catch (Exception e) {
            logger.debug(L.t("player_manager.could_not_clean_cache"), e.toString());
        }
    }

    private Path runYtDlpDownload(List<String> cmd, Path botRoot, String url, Path cacheDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(botRoot.toFile());
        pb.redirectErrorStream(true);
        // Temp folder (on disk) and UTF-8 output
        YtDlpManager.applyEnv(pb);

        Process proc = pb.start();

        String lastNonEmpty = null;
        String errorLine = null;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.isBlank()) lastNonEmpty = line.trim();
                if (line.startsWith("ERROR:")) errorLine = line.trim();
                logger.debug("[yt-dlp] {}", line);
            }
        }

        if (!proc.waitFor(600, TimeUnit.SECONDS)) {
            proc.destroyForcibly();
            throw new RuntimeException(L.t("player_manager.yt_dlp_timed_out_600"));
        }
        if (proc.exitValue() != 0) {
            String why = errorLine != null ? errorLine : lastNonEmpty;
            logger.warn(L.t("player_manager.yt_dlp_error"), why);
            if (proc.exitValue() == 255) throw new YtDlpCrash("exit code=255" + (why == null ? "" : " (" + why + ")"));
            throw new RuntimeException("yt-dlp exit code=" + proc.exitValue() + (why == null ? "" : " (" + why + ")"));
        }

        if (lastNonEmpty == null) {
            String id = tryExtractYoutubeId(url);
            if (id == null) throw new IllegalStateException(L.t("player_manager.output_path_unknown_print_empty"));
            // Look for known extensions
            Path guessWebm = cacheDir.resolve(id + ".webm");
            if (Files.isRegularFile(guessWebm)) return guessWebm;
            Path guessM4a = cacheDir.resolve(id + ".m4a");
            if (Files.isRegularFile(guessM4a)) return guessM4a;
            throw new FileNotFoundException(L.t("player_manager.output_file_not_found"));
        }

        Path out = Paths.get(lastNonEmpty);
        if (!out.isAbsolute()) out = botRoot.resolve(out).normalize();
        if (!Files.isRegularFile(out)) throw new FileNotFoundException(L.t("player_manager.output_file_missing") + out);
        logger.debug(L.t("player_manager.yt_dlp_finished"), out);
        return out;
    }

    /** cookies.txt in the bot folder (null if missing). */
    public static Path cookiesFile() {
        Path p = Paths.get("cookies.txt").toAbsolutePath();
        try {
            return Files.isRegularFile(p) && Files.size(p) > 0 ? p : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Turns a YouTube/yt-dlp error message into a reason shown to the user (in the bot language). */
    public static String explainYoutubeFailure(String msg) {
        return explainYoutubeFailure(msg, true);
    }

    /** @param adminHint false: don't add admin setup hints (for DMs) */
    public static String explainYoutubeFailure(String msg, boolean adminHint) {
        String m = msg == null ? "" : msg.toLowerCase(Locale.ROOT);
        boolean login = cookiesFile() != null;
        if (m.contains("exit code=255") || m.contains("[pyi-") || m.contains("no space left"))
            return L.t("player_manager.fallback_downloader_yt_dlp_server");
        if (m.contains("confirm your age") || m.contains("age-restricted") || m.contains("age restricted")
                || m.contains("inappropriate for some users") || m.contains("requires login") || m.contains("login required"))
            return L.t("player_manager.youtube_requires_sign_age_verification")
                    + (login ? L.t("player_manager.account_cookies_txt_was_not")
                    : adminHint ? L.t("player_manager.play_give_bot_youtube_account") : "");
        if (m.contains("not a bot")) return L.t("player_manager.youtube_thought_server_was_bot") + (login ? "" : L.t("player_manager.adding_cookies_txt_usually_fixes"));
        if (m.contains("private video")) return L.t("player_manager.video_private");
        if (m.contains("in your country") || m.contains("geo restrict") || m.contains("geo-restrict"))
            return L.t("player_manager.video_blocked_server_s_country");
        if (m.contains("members-only") || m.contains("join this channel")) return L.t("player_manager.video_only_available_channel_members");
        if (m.contains("premium")) return L.t("player_manager.video_only_available_youtube_premium");
        if (m.contains("removed") || m.contains("unavailable") || m.contains("does not exist") || m.contains("terminated"))
            return L.t("player_manager.video_was_removed_inaccessible");
        return L.t("player_manager.youtube_did_not_provide_video");
    }

    private String toYoutubeUrl(String input) {
        String s = input == null ? "" : input.trim();
        if (s.startsWith("http://") || s.startsWith("https://")) return s;
        return "https://www.youtube.com/watch?v=" + s;
    }

    private String tryExtractYoutubeId(String url) {
        try {
            int vIndex = url.indexOf("v=");
            if (vIndex >= 0) {
                String v = url.substring(vIndex + 2);
                int amp = v.indexOf('&');
                return amp > 0 ? v.substring(0, amp) : v;
            }
            int idx = url.indexOf("youtu.be/");
            if (idx >= 0) {
                String v = url.substring(idx + "youtu.be/".length());
                int q = v.indexOf('?');
                return q > 0 ? v.substring(0, q) : v;
            }
            idx = url.indexOf("/shorts/");
            if (idx >= 0) {
                String v = url.substring(idx + "/shorts/".length());
                int q = v.indexOf('?');
                return q > 0 ? v.substring(0, q) : v;
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ======== For carrying info when a track is replaced ========
    static final class TrackContext {
        final AudioTrackInfo originalInfo; // Original (YouTube etc.) track info
        final Object userData;             // Existing user data such as RequestMetadata
        TrackContext(AudioTrackInfo info, Object userData) {
            this.originalInfo = info;
            this.userData = userData;
        }
    }

    // Copy the original track's info to the new local track
    void applyReplacementContext(AudioTrack newTrack, AudioTrack oldTrack) {
        Object ud = oldTrack.getUserData(); // RequestMetadata vb.
        newTrack.setUserData(new TrackContext(oldTrack.getInfo(), ud));
    }

    // ==========================================================
    // Error during playback → yt-dlp fallback
    // ==========================================================
    private static class YtDlpExceptionListener extends AudioEventAdapter {
        private final PlayerManager pm;
        private final AudioPlayer player;
        private final AudioHandler handler;
        private final AtomicBoolean fallingBack = new AtomicBoolean(false);

        YtDlpExceptionListener(PlayerManager pm, AudioPlayer player, AudioHandler handler) {
            this.pm = pm;
            this.player = player;
            this.handler = handler;
        }

        @Override
        public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
            String id = track != null ? track.getIdentifier() : null;
            final String ytError = fullMessage(exception);
            pm.logger.warn(L.t("player_manager.could_not_open_youtube_stream"), id, summarize(ytError),
                    pm.shouldFallbackToYtDlp(id) ? L.t("player_manager.trying_yt_dlp") : "");

            if (track == null || !pm.shouldFallbackToYtDlp(id)) return; // Not YouTube: move to the next track normally

            // If another fallback download is running, don't wait for it; move to the next track normally
            if (!fallingBack.compareAndSet(false, true)) {
                pm.logger.warn(L.t("player_manager.another_yt_dlp_download_running"), id);
                return;
            }

            // Ignore the next onTrackEnd once: the same track will be played when the fallback file is ready
            handler.suppressAutoLeaveOnce();
            final RequestMetadata rm = handler.getRequestMetadata().getOwner() != 0L ? handler.getRequestMetadata()
                    : (track.getUserData() instanceof RequestMetadata ? (RequestMetadata) track.getUserData() : RequestMetadata.EMPTY);

            handler.setPreparingTitle(track.getInfo().title);
            final long session = handler.getSession();
            CompletableFuture.runAsync(() -> {
                boolean started = false;
                try {
                    // Wait until the failed track has fully left the player (so onTrackEnd is handled);
                    // otherwise a file coming instantly from the cache is mistaken for "another track is playing" and queued.
                    for (int i = 0; i < 150 && player.getPlayingTrack() == track; i++) Thread.sleep(20);
                    Path out = pm.downloadViaYtDlp(id);
                    if (out == null || !Files.isRegularFile(out))
                        throw new IllegalStateException(L.t("player_manager.yt_dlp_output_not_found") + out);

                    pm.logger.info(L.t("player_manager.yt_dlp_fallback_succeeded_playing"), out);
                    AudioItem item = pm.loadItemSync(out.toAbsolutePath().toString());
                    AudioTrack newTrack = item instanceof AudioTrack ? (AudioTrack) item
                            : item instanceof AudioPlaylist && !((AudioPlaylist) item).getTracks().isEmpty()
                            ? ((AudioPlaylist) item).getTracks().get(0) : null;
                    if (newTrack == null) throw new IllegalStateException(L.t("player_manager.could_not_load_local_file") + out);
                    if (handler.getSession() != session) {
                        pm.logger.info(L.t("player_manager.music_was_stopped_meantime_downloaded"), out);
                        started = true; // don't move to the next either: the user stopped
                        return;
                    }

                    if (player.getPlayingTrack() == null) {
                        pm.applyReplacementContext(newTrack, track);
                        player.startTrack(newTrack, false);
                    } else {
                        // If another track started in the meantime, don't interrupt it; put the fallback (with its title) at the front of the queue
                        QueuedTrack qt = new QueuedTrack(newTrack, rm);
                        pm.applyReplacementContext(newTrack, track);
                        handler.getQueue().addAt(0, qt);
                    }
                    started = true;
                } catch (Exception ex) {
                    pm.logger.error(L.t("player_manager.yt_dlp_fallback_during_playback"), ex.toString());
                    if (handler.getSession() == session) notifyFailure(track, ytError + "\n" + ex.getMessage(), rm.getOwner());
                } finally {
                    fallingBack.set(false);
                    if (handler.getSession() == session) {
                        handler.setPreparingTitle(null);
                        if (!started) handler.advanceAfterFailure();
                    }
                }
            });
        }

        /**
         * Reports a track that couldn't be played. config.txt → failnotice:
         * "none" (console only), "dm" (direct message to the requester), "channel" (channel of the last command).
         */
        private void notifyFailure(AudioTrack track, String error, long requester) {
            String mode = pm.bot.getConfig().getFailNotice();
            pm.logger.warn(L.t("player_manager.could_not_play_track_id"), track.getIdentifier(), explainYoutubeFailure(error).replace("**", ""));
            if (mode.equals("none")) return;
            try {
                if (mode.equals("dm")) {
                    if (requester == 0L || pm.bot.getJDA() == null) return;
                    String title = AudioHandler.displayInfo(track).title;
                    if (title == null || title.isBlank() || title.equals("Unknown title")) title = track.getIdentifier();
                    String text = "⚠️ **" + com.jagrosh.jmusicbot.utils.FormatUtil.filter(title.length() > 120 ? title.substring(0, 119) + "…" : title)
                            + L.t("player_manager.could_not_played") + explainYoutubeFailure(error, false);
                    pm.bot.getJDA().retrieveUserById(requester).queue(u -> u.openPrivateChannel().queue(
                            pc -> pc.sendMessage(text).queue(null, t -> { }), t -> { }), t -> { });
                    return;
                }
                net.dv8tion.jda.api.entities.Guild guild = pm.bot.getJDA() == null ? null : pm.bot.getJDA().getGuildById(handler.getGuildId());
                if (guild == null) return;
                String title = AudioHandler.displayInfo(track).title;
                if (title == null || title.isBlank() || title.equals("Unknown title")) title = track.getIdentifier();
                String text = "⚠️ **" + com.jagrosh.jmusicbot.utils.FormatUtil.filter(title.length() > 120 ? title.substring(0, 119) + "…" : title)
                        + L.t("player_manager.could_not_played") + explainYoutubeFailure(error)
                        + (handler.getQueue().isEmpty() ? "" : L.t("player_manager.moving_next_track"));
                dev.cosgy.jmusicbot.util.LastSendTextChannel.SendMessage(guild, text);
            } catch (Exception e) {
                pm.logger.debug(L.t("player_manager.could_not_send_failure_notice"), e.toString());
            }
        }

        /** Turns the "Client [ANDROID_VR] failed: ..." lines into a short one-line summary. */
        static String summarize(String msg) {
            if (msg == null || msg.isBlank()) return L.t("player_manager.unknown_error");
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("Client \\[([A-Z0-9_]+)\\] failed: ([^\\n]*)").matcher(msg);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                String reason = m.group(2).trim().replaceAll("\\s*:\\s*$", "");
                // the library writes the reason twice: "X.X." / "X: X: "
                int half = reason.length() / 2;
                if (reason.length() % 2 == 0 && reason.substring(0, half).equals(reason.substring(half))) reason = reason.substring(0, half);
                reason = reason.trim();
                String low = reason.toLowerCase(java.util.Locale.ROOT);
                if (low.contains("requires login") || low.contains("sign in to confirm your age")) reason = L.t("player_manager.sign_required");
                else if (low.contains("not a bot")) reason = L.t("player_manager.bot_verification");
                else if (low.contains("no supported audio streams")) reason = L.t("player_manager.no_audio_stream");
                else if (low.contains("video is unavailable")) reason = L.t("player_manager.unavailable");
                else if (low.contains("private video")) reason = L.t("player_manager.private_video");
                if (sb.length() > 0) sb.append(", ");
                sb.append(m.group(1)).append(": ").append(reason);
            }
            if (sb.length() == 0) {
                String first = msg.strip().split("\\R", 2)[0];
                return first.length() > 300 ? first.substring(0, 300) + "…" : first;
            }
            return sb.toString();
        }

        private static String fullMessage(Throwable t) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; t != null && i < 6; i++, t = t.getCause()) {
                if (t.getMessage() != null) sb.append(t.getMessage()).append('\n');
                for (Throwable s : t.getSuppressed()) if (s.getMessage() != null) sb.append(s.getMessage()).append('\n');
            }
            return sb.toString();
        }

        @Override
        public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
            if (track == null) return;
            String id = track.getIdentifier();
            if (pm.shouldFallbackToYtDlp(id)) {
                pm.logger.warn(L.t("player_manager.track_got_stuck_trying_yt"), id, thresholdMs);
                onTrackException(player, track, new FriendlyException("stuck " + thresholdMs + "ms",
                        FriendlyException.Severity.SUSPICIOUS, null));
            }
        }
    }
}
