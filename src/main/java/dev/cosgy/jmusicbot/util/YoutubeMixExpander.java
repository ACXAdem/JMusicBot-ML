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
package dev.cosgy.jmusicbot.util;

import dev.cosgy.jmusicbot.i18n.L;

import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * YouTube Mix / Radio playlists (list=RD..., start_radio=1) only return ~25 tracks on the first load.
 * This class fetches the rest of the list with yt-dlp and adds the remaining tracks to the queue in the background.
 */
public final class YoutubeMixExpander {
    private static final Logger log = LoggerFactory.getLogger("YouTubeMix");
    /** Maximum number of tracks taken from one radio playlist. */
    public static final int MAX_TRACKS = 75;
    private static final Pattern VIDEO_ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "youtube-mix");
        t.setDaemon(true);
        return t;
    });

    private YoutubeMixExpander() {
    }

    /** Function that loads a track from a video ID (can be replaced in tests). */
    static java.util.function.BiFunction<Bot, String, AudioTrack> loader = YoutubeMixExpander::load;

    /** Is the link a YouTube Mix / Radio playlist? */
    public static boolean isMix(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.ROOT);
        return (u.contains("youtube.com") || u.contains("youtu.be")) && (u.contains("list=rd") || u.contains("start_radio=1"));
    }

    /**
     * Adds the rest of the list to the queue in the background.
     *
     * @param alreadyQueued video IDs from this list that are already queued
     */
    public static void expand(Bot bot, Guild guild, User user, String url, Set<String> alreadyQueued, MessageChannel channel) {
        Path ytDlp = bot.getPlayerManager().getYtDlpPath();
        if (ytDlp == null) return;
        Set<String> seen = new HashSet<>(alreadyQueued);
        AudioHandler startHandler = (AudioHandler) guild.getAudioManager().getSendingHandler();
        if (startHandler == null) return;
        final long session = startHandler.getSession();
        POOL.submit(() -> {
            try {
                List<String> ids = listIds(ytDlp, url);
                int added = 0;
                AudioHandler handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
                for (String id : ids) {
                    if (seen.size() >= MAX_TRACKS) break;
                    // Stop adding if music was stopped in the meantime (or the bot left the channel)
                    if (handler == null || handler.getSession() != session) {
                        log.info(L.t("youtube_mix_expander.music_was_stopped_not_adding"));
                        return;
                    }
                    if (!seen.add(id)) continue;
                    AudioTrack track = loader.apply(bot, id);
                    if (track == null || bot.getConfig().isTooLong(track)) continue;
                    if (handler.getSession() != session || !guild.getSelfMember().getVoiceState().inAudioChannel()) {
                        log.info(L.t("youtube_mix_expander.music_was_stopped_not_adding"));
                        return;
                    }
                    handler.addTrack(new QueuedTrack(track, user));
                    added++;
                }
                log.info(L.t("youtube_mix_expander.added_more_tracks_from_youtube"), added, url);
                if (added > 0 && channel != null) {
                    channel.sendMessage(L.t("youtube_mix_expander.from_rest_radio_playlist") + added + L.t("youtube_mix_expander.more_tracks_added_queue")).queue(null, t -> { });
                }
                bot.getNowplayingHandler().refresh(guild.getIdLong());
            } catch (Exception e) {
                log.warn(L.t("youtube_mix_expander.could_not_expand_youtube_radio"), e.toString());
            }
        });
    }

    /** Returns the video ID (only for YouTube tracks). */
    public static String videoId(AudioTrack track) {
        String id = track.getIdentifier();
        return id != null && VIDEO_ID.matcher(id).matches() ? id : null;
    }

    private static List<String> listIds(Path ytDlp, String url) throws Exception {
        List<String> cmd = List.of(ytDlp.toString(), "--flat-playlist", "--ignore-config", "--no-warnings",
                "--playlist-end", String.valueOf(MAX_TRACKS), "--print", "%(id)s", url);
        ProcessBuilder pb = YtDlpManager.applyEnv(new ProcessBuilder(cmd).redirectErrorStream(true));
        Process p = pb.start();
        List<String> ids = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String s = line.trim();
                if (VIDEO_ID.matcher(s).matches()) ids.add(s);
                else if (!s.isEmpty()) log.debug("[yt-dlp] {}", s);
            }
        }
        if (!p.waitFor(90, TimeUnit.SECONDS)) p.destroyForcibly();
        return ids;
    }

    private static AudioTrack load(Bot bot, String id) {
        try {
            AudioItem item = bot.getPlayerManager().loadItemSync("https://www.youtube.com/watch?v=" + id);
            if (item instanceof AudioTrack) return (AudioTrack) item;
            if (item instanceof AudioPlaylist) {
                AudioPlaylist pl = (AudioPlaylist) item;
                if (pl.getSelectedTrack() != null) return pl.getSelectedTrack();
                if (!pl.getTracks().isEmpty()) return pl.getTracks().get(0);
            }
        } catch (Exception e) {
            log.debug(L.t("youtube_mix_expander.could_not_load"), id, e.getMessage());
        }
        return null;
    }
}
