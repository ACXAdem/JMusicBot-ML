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
import com.jagrosh.jmusicbot.JMusicBot;
import com.jagrosh.jmusicbot.PlayStatus;
import com.jagrosh.jmusicbot.playlist.PlaylistLoader.Playlist;
import com.jagrosh.jmusicbot.queue.FairQueue;
import com.jagrosh.jmusicbot.settings.Settings;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import dev.cosgy.jmusicbot.settings.RepeatMode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author John Grosh
 */
public class AudioHandler extends AudioEventAdapter implements AudioSendHandler {
    private final FairQueue<QueuedTrack> queue = new FairQueue<>();
    private final List<AudioTrack> defaultQueue = new LinkedList<>();
    private final Set<String> votes = new HashSet<>();
    /** Recently played tracks for the "previous track" button (at most 25). */
    private final java.util.Deque<AudioTrack> history = new java.util.ArrayDeque<>();
    private static final int HISTORY_LIMIT = 25;
    private final PlayerManager manager;
    private final AudioPlayer audioPlayer;
    private final long guildId;
    private final String stringGuildId;
    private AudioFrame lastFrame;

    // Flag to "suppress leaving only once" during a replacement
    private final AtomicBoolean suppressAutoLeaveOnce = new AtomicBoolean(false);

    protected AudioHandler(PlayerManager manager, Guild guild, AudioPlayer player) {
        this.manager = manager;
        this.audioPlayer = player;
        this.guildId = guild.getIdLong();
        this.stringGuildId = guild.getId();
    }

    // Called right before the replacement
    public void suppressAutoLeaveOnce() {
        this.suppressAutoLeaveOnce.set(true);
    }

    /** True if nothing is playing AND no fallback download is running. */
    private boolean isIdle() {
        return audioPlayer.getPlayingTrack() == null && preparingTitle == null;
    }

    public int addTrackToFront(QueuedTrack qtrack) {
        if (isIdle()) {
            audioPlayer.playTrack(qtrack.getTrack());
            return -1;
        } else {
            queue.addAt(0, qtrack);
            return 0;
        }
    }

    public int addTrack(QueuedTrack qtrack) {
        if (isIdle()) {
            audioPlayer.playTrack(qtrack.getTrack());
            return -1;
        } else {
            boolean toEnt = manager.getBot().getSettingsManager().getSettings(guildId).isForceToEndQue();
            return queue.add(qtrack, toEnt);
        }
    }

    public void addTrackIfRepeat(AudioTrack track) {
        RepeatMode mode = manager.getBot().getSettingsManager().getSettings(guildId).getRepeatMode();
        boolean toEnt = manager.getBot().getSettingsManager().getSettings(guildId).isForceToEndQue();
        if (mode != RepeatMode.OFF) {
            AudioTrack cloned = track.makeClone();
            cloned.setUserData(track.getUserData());
            queue.add(new QueuedTrack(cloned, extractRequestMetadata(track)), toEnt);
        }
    }

    /** Requester info of the track (also works for tracks replaced via yt-dlp; never null). */
    public static RequestMetadata extractRequestMetadata(AudioTrack track) {
        if (track == null) return RequestMetadata.EMPTY;
        Object ud = track.getUserData();
        if (ud instanceof RequestMetadata) return (RequestMetadata) ud;
        if (ud instanceof PlayerManager.TrackContext) {
            PlayerManager.TrackContext tc = (PlayerManager.TrackContext) ud;
            if (tc.userData instanceof RequestMetadata) {
                return (RequestMetadata) tc.userData;
            }
        }
        return RequestMetadata.EMPTY; // Empty (EMPTY) placeholder to avoid NPEs
    }

    /** Is there a previously played track? */
    public synchronized boolean hasPrevious() {
        return !history.isEmpty();
    }

