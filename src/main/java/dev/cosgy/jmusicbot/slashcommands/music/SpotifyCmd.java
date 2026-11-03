/*
 *  Copyright 2023 Cosgy Dev (info@cosgy.dev).
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

package dev.cosgy.jmusicbot.slashcommands.music;

import dev.cosgy.jmusicbot.i18n.L;

import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import dev.cosgy.jmusicbot.slashcommands.MusicCommand;
import dev.cosgy.jmusicbot.spotify.SpotifyAudioPlaylist;
import dev.cosgy.jmusicbot.spotify.SpotifyCollection;
import dev.cosgy.jmusicbot.spotify.SpotifyLink;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Plays Spotify links: track, album, playlist and artist (popular tracks).
 * Note: Spotify links also work directly with /play and !play; this command additionally
 * shows an info card with the cover art.
 */
public class SpotifyCmd extends MusicCommand {
    private static final Color SPOTIFY_GREEN = new Color(30, 215, 96);
    private static final String SPOTIFY_ICON = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/19/Spotify_logo_without_text.svg/240px-Spotify_logo_without_text.svg.png";

    public SpotifyCmd(Bot bot) {
        super(bot);
        this.name = "spotify";
        this.arguments = L.t("spotify.spotify_link");
        this.help = L.t("spotify.plays_spotify_track_album_playlist");
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = true;
        this.bePlaying = false;

        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.STRING, "link", L.t("spotify.spotify_link_track_album_playlist"), true));
        this.options = options;
    }

    @Override
    public void doCommand(SlashCommandEvent event) {
        String url = SpotifyLink.clean(event.getOption("link").getAsString());
        if (!SpotifyLink.isSpotify(url)) {
            event.reply(event.getClient().getError() + " " + invalidLinkMessage()).setEphemeral(true).queue();
            return;
        }
        // Defer first so Discord doesn't show "the application did not respond" while Spotify is queried
        event.deferReply().queue(hook -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), url,
                new Handler(event.getGuild(), event.getUser(), event.getClient().getSuccess(), event.getClient().getWarning(),
                        event.getClient().getError(),
                        (text, embed) -> {
                            if (embed == null) hook.editOriginal(text).queue();
                            else hook.editOriginal(text).setEmbeds(embed).queue();
                        })));
    }

    @Override
    public void doCommand(CommandEvent event) {
        String url = SpotifyLink.clean(event.getArgs());
        if (url.isEmpty()) {
            event.replyError(L.t("spotify.please_enter_spotify_link_example") + event.getClient().getPrefix() + name
                    + " https://open.spotify.com/intl-tr/track/...`");
            return;
        }
        if (!SpotifyLink.isSpotify(url)) {
            event.replyError(invalidLinkMessage());
            return;
        }
        event.reply(bot.getConfig().getLoading() + L.t("spotify.loading_from_spotify"), m ->
                bot.getPlayerManager().loadItemOrdered(event.getGuild(), url,
                        new Handler(event.getGuild(), event.getAuthor(), event.getClient().getSuccess(), event.getClient().getWarning(),
                                event.getClient().getError(),
                                (text, embed) -> {
                                    if (embed == null) m.editMessage(text).queue();
                                    else m.editMessage(text).setEmbeds(embed).queue();
                                })));
    }

    private static String invalidLinkMessage() {
        return L.t("spotify.not_valid_spotify_link_enter")
                + L.t("spotify.example_https_open_spotify_com");
    }

    /** Adds the load result to the queue and shows the user an info card. */
    private class Handler implements AudioLoadResultHandler {
        private final Guild guild;
        private final User user;
        private final String success, warning, error;
        private final BiConsumer<String, MessageEmbed> out;

        Handler(Guild guild, User user, String success, String warning, String error, BiConsumer<String, MessageEmbed> out) {
            this.guild = guild;
            this.user = user;
            this.success = success;
            this.warning = warning;
            this.error = error;
            this.out = out;
        }

        @Override
        public void trackLoaded(AudioTrack track) {
            if (bot.getConfig().isTooLong(track)) {
                out.accept(FormatUtil.filter(warning + " **" + track.getInfo().title + "** (`" + FormatUtil.formatTime(track.getDuration())
                        + L.t("spotify.exceeds_maximum_allowed_length") + bot.getConfig().getMaxTime() + L.t("spotify.text")), null);
                return;
            }
            AudioHandler handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
            int pos = handler.addTrack(new QueuedTrack(track, user)) + 1;

            EmbedBuilder eb = new EmbedBuilder()
                    .setColor(SPOTIFY_GREEN)
                    .setAuthor(pos == 0 ? L.t("spotify.now_playing") : L.t("spotify.added_queue"), null, SPOTIFY_ICON)
                    .setTitle(limit(track.getInfo().title, 250), track.getInfo().uri.startsWith("http") ? track.getInfo().uri : null)
                    .addField(L.t("spotify_link.artist"), limit(empty(track.getInfo().author), 1000), true)
                    .addField(L.t("spotify.duration"), track.getDuration() > 0 ? FormatUtil.formatTime(track.getDuration()) : "?", true);
            if (pos > 0) eb.addField(L.t("spotify.queue_position"), "#" + pos, true);
            if (track.getInfo().artworkUrl != null) eb.setThumbnail(track.getInfo().artworkUrl);
            eb.setFooter(L.t("spotify.requested_by") + user.getName(), user.getEffectiveAvatarUrl());

            out.accept(success + (pos == 0 ? L.t("spotify.now_playing_2") : L.t("spotify.added_queue_2")), eb.build());
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            if (playlist.isSearchResult() || playlist.getTracks().size() == 1) {
                if (playlist.getTracks().isEmpty()) {
                    noMatches();
                } else {
                    trackLoaded(playlist.getSelectedTrack() != null ? playlist.getSelectedTrack() : playlist.getTracks().get(0));
                }
                return;
            }

            AudioHandler handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
            int added = 0, skipped = 0;
            for (AudioTrack track : playlist.getTracks()) {
                if (bot.getConfig().isTooLong(track)) {
                    skipped++;
                    continue;
                }
                handler.addTrack(new QueuedTrack(track, user));
                added++;
            }
            if (added == 0) {
                out.accept(FormatUtil.filter(warning + L.t("spotify.all_tracks_list_exceed_maximum")
                        + bot.getConfig().getMaxTime() + L.t("spotify.text")), null);
                return;
            }

            SpotifyCollection c = playlist instanceof SpotifyAudioPlaylist ? ((SpotifyAudioPlaylist) playlist).getCollection() : null;
            String type = c != null && c.link != null ? c.link.type.turkish() : L.t("spotify.list");

            EmbedBuilder eb = new EmbedBuilder()
                    .setColor(SPOTIFY_GREEN)
                    .setAuthor("Spotify " + ("de".equals(L.lang()) ? type : type.toLowerCase(L.locale())) + L.t("spotify.added_queue_3"), null, SPOTIFY_ICON)
                    .setTitle(limit(empty(playlist.getName()), 250), c != null && c.link != null ? c.link.openUrl() : null);
            if (c != null && !c.owner.isEmpty()) {
                eb.addField(c.link != null && c.link.type == SpotifyLink.Type.PLAYLIST ? L.t("spotify.created_by") : L.t("spotify_link.artist"), limit(c.owner, 1000), true);
            }
            eb.addField(L.t("spotify.tracks_added"), String.valueOf(added), true);
            if (c != null && c.imageUrl != null) eb.setThumbnail(c.imageUrl);

            StringBuilder desc = new StringBuilder();
            if (skipped > 0) {
                desc.append(warning).append(" ").append(skipped).append(L.t("spotify.tracks_were_skipped_because_they"))
                        .append(bot.getConfig().getMaxTime()).append(L.t("spotify.text_2"));
            }
            if (c != null && c.isTruncated()) {
                desc.append(warning).append(L.t("spotify.spotify_shows")).append(c.totalOnSpotify)
                        .append(L.t("spotify.tracks_list_but_only_first")).append(c.tracks.size()).append(L.t("spotify.could_retrieved"));
            }
            desc.append(L.t("spotify.tracks_are_looked_up_youtube"));
            eb.setDescription(desc.toString());
            eb.setFooter(L.t("spotify.requested_by") + user.getName(), user.getEffectiveAvatarUrl());

            out.accept(success + " **" + added + L.t("spotify.tracks_added_queue"), eb.build());
        }

        @Override
        public void noMatches() {
            out.accept(warning + L.t("spotify.no_playable_tracks_could_retrieved"), null);
        }

        @Override
        public void loadFailed(FriendlyException throwable) {
            if (throwable.severity == FriendlyException.Severity.COMMON) {
                out.accept(error + L.t("spotify.could_not_load") + dev.cosgy.jmusicbot.util.ErrorTranslator.tr(throwable.getMessage()), null);
            } else {
                out.accept(error + L.t("spotify.unexpected_error_occurred_while_loading"), null);
            }
        }
    }

    private static String empty(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }

    private static String limit(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
