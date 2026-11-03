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

import com.jagrosh.jmusicbot.JMusicBot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.audio.RequestMetadata;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import dev.cosgy.jmusicbot.settings.RepeatMode;
import dev.cosgy.jmusicbot.spotify.SpotifyMirrorTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.Color;
import java.util.List;
import java.util.Locale;

/**
 * "Now playing" card: cover art, progress bar, info and control buttons.
 */
public final class NowPlayingCard {
    private NowPlayingCard() {
    }

    public static final String PREFIX = "np:";

    private static final Color SPOTIFY = new Color(30, 215, 96);
    private static final Color YOUTUBE = new Color(255, 0, 51);
    private static final Color SOUNDCLOUD = new Color(255, 85, 0);
    private static final Color DEFAULT = new Color(88, 101, 242);
    private static final Color PAUSED = new Color(128, 132, 142);

    public static MessageCreateData build(AudioHandler handler, Guild guild, RepeatMode repeat) {
        AudioTrack track = handler.getPlayer().getPlayingTrack();
        boolean paused = handler.getPlayer().isPaused();
        RequestMetadata rm = handler.getRequestMetadata();

        // Show the original info for a replaced track (yt-dlp fallback)
        AudioTrackInfo info = track.getInfo();
        AudioTrackInfo shown = handler.getDisplayInfo();
        String title = shown.title;
        String uri = shown.uri;
        String author = shown.author;
        boolean gensokyo = uri != null && uri.contains("gensokyoradio.net");
        if (gensokyo) title = L.t("queue_paginator_manager.gensokyo_radio");
        if (title == null || title.isBlank() || title.equals("Unknown title")) title = L.t("now_playing_card.unknown_title");

        String source = sourceName(track, uri);
        Color color = paused ? PAUSED : colorFor(source);

        AudioChannel vc = guild.getSelfMember().getVoiceState() == null ? null : guild.getSelfMember().getVoiceState().getChannel();
        EmbedBuilder eb = new EmbedBuilder().setColor(color);
        eb.setAuthor((paused ? L.t("now_playing_card.paused") : L.t("now_playing_card.now_playing")) + (vc == null ? "" : " · " + vc.getName()),
                null, guild.getSelfMember().getEffectiveAvatarUrl());
        try {
            eb.setTitle(limit(title, 250), uri != null && uri.startsWith("http") ? uri : null);
        } catch (Exception e) {
            eb.setTitle(limit(title, 250));
        }

        // Progress bar
        long pos = track.getPosition();
        long dur = track.getDuration();
        StringBuilder desc = new StringBuilder();
        if (author != null && !author.isBlank() && !gensokyo && !author.equalsIgnoreCase("Unknown artist")) desc.append("**").append(FormatUtil.filter(limit(author, 200))).append("**\n\n");
        if (info.isStream || dur == Long.MAX_VALUE || gensokyo) {
            desc.append(L.t("now_playing_card.live"));
        } else {
            desc.append("`").append(FormatUtil.formatTime(pos)).append("` ")
                    .append(bar(dur <= 0 ? 0 : (double) pos / dur, 16))
                    .append(" `").append(FormatUtil.formatTime(dur)).append("`");
        }
        eb.setDescription(desc.toString());

        // Info fields
        eb.addField(L.t("now_playing_card.requested_by"), rm.getOwner() == 0L ? L.t("now_playing_card.autoplay") : "<@" + rm.getOwner() + ">", true);
        int vol = handler.getPlayer().getVolume();
        eb.addField(FormatUtil.volumeIcon(vol) + L.t("now_playing_card.volume"), "%" + vol, true);
        eb.addField(L.t("now_playing_card.repeat"), repeat == RepeatMode.ALL ? L.t("now_playing_card.whole_queue") : repeat == RepeatMode.SINGLE ? L.t("now_playing_card.single_track") : L.t("now_playing_card.off"), true);

        List<QueuedTrack> queue = handler.getQueue().getList();
        if (!queue.isEmpty()) {
            long total = 0;
            for (QueuedTrack qt : queue) {
                long d = qt.getTrack().getDuration();
                if (d > 0 && d != Long.MAX_VALUE) total += d;
            }
            AudioTrackInfo next = queue.get(0).getTrack().getInfo();
            String nextTitle = next.uri != null && next.uri.contains("gensokyoradio.net") ? L.t("queue_paginator_manager.gensokyo_radio") : next.title;
            eb.addField(L.t("now_playing_card.up_next"), FormatUtil.filter(limit(nextTitle, 90)) + (next.author == null || next.author.isBlank() ? "" : " — " + FormatUtil.filter(limit(next.author, 40))), false);
            eb.addField(L.t("now_playing_card.queue"), queue.size() + L.t("now_playing_card.tracks") + FormatUtil.formatTime(total), true);
        } else {
            eb.addField(L.t("now_playing_card.up_next"), L.t("now_playing_card.queue_empty"), false);
        }

        // Kapak resmi
        String art = artwork(track, info, uri);
        if (art != null) eb.setThumbnail(art);

        eb.setFooter(source + " · JMusicBot-ML " + JMusicBot.BOT_VERSION);

        return new MessageCreateBuilder()
                .setEmbeds(eb.build())
                .setComponents(buttons(paused, repeat, handler.hasPrevious()))
                .build();
    }