    /**
     * Plays the previous track immediately; the current track is put back at the front of the queue.
     *
     * @return false if the history is empty
     */
    public synchronized boolean playPrevious() {
        AudioTrack prev = history.pollLast();
        if (prev == null) return false;
        AudioTrack cur = audioPlayer.getPlayingTrack();
        if (cur != null) {
            AudioTrack again = cur.makeClone();
            again.setUserData(cur.getUserData());
            queue.addAt(0, new QueuedTrack(again, extractRequestMetadata(cur)));
        }
        // The current track ends with REPLACED; onTrackEnd doesn't add it to the history
        audioPlayer.startTrack(prev, false);
        return true;
    }

    private synchronized void remember(AudioTrack track) {
        if (track == null) return;
        try {
            AudioTrack copy = track.makeClone();
            copy.setUserData(track.getUserData());
            history.addLast(copy);
            while (history.size() > HISTORY_LIMIT) history.pollFirst();
        } catch (Exception ignored) {
            // tracks that can't be cloned (some live streams) are not added to the history
        }
    }

    /** Track info to show (the original info for a track replaced via yt-dlp). */
    public AudioTrackInfo getDisplayInfo() {
        return displayInfo(audioPlayer.getPlayingTrack());
    }

    /** Info to show for any track (the original title/link if it was replaced via yt-dlp). */
    public static AudioTrackInfo displayInfo(AudioTrack track) {
        if (track == null) return null;
        AudioTrackInfo info = track.getInfo();
        Object ud = track.getUserData();
        if (ud instanceof PlayerManager.TrackContext) {
            PlayerManager.TrackContext tc = (PlayerManager.TrackContext) ud;
            if (tc.originalInfo != null) {
                return new AudioTrackInfo(
                        tc.originalInfo.title != null ? tc.originalInfo.title : info.title,
                        tc.originalInfo.author != null ? tc.originalInfo.author : info.author,
                        info.length, info.identifier, info.isStream,
                        tc.originalInfo.uri != null ? tc.originalInfo.uri : info.uri,
                        tc.originalInfo.artworkUrl != null ? tc.originalInfo.artworkUrl : info.artworkUrl,
                        info.isrc);
            }
        }
        return info;
    }

    public FairQueue<QueuedTrack> getQueue() {
        return queue;
    }

    /**
     * Playback session. Incremented on every "stop"; background jobs (adding the rest of a radio playlist,
     * downloading with yt-dlp) don't add their results to the queue if they belong to an old session.
     */
    private final java.util.concurrent.atomic.AtomicLong session = new java.util.concurrent.atomic.AtomicLong();
    /** Prevents an onTrackEnd that arrives during stopAndClear from moving to the next/default playlist. */
    private volatile boolean stopping = false;

    public long getGuildId() {
        return guildId;
    }

    public long getSession() {
        return session.get();
    }

    public void stopAndClear() {
        session.incrementAndGet();
        stopping = true;
        try {
            queue.clear();
            defaultQueue.clear();
            synchronized (this) {
                history.clear();
            }
            votes.clear();
            preparingTitle = null;
            audioPlayer.setPaused(false);
            audioPlayer.stopTrack();
        } finally {
            stopping = false;
        }

        Guild guild = guild(manager.getBot().getJDA());
        Bot.updatePlayStatus(guild, guild.getSelfMember(), PlayStatus.STOPPED);
    }

    public boolean isMusicPlaying(JDA jda) {
        return guild(jda).getSelfMember().getVoiceState().inAudioChannel() && audioPlayer.getPlayingTrack() != null;
    }

    public Set<String> getVotes() {
        return votes;
    }

    public AudioPlayer getPlayer() {
        return audioPlayer;
    }

    public RequestMetadata getRequestMetadata() {
        if (audioPlayer.getPlayingTrack() == null)
            return RequestMetadata.EMPTY;
        Object ud = audioPlayer.getPlayingTrack().getUserData();
        if (ud instanceof RequestMetadata) return (RequestMetadata) ud;
        if (ud instanceof PlayerManager.TrackContext) {
            PlayerManager.TrackContext tc = (PlayerManager.TrackContext) ud;
            if (tc.userData instanceof RequestMetadata) {
                return (RequestMetadata) tc.userData;
            }
        }
        return RequestMetadata.EMPTY;
    }

    public boolean playFromDefault() {
        if (!defaultQueue.isEmpty()) {
            audioPlayer.playTrack(defaultQueue.remove(0));
            return true;
        }
        Settings settings = manager.getBot().getSettingsManager().getSettings(guildId);
        if (settings == null || settings.getDefaultPlaylist() == null)
            return false;

        Playlist pl = manager.getBot().getPlaylistLoader().getPlaylist(stringGuildId, settings.getDefaultPlaylist());
        if (pl == null || pl.getItems().isEmpty())
            return false;
        pl.loadTracks(manager, (at) -> {
            if (audioPlayer.getPlayingTrack() == null)
                audioPlayer.playTrack(at);
            else
                defaultQueue.add(at);
        }, () -> {
            if (pl.getTracks().isEmpty() && !manager.getBot().getConfig().getStay())
                manager.getBot().closeAudioConnection(guildId);
        });
        return true;
    }

    // Audio Events
    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // ★ Skip the leave logic on a replacement (REPLACED) or temporary suppression
        if (stopping) {
            // "Stop" command: don't move on, and don't add to the history
            suppressAutoLeaveOnce.set(false);
            return;
        }
        if (endReason == AudioTrackEndReason.REPLACED || suppressAutoLeaveOnce.getAndSet(false)) {
            return;
        }

        RepeatMode repeatMode = manager.getBot().getSettingsManager().getSettings(guildId).getRepeatMode();

        // Add to the history for the "previous" button (finished or skipped)
        if (endReason == AudioTrackEndReason.FINISHED || endReason == AudioTrackEndReason.STOPPED) {
            remember(track);
        }

        // Repeat handling when a track finishes completely
        if (endReason == AudioTrackEndReason.FINISHED && repeatMode != RepeatMode.OFF) {
            // Copy the original track's userData to the clone as is
            AudioTrack cloned = track.makeClone();
            cloned.setUserData(track.getUserData());
            RequestMetadata rm = extractRequestMetadata(track);

            if (repeatMode == RepeatMode.ALL) {
                queue.add(new QueuedTrack(cloned, rm), true);
            } else if (repeatMode == RepeatMode.SINGLE) {
                queue.addAt(0, new QueuedTrack(cloned, rm));
            }
        }