    /** Control buttons (two rows). */
    public static List<ActionRow> buttons(boolean paused, RepeatMode repeat, boolean hasPrevious) {
        Button prev = Button.secondary(PREFIX + "prev", Emoji.fromUnicode("⏮")).withDisabled(!hasPrevious);
        Button pause = paused
                ? Button.success(PREFIX + "pause", L.t("now_playing_card.resume")).withEmoji(Emoji.fromUnicode("▶"))
                : Button.primary(PREFIX + "pause", L.t("now_playing_card.pause")).withEmoji(Emoji.fromUnicode("⏸"));
        Button skip = Button.secondary(PREFIX + "skip", Emoji.fromUnicode("⏭"));
        Button stop = Button.danger(PREFIX + "stop", Emoji.fromUnicode("⏹"));
        Button rep = (repeat == RepeatMode.OFF ? Button.secondary(PREFIX + "repeat", Emoji.fromUnicode("🔁"))
                : Button.success(PREFIX + "repeat", Emoji.fromUnicode(repeat == RepeatMode.SINGLE ? "🔂" : "🔁")));

        Button volDown = Button.secondary(PREFIX + "voldown", Emoji.fromUnicode("🔉"));
        Button volUp = Button.secondary(PREFIX + "volup", Emoji.fromUnicode("🔊"));
        Button shuffle = Button.secondary(PREFIX + "shuffle", Emoji.fromUnicode("🔀"));
        Button queue = Button.secondary(PREFIX + "queue", L.t("now_playing_card.queue_2")).withEmoji(Emoji.fromUnicode("📜"));
        Button lyrics = Button.secondary(PREFIX + "lyrics", L.t("now_playing_card.lyrics")).withEmoji(Emoji.fromUnicode("🎤"));

        return List.of(ActionRow.of(prev, pause, skip, stop, rep), ActionRow.of(volDown, volUp, shuffle, queue, lyrics));
    }

    private static String bar(double percent, int length) {
        if (percent < 0) percent = 0;
        if (percent > 1) percent = 1;
        int knob = (int) Math.round(percent * (length - 1));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i < knob) sb.append('━');
            else if (i == knob) sb.append("🔘"); // 🔘
            else sb.append('─');
        }
        return sb.toString();
    }

    private static String sourceName(AudioTrack track, String uri) {
        if (track instanceof SpotifyMirrorTrack) return "Spotify → YouTube";
        String src = track.getSourceManager() == null ? "" : track.getSourceManager().getSourceName().toLowerCase(Locale.ROOT);
        String u = uri == null ? "" : uri.toLowerCase(Locale.ROOT);
        if (src.contains("youtube") || u.contains("youtube.com") || u.contains("youtu.be")) return "YouTube";
        if (src.contains("soundcloud")) return "SoundCloud";
        if (src.contains("bandcamp")) return "Bandcamp";
        if (src.contains("twitch")) return "Twitch";
        if (src.contains("vimeo")) return "Vimeo";
        if (src.contains("nico")) return "Niconico";
        if (src.contains("local")) return L.t("now_playing_card.file");
        if (src.contains("http")) return L.t("now_playing_card.internet");
        return src.isEmpty() ? L.t("now_playing_card.music") : Character.toUpperCase(src.charAt(0)) + src.substring(1);
    }

    private static Color colorFor(String source) {
        if (source.startsWith("Spotify")) return SPOTIFY;
        if (source.equals("YouTube")) return YOUTUBE;
        if (source.equals("SoundCloud")) return SOUNDCLOUD;
        return DEFAULT;
    }

    private static String artwork(AudioTrack track, AudioTrackInfo info, String uri) {
        if (info.artworkUrl != null && !info.artworkUrl.isBlank()) return info.artworkUrl;
        String id = youtubeId(uri);
        if (id == null && "YouTube".equals(sourceName(track, uri))) id = track.getIdentifier();
        return id == null || id.length() != 11 ? null : "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg";
    }

    private static String youtubeId(String url) {
        if (url == null) return null;
        int v = url.indexOf("v=");
        if (v >= 0) {
            String s = url.substring(v + 2);
            int amp = s.indexOf('&');
            return amp > 0 ? s.substring(0, amp) : s;
        }
        int i = url.indexOf("youtu.be/");
        if (i >= 0) {
            String s = url.substring(i + 9);
            int q = s.indexOf('?');
            return q > 0 ? s.substring(0, q) : s;
        }
        return null;
    }

    private static String limit(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