        continueQueue(player);
    }

    /**
     * Called when the yt-dlp fallback also fails: moves to the next track
     * (otherwise the bot would silently get stuck).
     */
    /** Name of the track being prepared by the yt-dlp fallback (null if none). */
    private volatile String preparingTitle;

    /** Is the yt-dlp fallback preparing a track right now? */
    public boolean isPreparing() {
        return preparingTitle != null;
    }

    public void setPreparingTitle(String title) {
        this.preparingTitle = title;
    }

    public void advanceAfterFailure() {
        if (audioPlayer.getPlayingTrack() != null) return; // don't touch it if another track started in the meantime
        continueQueue(audioPlayer);
    }

    private void continueQueue(AudioPlayer player) {
        if (queue.isEmpty()) {
            if (!playFromDefault()) {
                manager.getBot().getNowplayingHandler().onTrackUpdate(guildId, null, this);
                if (!manager.getBot().getConfig().getStay()) manager.getBot().closeAudioConnection(guildId);

                player.setPaused(false);

                Guild guild = guild(manager.getBot().getJDA());
                Bot.updatePlayStatus(guild, guild.getSelfMember(), PlayStatus.STOPPED);
            }
        } else {
            QueuedTrack qt = queue.pull();
            player.playTrack(qt.getTrack());
        }
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        votes.clear();
        manager.getBot().getNowplayingHandler().onTrackUpdate(guildId, track, this);

        Guild guild = guild(manager.getBot().getJDA());
        Bot.updatePlayStatus(guild, guild.getSelfMember(), PlayStatus.PLAYING);
    }

    // Formatting
    public MessageCreateData getNowPlaying(JDA jda) throws Exception {
        if (!isMusicPlaying(jda)) return null;
        RepeatMode repeat = manager.getBot().getSettingsManager().getSettings(guildId).getRepeatMode();
        return dev.cosgy.jmusicbot.util.NowPlayingCard.build(this, guild(jda), repeat);
    }

    public MessageCreateData getNoMusicPlaying(JDA jda) {
        Guild guild = guild(jda);
        String preparing = preparingTitle;
        if (preparing != null) {
            return new MessageCreateBuilder()
                    .setEmbeds(new EmbedBuilder()
                            .setTitle(L.t("audio_handler.preparing_track"))
                            .setDescription("**" + FormatUtil.filter(preparing) + L.t("audio_handler.youtube_stream_could_not_opened")
                                    + (queue.isEmpty() ? "" : L.t("audio_handler.queue") + queue.size() + L.t("audio_handler.tracks_waiting")))
                            .setColor(new java.awt.Color(250, 166, 26))
                            .setFooter("JMusicBot-ML " + JMusicBot.BOT_VERSION)
                            .build())
                    .setComponents()
                    .build();
        }
        return new MessageCreateBuilder()
                .setContent(FormatUtil.filter(manager.getBot().getConfig().getSuccess() + L.t("audio_handler.nothing_playing_right_now")))
                .setEmbeds(new EmbedBuilder()
                        .setTitle(L.t("audio_handler.nothing_playing_right_now_2"))
                        .setDescription(JMusicBot.STOP_EMOJI + " " + FormatUtil.progressBar(-1) + " " + FormatUtil.volumeIcon(audioPlayer.getVolume()))
                        .setColor(guild.getSelfMember().getColor())
                        .setFooter("JMusicBot-ML " + JMusicBot.BOT_VERSION)
                        .build())
                .setComponents()
                .build();
    }

    public String getTopicFormat(JDA jda) {
        if (isMusicPlaying(jda)) {
            long userid = getRequestMetadata().getOwner();
            AudioTrack track = audioPlayer.getPlayingTrack();

            if (track.getInfo().uri.matches(".*stream.gensokyoradio.net/.*")) {
                return L.t("audio_handler.gensokyo_radio") + (userid == 0 ? L.t("now_playing_card.autoplay") : "<@" + userid + ">") + "]"
                        + "\n" + (audioPlayer.isPaused() ? JMusicBot.PAUSE_EMOJI : JMusicBot.PLAY_EMOJI) + " "
                        + "[CANLI] "
                        + FormatUtil.volumeIcon(audioPlayer.getVolume());
            }

            // Show the original title for a replaced track too
            AudioTrackInfo info = track.getInfo();
            String title = info.title;
            String uri   = info.uri;
            Object udAll = track.getUserData();
            if (udAll instanceof PlayerManager.TrackContext) {
                PlayerManager.TrackContext tc = (PlayerManager.TrackContext) udAll;
                if (tc.originalInfo != null) {
                    if (tc.originalInfo.title != null) title = tc.originalInfo.title;
                    if (tc.originalInfo.uri != null) uri = tc.originalInfo.uri;
                }
            }
            if (title == null || title.equals(L.t("now_playing_card.unknown_title")))
                title = uri;

            return "**" + title + "** [" + (userid == 0 ? L.t("now_playing_card.autoplay") : "<@" + userid + ">") + "]"
                    + "\n" + (audioPlayer.isPaused() ? JMusicBot.PAUSE_EMOJI : JMusicBot.PLAY_EMOJI) + " "
                    + "[" + FormatUtil.formatTime(track.getDuration()) + "] "
                    + FormatUtil.volumeIcon(audioPlayer.getVolume());
        } else return L.t("audio_handler.no_music_playing") + JMusicBot.STOP_EMOJI + " " + FormatUtil.volumeIcon(audioPlayer.getVolume());
    }

    // Audio Send Handler methods
    @Override
    public boolean canProvide() {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        return ByteBuffer.wrap(lastFrame.getData());
    }

    @Override
    public boolean isOpus() {
        return true;
    }

    // Private methods
    private Guild guild(JDA jda) {
        return jda.getGuildById(guildId);
    }

    // Extract the YouTube ID from the original URL (to show the replaced track)
    private static String extractYoutubeId(String url) {
        if (url == null) return null;
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
}