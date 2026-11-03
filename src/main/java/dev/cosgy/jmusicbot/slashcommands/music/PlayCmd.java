/*
 * Copyright 2018 John Grosh (jagrosh).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.cosgy.jmusicbot.slashcommands.music;

import dev.cosgy.jmusicbot.i18n.L;

import dev.cosgy.jmusicbot.framework.jdautilities.command.Command;
import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommand;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.menu.ButtonMenu;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.PlayStatus;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.playlist.PlaylistLoader.Playlist;
import com.jagrosh.jmusicbot.settings.Settings;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.cosgy.jmusicbot.playlist.CacheLoader;
import dev.cosgy.jmusicbot.playlist.MylistLoader;
import dev.cosgy.jmusicbot.playlist.PubliclistLoader;
import dev.cosgy.jmusicbot.slashcommands.DJCommand;
import dev.cosgy.jmusicbot.slashcommands.MusicCommand;
import dev.cosgy.jmusicbot.util.Cache;
import dev.cosgy.jmusicbot.util.StackTraceUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class PlayCmd extends MusicCommand {
    private final static String LOAD = L.t("play.add");    // 📥
    private final static String CANCEL = L.t("play.cancel");  // 🚫

    private final String loadingEmoji;

    public PlayCmd(Bot bot) {
        super(bot);
        this.loadingEmoji = bot.getConfig().getLoading();
        this.name = "play";
        this.arguments = L.t("play.track_name_url_spotify_link");
        this.help = L.t("play.plays_specified_track_youtube_spotify");
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = true;
        this.bePlaying = false;

        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.STRING, "input", L.t("play.url_track_name_spotify_links"), false));
        this.options = options;

        //this.children = new SlashCommand[]{new PlaylistCmd(bot), new MylistCmd(bot), new PublistCmd(bot), new RequestCmd(bot)};
    }

    @Override
    public void doCommand(CommandEvent event) {

        if (event.getArgs().isEmpty() && event.getMessage().getAttachments().isEmpty()) {
            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            if (handler.getPlayer().getPlayingTrack() != null && handler.getPlayer().isPaused()) {
                if (DJCommand.checkDJPermission(event)) {
                    handler.getPlayer().setPaused(false);
                    event.replySuccess("**" + handler.getPlayer().getPlayingTrack().getInfo().title + L.t("play.resumed_playing"));
                    bot.getNowplayingHandler().onTrackUpdate(event.getGuild().getIdLong(), handler.getPlayer().getPlayingTrack(), handler);

                    Bot.updatePlayStatus(event.getGuild(), event.getGuild().getSelfMember(), PlayStatus.PLAYING);
                } else
                    event.replyError(L.t("play.only_dj_can_resume_playback"));
                return;
            }

            // Loading mechanism from the cache
            if (bot.getCacheLoader().cacheExists(event.getGuild().getId())) {
                List<Cache> data = bot.getCacheLoader().GetCache(event.getGuild().getId());

                AtomicInteger count = new AtomicInteger();
                CacheLoader.CacheResult cache = bot.getCacheLoader().ConvertCache(data);
                event.getChannel().sendMessage(L.t("play.calling_loading_cache_file") + cache.getItems().size() + L.t("play.tracks")).queue(m -> {
                    cache.loadTracks(bot.getPlayerManager(), (at) -> {
                        handler.addTrack(new QueuedTrack(at, (User) User.fromId(data.get(count.get()).getUserId())));
                        count.getAndIncrement();
                    }, () -> {
                        StringBuilder builder = new StringBuilder(cache.getTracks().isEmpty()
                                ? event.getClient().getWarning() + L.t("play.no_tracks_could_loaded")
                                : event.getClient().getSuccess() + L.t("play.cache_file") + "**" + cache.getTracks().size() + L.t("play.tracks_loaded"));
                        if (!cache.getErrors().isEmpty())
                            builder.append(L.t("play.these_tracks_could_not_loaded"));
                        cache.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex() + 1).append("]` **").append(err.getItem()).append("**: ").append(err.getReason()));
                        String str = builder.toString();
                        if (str.length() > 2000)
                            str = str.substring(0, 1994) + L.t("play.truncated");
                        m.editMessage(FormatUtil.filter(str)).queue();
                    });
                });
                try {
                    bot.getCacheLoader().deleteCache(event.getGuild().getId());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }

            if (handler.playFromDefault()) {
                Settings settings = event.getClient().getSettingsFor(event.getGuild());
                handler.stopAndClear();
                Playlist playlist = bot.getPlaylistLoader().getPlaylist(event.getGuild().getId(), settings.getDefaultPlaylist());
                if (playlist == null) {
                    event.replyError(L.t("play.text") + settings.getDefaultPlaylist() + L.t("play.txt_was_not_found_playlist"));
                    return;
                }
                event.getChannel().sendMessage(loadingEmoji + L.t("play.loading_playlist") + settings.getDefaultPlaylist() + "** ... (" + playlist.getItems().size() + L.t("play.tracks")).queue(m ->
                {

                    playlist.loadTracks(bot.getPlayerManager(), (at) -> handler.addTrack(new QueuedTrack(at, event.getAuthor())), () -> {
                        StringBuilder builder = new StringBuilder(playlist.getTracks().isEmpty()
                                ? event.getClient().getWarning() + L.t("play.no_tracks_could_loaded_2")
                                : event.getClient().getSuccess() + " ** " + playlist.getTracks().size() + L.t("play.tracks_loaded_2"));
                        if (!playlist.getErrors().isEmpty())
                            builder.append(L.t("play.these_tracks_could_not_loaded"));
                        playlist.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex() + 1).append("]` **").append(err.getItem()).append("**: ").append(err.getReason()));
                        String str = builder.toString();
                        if (str.length() > 2000)
                            str = str.substring(0, 1994) + " (...)";
                        m.editMessage(FormatUtil.filter(str)).queue();
                    });
                });
                return;

            }

            StringBuilder builder = new StringBuilder(event.getClient().getWarning() + L.t("play.play_command"));
            builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(L.t("play.track_name_plays_first_result"));
            builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(L.t("play.url_plays_specified_track_playlist"));
            for (Command cmd : children)
                builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" ").append(cmd.getName()).append(" ").append(cmd.getArguments()).append("` - ").append(cmd.getHelp());
            event.reply(builder.toString());
            return;
        }
        String args = event.getArgs().startsWith("<") && event.getArgs().endsWith(">")
                ? event.getArgs().substring(1, event.getArgs().length() - 1)
                : event.getArgs().isEmpty() ? event.getMessage().getAttachments().get(0).getUrl() : event.getArgs();
        event.reply(loadingEmoji + "`[" + args + L.t("play.loading"), m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), args, new ResultHandler(m, event, false)));
    }

    @Override
    public void doCommand(SlashCommandEvent event) {

        if (event.getOption("input") == null) {
            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            if (handler.getPlayer().getPlayingTrack() != null && handler.getPlayer().isPaused()) {
                if (DJCommand.checkDJPermission(event.getClient(), event)) {

                    handler.getPlayer().setPaused(false);
                    event.reply(event.getClient().getSuccess() + "**" + handler.getPlayer().getPlayingTrack().getInfo().title + L.t("play.resumed_playing")).queue();
                    bot.getNowplayingHandler().onTrackUpdate(event.getGuild().getIdLong(), handler.getPlayer().getPlayingTrack(), handler);

                    Bot.updatePlayStatus(event.getGuild(), event.getGuild().getSelfMember(), PlayStatus.PLAYING);
                } else
                    event.reply(event.getClient().getError() + L.t("play.only_dj_can_resume_playback")).queue();
                return;
            }

            // Loading mechanism from the cache
            if (bot.getCacheLoader().cacheExists(event.getGuild().getId())) {
                List<Cache> data = bot.getCacheLoader().GetCache(event.getGuild().getId());

                AtomicInteger count = new AtomicInteger();
                CacheLoader.CacheResult cache = bot.getCacheLoader().ConvertCache(data);
                event.reply(L.t("play.calling_loading_cache_file") + cache.getItems().size() + L.t("play.tracks")).queue(m -> {
                    cache.loadTracks(bot.getPlayerManager(), (at) -> {
                        // TODO: use the user ID saved in the cache.
                        handler.addTrack(new QueuedTrack(at, event.getUser()));
                        count.getAndIncrement();
                    }, () -> {
                        StringBuilder builder = new StringBuilder(cache.getTracks().isEmpty()
                                ? event.getClient().getWarning() + L.t("play.no_tracks_could_loaded")
                                : event.getClient().getSuccess() + L.t("play.cache_file") + "**" + cache.getTracks().size() + L.t("play.tracks_loaded"));
                        if (!cache.getErrors().isEmpty())
                            builder.append(L.t("play.these_tracks_could_not_loaded"));
                        cache.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex() + 1).append("]` **").append(err.getItem()).append("**: ").append(err.getReason()));
                        String str = builder.toString();
                        if (str.length() > 2000)
                            str = str.substring(0, 1994) + L.t("play.truncated");
                        m.editOriginal(FormatUtil.filter(str)).queue();
                    });
                });
                try {
                    bot.getCacheLoader().deleteCache(event.getGuild().getId());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }

            if (handler.playFromDefault()) {
                Settings settings = event.getClient().getSettingsFor(event.getGuild());
                handler.stopAndClear();
                Playlist playlist = bot.getPlaylistLoader().getPlaylist(event.getGuild().getId(), settings.getDefaultPlaylist());
                if (playlist == null) {
                    event.reply(L.t("play.text") + settings.getDefaultPlaylist() + L.t("play.txt_was_not_found_playlist")).queue();
                    return;
                }
                event.reply(loadingEmoji + L.t("play.loading_playlist") + settings.getDefaultPlaylist() + "** ... (" + playlist.getItems().size() + L.t("play.tracks")).queue(m ->
                {

                    playlist.loadTracks(bot.getPlayerManager(), (at) -> handler.addTrack(new QueuedTrack(at, event.getUser())), () -> {
                        StringBuilder builder = new StringBuilder(playlist.getTracks().isEmpty()
                                ? event.getClient().getWarning() + L.t("play.no_tracks_could_loaded_2")
                                : event.getClient().getSuccess() + " ** " + playlist.getTracks().size() + L.t("play.tracks_loaded_2"));
                        if (!playlist.getErrors().isEmpty())
                            builder.append(L.t("play.these_tracks_could_not_loaded"));
                        playlist.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex() + 1).append("]` **").append(err.getItem()).append("**: ").append(err.getReason()));
                        String str = builder.toString();
                        if (str.length() > 2000)
                            str = str.substring(0, 1994) + " (...)";
                        m.editOriginal(FormatUtil.filter(str)).queue();
                    });
                });
                return;

            }

            StringBuilder builder = new StringBuilder(event.getClient().getWarning() + L.t("play.play_command"));
            builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(L.t("play.track_name_plays_first_result"));
            builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(L.t("play.url_plays_specified_track_playlist"));
            for (Command cmd : children)
                builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" ").append(cmd.getName()).append(" ").append(cmd.getArguments()).append("` - ").append(cmd.getHelp());
            event.reply(builder.toString()).queue();
            return;
        }
        event.reply(loadingEmoji + "`[" + event.getOption("input").getAsString() + L.t("play.loading")).queue(m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), event.getOption("input").getAsString(), new SlashResultHandler(m, event, false)));

    }

    public class SlashResultHandler implements AudioLoadResultHandler {
        private final InteractionHook m;
        private final SlashCommandEvent event;
        private final boolean ytsearch;

        SlashResultHandler(InteractionHook m, SlashCommandEvent event, boolean ytsearch) {
            this.m = m;
            this.event = event;
            this.ytsearch = ytsearch;
        }

        private void loadSingle(AudioTrack track, AudioPlaylist playlist) {
            // too-long checks remain the same
            if (bot.getConfig().isTooLong(track)) {
                m.editOriginal(FormatUtil.filter(event.getClient().getWarning() +
                                " **" + (track.getInfo().uri.matches(".*stream.gensokyoradio.net/.*") ? L.t("queue_paginator_manager.gensokyo_radio") : track.getInfo().title)
                                + "**`(" + FormatUtil.formatTime(track.getDuration()) + L.t("play.exceeds_configured_maximum_length") + FormatUtil.formatTime(bot.getConfig().getMaxSeconds() * 1000) + L.t("play.text_2")))
                        .queue();
                return;
            }
            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            int pos = handler.addTrack(new QueuedTrack(track, event.getUser())) + 1;

            String addMsg = FormatUtil.filter(event.getClient().getSuccess() + " **" + (track.getInfo().uri.matches(".*stream.gensokyoradio.net/.*") ? L.t("queue_paginator_manager.gensokyo_radio") : track.getInfo().title)
                    + "** (`" + FormatUtil.formatTime(track.getDuration()) + "`) " + (pos == 0 ? L.t("play.started_playing") : L.t("play.was_added_queue_at_position") + pos + L.t("play.text_3")));

            // If there is no playlist or we cannot send buttons, simply edit the message
            if (playlist == null || !event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_SEND)) {
                m.editOriginal(addMsg).queue();
            } else {
                // build buttons with unique IDs so multiple interactions do not conflict
                String unique = Long.toHexString(System.currentTimeMillis());
                String loadId = "play:load:" + unique;
                String cancelId = "play:cancel:" + unique;

                Button loadButton = Button.primary(loadId, LOAD);
                Button cancelButton = Button.danger(cancelId, CANCEL);

                m.editOriginal(addMsg + "\n" + event.getClient().getWarning()
                                + L.t("play.playlist_containing_track_has") + playlist.getTracks().size() + L.t("play.more_tracks_load_them_all") + LOAD + L.t("play.text_4"))
                        .setComponents(ActionRow.of(loadButton, cancelButton))
                        .queue();

                // wait for button click or timeout
                bot.getWaiter().waitForEvent(ButtonInteractionEvent.class,
                        (e) -> e.getComponentId().equals(loadId) || e.getComponentId().equals(cancelId),
                        (e) -> {
                            // always defer the edit so the user doesn't see "interaction failed"
                            e.deferEdit().queue();
                            if (e.getComponentId().equals(loadId)) {
                                int count = loadPlaylist(playlist, track);
                                m.editOriginal(addMsg + "\n" + event.getClient().getSuccess()
                                                + "**" + count + L.t("play.tracks_added_queue"))
                                        .setComponents()
                                        .queue();
                            } else {
                                m.editOriginal(addMsg)
                                        .setComponents()
                                        .queue();
                            }
                        },
                        30, TimeUnit.SECONDS,
                        () -> m.editOriginal(addMsg).setComponents().queue());
            }
        }

        /**
         * When a track comes with a list containing it (e.g. a YouTube Mix/Radio or a link with list=),
         * adds the track and all tracks in the list to the queue without asking.
         */
        private void loadAll(AudioTrack selected, AudioPlaylist playlist) {
            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            boolean selectedTooLong = bot.getConfig().isTooLong(selected);
            int pos = selectedTooLong ? -2 : handler.addTrack(new QueuedTrack(selected, event.getUser())) + 1;

            // Keep the YouTube order: tracks after the selected one first, the earlier ones at the end
            List<AudioTrack> all = playlist.getTracks();
            int idx = -1;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i) == selected || all.get(i).getIdentifier().equals(selected.getIdentifier())) { idx = i; break; }
            }
            List<AudioTrack> ordered = new ArrayList<>();
            if (idx >= 0) {
                ordered.addAll(all.subList(idx + 1, all.size()));
                ordered.addAll(all.subList(0, idx));
            } else {
                ordered.addAll(all);
            }
            java.util.Set<String> ids = new java.util.HashSet<>();
            String selId = dev.cosgy.jmusicbot.util.YoutubeMixExpander.videoId(selected);
            if (selId != null) ids.add(selId);
            int count = 0, skipped = 0;
            boolean mix = dev.cosgy.jmusicbot.util.YoutubeMixExpander.isMix(event.getOption("input") == null ? "" : event.getOption("input").getAsString());
            for (AudioTrack t : ordered) {
                // Total limit for radio playlists (YoutubeMixExpander.MAX_TRACKS)
                if (mix && ids.size() >= dev.cosgy.jmusicbot.util.YoutubeMixExpander.MAX_TRACKS) break;
                if (bot.getConfig().isTooLong(t)) { skipped++; continue; }
                String vid = dev.cosgy.jmusicbot.util.YoutubeMixExpander.videoId(t);
                if (vid != null && !ids.add(vid)) continue; // don't add the same track twice
                handler.addTrack(new QueuedTrack(t, event.getUser()));
                count++;
            }

            String title = track_title(selected);
            StringBuilder msg = new StringBuilder();
            if (selectedTooLong) {
                msg.append(event.getClient().getWarning()).append(" **").append(title).append(L.t("play.was_not_added_because_exceeds"));
            } else {
                msg.append(event.getClient().getSuccess()).append(" **").append(title).append("** (`").append(FormatUtil.formatTime(selected.getDuration())).append("`) ")
                        .append(pos == 0 ? L.t("play.started_playing_2") : L.t("play.was_added_queue_at_position_2") + pos + L.t("play.text_3"));
            }
            if (count > 0) msg.append("\n").append(event.getClient().getSuccess()).append(L.t("play.from_playlist")).append(count).append(L.t("youtube_mix_expander.more_tracks_added_queue"));
            if (skipped > 0) msg.append("\n").append(event.getClient().getWarning()).append(" ").append(skipped).append(L.t("play.tracks_were_skipped_because_they"));

            String url = event.getOption("input") == null ? "" : event.getOption("input").getAsString();
            if (dev.cosgy.jmusicbot.util.YoutubeMixExpander.isMix(url) && bot.getPlayerManager().getYtDlpPath() != null
                    && ids.size() < dev.cosgy.jmusicbot.util.YoutubeMixExpander.MAX_TRACKS) {
                msg.append(L.t("play.youtube_radio_playlist_rest_being") + dev.cosgy.jmusicbot.util.YoutubeMixExpander.MAX_TRACKS + L.t("play.tracks_2"));
                dev.cosgy.jmusicbot.util.YoutubeMixExpander.expand(bot, event.getGuild(), event.getUser(), url, ids, event.getChannel());
            }
            m.editOriginal(FormatUtil.filter(msg.toString())).setComponents().queue();
        }

        private String track_title(AudioTrack t) {
            return t.getInfo().uri.contains("https://stream.gensokyoradio.net/") ? L.t("queue_paginator_manager.gensokyo_radio") : t.getInfo().title;
        }

        private int loadPlaylist(AudioPlaylist playlist, AudioTrack exclude) {
            int[] count = {0};
            playlist.getTracks().forEach((track) -> {
                if (!bot.getConfig().isTooLong(track) && !track.equals(exclude)) {
                    AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                    handler.addTrack(new QueuedTrack(track, event.getUser()));
                    count[0]++;
                }
            });
            return count[0];
        }

        @Override
        public void trackLoaded(AudioTrack track) {
            loadSingle(track, null);
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            if (playlist.getTracks().size() == 1 || playlist.isSearchResult()) {
                AudioTrack single = playlist.getSelectedTrack() == null ? playlist.getTracks().get(0) : playlist.getSelectedTrack();
                loadSingle(single, null);
            } else if (playlist.getSelectedTrack() != null) {
                AudioTrack single = playlist.getSelectedTrack();
                loadAll(single, playlist);
            } else {
                int count = loadPlaylist(playlist, null);
                if (count == 0) {
                    m.editOriginal(FormatUtil.filter(event.getClient().getWarning() + L.t("play.all_tracks_playlist") + (playlist.getName() == null ? "" : "(**" + playlist.getName()
                            + "**) ") + L.t("play.exceed_maximum_allowed_length") + bot.getConfig().getMaxTime() + "`)")).queue();
                } else {
                    m.editOriginal(FormatUtil.filter(event.getClient().getSuccess()
                            + (playlist.getName() == null ? L.t("spotify_link.playlist") : L.t("play.playlist") + playlist.getName() + "**") + L.t("play.text_5")
                            + playlist.getTracks().size() + L.t("play.tracks_added_queue_2")
                            + (count < playlist.getTracks().size() ? "\n" + event.getClient().getWarning() + L.t("play.tracks_exceeding_maximum_allowed_length")
                            + bot.getConfig().getMaxTime() + L.t("play.were_skipped") : ""))).queue();
                }
            }
        }

        @Override
        public void noMatches() {
            if (ytsearch)
                m.editOriginal(FormatUtil.filter(event.getClient().getWarning() + L.t("play.no_search_results_found") + event.getOption("input").getAsString() + "`.")).queue();
            else
                bot.getPlayerManager().loadItemOrdered(event.getGuild(), "ytsearch:" + event.getOption("input").getAsString(), new SlashResultHandler(m, event, true));
        }

        @Override
        public void loadFailed(FriendlyException throwable) {
            if (throwable.severity == Severity.COMMON) {
                m.editOriginal(event.getClient().getError() + L.t("play.error_occurred_while_loading") + dev.cosgy.jmusicbot.util.ErrorTranslator.tr(throwable.getMessage())).queue();
            } else {

                m.editOriginal(event.getClient().getError() + L.t("play.error_occurred_while_loading_track")).queue();
            }
        }
    }

    private class ResultHandler implements AudioLoadResultHandler {
        private final Message m;
        private final CommandEvent event;
        private final boolean ytsearch;

        private ResultHandler(Message m, CommandEvent event, boolean ytsearch) {
            this.m = m;
            this.event = event;
            this.ytsearch = ytsearch;
        }

        private void loadSingle(AudioTrack track, AudioPlaylist playlist) {
            if (bot.getConfig().isTooLong(track)) {
                m.editMessage(FormatUtil.filter(event.getClient().getWarning() +
                                " **" + track.getInfo().title + "**`(" + FormatUtil.formatTime(track.getDuration()) + L.t("play.exceeds_configured_maximum_length") + FormatUtil.formatTime(bot.getConfig().getMaxSeconds() * 1000) + L.t("play.text_2")))
                        .queue();
                return;
            }
            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            int pos = handler.addTrack(new QueuedTrack(track, event.getAuthor())) + 1;
            String addMsg = FormatUtil.filter(event.getClient().getSuccess() + " **" + (track.getInfo().uri.contains("https://stream.gensokyoradio.net/") ? L.t("queue_paginator_manager.gensokyo_radio") : track.getInfo().title)
                    + "** (`" + FormatUtil.formatTime(track.getDuration()) + "`) " + (pos == 0 ? L.t("play.started_playing") : L.t("play.was_added_queue_at_position") + pos + L.t("play.text_3")));

            // If no playlist or cannot send components, just edit the message
            if (playlist == null || !event.getSelfMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_SEND)) {
                m.editMessage(addMsg).queue();
            } else {
                // unique IDs for this interaction
                String unique = Long.toHexString(System.currentTimeMillis());
                String loadId = "play:load:" + unique;
                String cancelId = "play:cancel:" + unique;
                Button loadButton = Button.primary(loadId, LOAD);
                Button cancelButton = Button.danger(cancelId, CANCEL);

                m.editMessage(addMsg + "\n" + event.getClient().getWarning() + L.t("play.playlist_containing_track_has") + playlist.getTracks().size()
                                + L.t("play.more_tracks_load_them_all") + LOAD + L.t("play.text_4"))
                        .setComponents(ActionRow.of(loadButton, cancelButton))
                        .queue();
                // wait for a button click
                bot.getWaiter().waitForEvent(ButtonInteractionEvent.class,
                        (e) -> e.getComponentId().equals(loadId) || e.getComponentId().equals(cancelId),
                        (e) -> {
                            e.deferEdit().queue();
                            if (e.getComponentId().equals(loadId)) {
                                int count = loadPlaylist(playlist, track);
                                m.editMessage(addMsg + "\n" + event.getClient().getSuccess() + "**" + count + L.t("play.tracks_added_queue"))
                                        .setComponents()
                                        .queue();
                            } else {
                                m.editMessage(addMsg)
                                        .setComponents()
                                        .queue();
                            }
                        },
                        30, TimeUnit.SECONDS,
                        () -> m.editMessage(addMsg).setComponents().queue());
            }
        }

        /**
         * When a track comes with a list containing it (e.g. a YouTube Mix/Radio or a link with list=),
         * adds the track and all tracks in the list to the queue without asking.
         */
        private void loadAll(AudioTrack selected, AudioPlaylist playlist) {
            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            boolean selectedTooLong = bot.getConfig().isTooLong(selected);
            int pos = selectedTooLong ? -2 : handler.addTrack(new QueuedTrack(selected, event.getAuthor())) + 1;

            // Keep the YouTube order: tracks after the selected one first, the earlier ones at the end
            List<AudioTrack> all = playlist.getTracks();
            int idx = -1;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i) == selected || all.get(i).getIdentifier().equals(selected.getIdentifier())) { idx = i; break; }
            }
            List<AudioTrack> ordered = new ArrayList<>();
            if (idx >= 0) {
                ordered.addAll(all.subList(idx + 1, all.size()));
                ordered.addAll(all.subList(0, idx));
            } else {
                ordered.addAll(all);
            }
            java.util.Set<String> ids = new java.util.HashSet<>();
            String selId = dev.cosgy.jmusicbot.util.YoutubeMixExpander.videoId(selected);
            if (selId != null) ids.add(selId);
            int count = 0, skipped = 0;
            boolean mix = dev.cosgy.jmusicbot.util.YoutubeMixExpander.isMix(event.getArgs());
            for (AudioTrack t : ordered) {
                // Total limit for radio playlists (YoutubeMixExpander.MAX_TRACKS)
                if (mix && ids.size() >= dev.cosgy.jmusicbot.util.YoutubeMixExpander.MAX_TRACKS) break;
                if (bot.getConfig().isTooLong(t)) { skipped++; continue; }
                String vid = dev.cosgy.jmusicbot.util.YoutubeMixExpander.videoId(t);
                if (vid != null && !ids.add(vid)) continue; // don't add the same track twice
                handler.addTrack(new QueuedTrack(t, event.getAuthor()));
                count++;
            }

            String title = track_title(selected);
            StringBuilder msg = new StringBuilder();
            if (selectedTooLong) {
                msg.append(event.getClient().getWarning()).append(" **").append(title).append(L.t("play.was_not_added_because_exceeds"));
            } else {
                msg.append(event.getClient().getSuccess()).append(" **").append(title).append("** (`").append(FormatUtil.formatTime(selected.getDuration())).append("`) ")
                        .append(pos == 0 ? L.t("play.started_playing_2") : L.t("play.was_added_queue_at_position_2") + pos + L.t("play.text_3"));
            }
            if (count > 0) msg.append("\n").append(event.getClient().getSuccess()).append(L.t("play.from_playlist")).append(count).append(L.t("youtube_mix_expander.more_tracks_added_queue"));
            if (skipped > 0) msg.append("\n").append(event.getClient().getWarning()).append(" ").append(skipped).append(L.t("play.tracks_were_skipped_because_they"));

            String url = event.getArgs();
            if (dev.cosgy.jmusicbot.util.YoutubeMixExpander.isMix(url) && bot.getPlayerManager().getYtDlpPath() != null
                    && ids.size() < dev.cosgy.jmusicbot.util.YoutubeMixExpander.MAX_TRACKS) {
                msg.append(L.t("play.youtube_radio_playlist_rest_being") + dev.cosgy.jmusicbot.util.YoutubeMixExpander.MAX_TRACKS + L.t("play.tracks_2"));
                dev.cosgy.jmusicbot.util.YoutubeMixExpander.expand(bot, event.getGuild(), event.getAuthor(), url, ids, event.getChannel());
            }
            m.editMessage(FormatUtil.filter(msg.toString())).setComponents().queue();
        }

        private String track_title(AudioTrack t) {
            return t.getInfo().uri.contains("https://stream.gensokyoradio.net/") ? L.t("queue_paginator_manager.gensokyo_radio") : t.getInfo().title;
        }

        private int loadPlaylist(AudioPlaylist playlist, AudioTrack exclude) {
            int[] count = {0};
            playlist.getTracks().forEach((track) -> {
                if (!bot.getConfig().isTooLong(track) && !track.equals(exclude)) {
                    AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                    handler.addTrack(new QueuedTrack(track, event.getAuthor()));
                    count[0]++;
                }
            });
            return count[0];
        }

        @Override
        public void trackLoaded(AudioTrack track) {
            loadSingle(track, null);
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            if (playlist.getTracks().size() == 1 || playlist.isSearchResult()) {
                AudioTrack single = playlist.getSelectedTrack() == null ? playlist.getTracks().get(0) : playlist.getSelectedTrack();
                loadSingle(single, null);
            } else if (playlist.getSelectedTrack() != null) {
                AudioTrack single = playlist.getSelectedTrack();
                loadAll(single, playlist);
            } else {
                int count = loadPlaylist(playlist, null);
                if (count == 0) {
                    m.editMessage(FormatUtil.filter(event.getClient().getWarning() + L.t("play.all_tracks_playlist") + (playlist.getName() == null ? "" : "(**" + playlist.getName()
                            + "**) ") + L.t("play.exceed_maximum_allowed_length") + bot.getConfig().getMaxTime() + "`)")).queue();
                } else {
                    m.editMessage(FormatUtil.filter(event.getClient().getSuccess()
                            + (playlist.getName() == null ? L.t("spotify_link.playlist") : L.t("play.playlist") + playlist.getName() + "**") + L.t("play.text_5")
                            + playlist.getTracks().size() + L.t("play.tracks_added_queue_2")
                            + (count < playlist.getTracks().size() ? "\n" + event.getClient().getWarning() + L.t("play.tracks_exceeding_maximum_allowed_length")
                            + bot.getConfig().getMaxTime() + L.t("play.were_skipped") : ""))).queue();
                }
            }
        }

        @Override
        public void noMatches() {
            if (ytsearch)
                m.editMessage(FormatUtil.filter(event.getClient().getWarning() + L.t("play.no_search_results_found") + event.getArgs() + "`.")).queue();
            else
                bot.getPlayerManager().loadItemOrdered(event.getGuild(), "ytsearch:" + event.getArgs(), new ResultHandler(m, event, true));
        }

        @Override
        public void loadFailed(FriendlyException throwable) {
            if (throwable.severity == Severity.COMMON) {
                m.editMessage(event.getClient().getError() + L.t("play.error_occurred_while_loading") + dev.cosgy.jmusicbot.util.ErrorTranslator.tr(throwable.getMessage())).queue();
            } else {
                if (event.getAuthor().getIdLong() == bot.getConfig().getOwnerId() || (event.getMember() != null && event.getMember().isOwner())) {
                    m.editMessage(event.getClient().getError() + L.t("play.error_occurred_while_loading_track_2") +
                            L.t("play.error_details") + throwable.getLocalizedMessage() + "**").queue();
                    StackTraceUtil.sendStackTrace(event.getChannel(), throwable);
                    return;
                }

                m.editMessage(event.getClient().getError() + L.t("play.error_occurred_while_loading_track")).queue();
            }
        }
    }

    public class RequestCmd extends MusicCommand {
        private final static String LOAD = "\uD83D\uDCE5"; // 📥
        private final static String CANCEL = "\uD83D\uDEAB"; // 🚫

        private final String loadingEmoji;
        private final JDA jda;

        public RequestCmd(Bot bot) {
            super(bot);
            this.jda = bot.getJDA();
            this.loadingEmoji = bot.getConfig().getLoading();
            this.name = "request";
            this.arguments = L.t("play.track_name_url");
            this.help = L.t("play.requests_track");
            this.aliases = bot.getConfig().getAliases(this.name);
            this.beListening = true;
            this.bePlaying = false;

            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "input", L.t("play.url_track_name_spotify_links"), false));
            this.options = options;

        }

        @Override
        public void doCommand(CommandEvent event) {
        }

        @Override
        public void doCommand(SlashCommandEvent event) {

            if (event.getOption("input") == null) {
                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                if (handler.getPlayer().getPlayingTrack() != null && handler.getPlayer().isPaused()) {
                    if (DJCommand.checkDJPermission(event.getClient(), event)) {

                        handler.getPlayer().setPaused(false);
                        event.reply(event.getClient().getSuccess() + "**" + handler.getPlayer().getPlayingTrack().getInfo().title + L.t("play.resumed_playing")).queue();
                        bot.getNowplayingHandler().onTrackUpdate(event.getGuild().getIdLong(), handler.getPlayer().getPlayingTrack(), handler);

                        Bot.updatePlayStatus(event.getGuild(), event.getGuild().getSelfMember(), PlayStatus.PLAYING);
                    } else
                        event.reply(event.getClient().getError() + L.t("play.only_dj_can_resume_playback")).queue();
                    return;
                }

                // Loading mechanism from the cache
                if (bot.getCacheLoader().cacheExists(event.getGuild().getId())) {
                    List<Cache> data = bot.getCacheLoader().GetCache(event.getGuild().getId());

                    AtomicInteger count = new AtomicInteger();
                    CacheLoader.CacheResult cache = bot.getCacheLoader().ConvertCache(data);
                    event.reply(L.t("play.calling_loading_cache_file") + cache.getItems().size() + L.t("play.tracks")).queue(m -> {
                        cache.loadTracks(bot.getPlayerManager(), (at) -> {
                            // TODO: use the user ID saved in the cache.
                            handler.addTrack(new QueuedTrack(at, event.getUser()));
                            count.getAndIncrement();
                        }, () -> {
                            StringBuilder builder = new StringBuilder(cache.getTracks().isEmpty()
                                    ? event.getClient().getWarning() + L.t("play.no_tracks_could_loaded")
                                    : event.getClient().getSuccess() + L.t("play.cache_file") + "**" + cache.getTracks().size() + L.t("play.tracks_loaded"));
                            if (!cache.getErrors().isEmpty())
                                builder.append(L.t("play.these_tracks_could_not_loaded"));
                            cache.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex() + 1).append("]` **").append(err.getItem()).append("**: ").append(err.getReason()));
                            String str = builder.toString();
                            if (str.length() > 2000)
                                str = str.substring(0, 1994) + L.t("play.truncated");
                            m.editOriginal(FormatUtil.filter(str)).queue();
                        });
                    });
                    try {
                        bot.getCacheLoader().deleteCache(event.getGuild().getId());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }

                if (handler.playFromDefault()) {
                    Settings settings = event.getClient().getSettingsFor(event.getGuild());
                    handler.stopAndClear();
                    Playlist playlist = bot.getPlaylistLoader().getPlaylist(event.getGuild().getId(), settings.getDefaultPlaylist());
                    if (playlist == null) {
                        event.reply(L.t("play.text") + settings.getDefaultPlaylist() + L.t("play.txt_was_not_found_playlist")).queue();
                        return;
                    }
                    event.reply(loadingEmoji + L.t("play.loading_playlist") + settings.getDefaultPlaylist() + "** ... (" + playlist.getItems().size() + L.t("play.tracks")).queue(m ->
                    {

                        playlist.loadTracks(bot.getPlayerManager(), (at) -> handler.addTrack(new QueuedTrack(at, event.getUser())), () -> {
                            StringBuilder builder = new StringBuilder(playlist.getTracks().isEmpty()
                                    ? event.getClient().getWarning() + L.t("play.no_tracks_could_loaded_2")
                                    : event.getClient().getSuccess() + " ** " + playlist.getTracks().size() + L.t("play.tracks_loaded_2"));
                            if (!playlist.getErrors().isEmpty())
                                builder.append(L.t("play.these_tracks_could_not_loaded"));
                            playlist.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex() + 1).append("]` **").append(err.getItem()).append("**: ").append(err.getReason()));
                            String str = builder.toString();
                            if (str.length() > 2000)
                                str = str.substring(0, 1994) + " (...)";
                            m.editOriginal(FormatUtil.filter(str)).queue();
                        });
                    });
                    return;

                }

                StringBuilder builder = new StringBuilder(event.getClient().getWarning() + L.t("play.play_command"));
                builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(L.t("play.track_name_plays_first_result"));
                builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(L.t("play.url_plays_specified_track_playlist"));
                for (Command cmd : children)
                    builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" ").append(cmd.getName()).append(" ").append(cmd.getArguments()).append("` - ").append(cmd.getHelp());
                event.reply(builder.toString()).queue();
                return;
            }
            event.reply(loadingEmoji + "`[" + event.getOption("input").getAsString() + L.t("play.loading")).queue(m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), event.getOption("input").getAsString(), new SlashResultHandler(m, event, false)));
        }


    }


    public class PlaylistCmd extends MusicCommand {
        public PlaylistCmd(Bot bot) {
            super(bot);
            this.name = "playlist";
            this.aliases = new String[]{"pl"};
            this.arguments = L.t("play.name");
            this.help = L.t("play.plays_specified_playlist");
            this.beListening = true;
            this.bePlaying = false;

            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", L.t("play.playlist_name"), true));
            this.options = options;
        }

        @Override
        public void doCommand(CommandEvent event) {
            String guildId = event.getGuild().getId();
            if (event.getArgs().isEmpty()) {
                event.reply(event.getClient().getError() + L.t("play.enter_playlist_name"));
                return;
            }
            Playlist playlist = bot.getPlaylistLoader().getPlaylist(guildId, event.getArgs());
            if (playlist == null) {
                event.replyError("`" + event.getArgs() + L.t("play.txt_not_found"));
                return;
            }
            event.getChannel().sendMessage(L.t("play.calling_loading_playlist") + event.getArgs() + L.t("play.text_6") + playlist.getItems().size() + L.t("play.tracks")).queue(m ->
            {
                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                playlist.loadTracks(bot.getPlayerManager(), (at) -> handler.addTrack(new QueuedTrack(at, event.getAuthor())), () -> {
                    StringBuilder builder = new StringBuilder(playlist.getTracks().isEmpty()
                            ? event.getClient().getWarning() + L.t("play.no_tracks_could_loaded")
                            : event.getClient().getSuccess() + "**" + playlist.getTracks().size() + L.t("play.tracks_loaded"));
                    if (!playlist.getErrors().isEmpty())
                        builder.append(L.t("play.these_tracks_could_not_loaded"));
                    playlist.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex() + 1).append("]` **").append(err.getItem()).append("**: ").append(err.getReason()));
                    String str = builder.toString();
                    if (str.length() > 2000)
                        str = str.substring(0, 1994) + L.t("play.truncated");
                    m.editMessage(FormatUtil.filter(str)).queue();
                });
            });
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            String guildId = event.getGuild().getId();

            String name = event.getOption("name").getAsString();

            Playlist playlist = bot.getPlaylistLoader().getPlaylist(guildId, name);
            if (playlist == null) {
                event.reply(event.getClient().getError() + "`" + name + L.t("play.txt_not_found")).queue();
                return;
            }
            event.reply(L.t("play.calling_loading_playlist") + name + L.t("play.text_6") + playlist.getItems().size() + L.t("play.tracks")).queue(m ->
            {
                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                playlist.loadTracks(bot.getPlayerManager(), (at) -> handler.addTrack(new QueuedTrack(at, event.getUser())), () -> {
                    StringBuilder builder = new StringBuilder(playlist.getTracks().isEmpty()
                            ? event.getClient().getWarning() + L.t("play.no_tracks_could_loaded")
                            : event.getClient().getSuccess() + "**" + playlist.getTracks().size() + L.t("play.tracks_loaded"));
                    if (!playlist.getErrors().isEmpty())
                        builder.append(L.t("play.these_tracks_could_not_loaded"));
                    playlist.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex() + 1).append("]` **").append(err.getItem()).append("**: ").append(err.getReason()));
                    String str = builder.toString();
                    if (str.length() > 2000)
                        str = str.substring(0, 1994) + L.t("play.truncated");
                    m.editOriginal(FormatUtil.filter(str)).queue();
                });
            });
        }
    }

    public class MylistCmd extends MusicCommand {
        public MylistCmd(Bot bot) {
            super(bot);
            this.name = "mylist";
            this.aliases = new String[]{"ml"};
            this.arguments = L.t("play.name");
            this.help = L.t("play.plays_personal_list");
            this.beListening = true;
            this.bePlaying = false;

            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", L.t("play.personal_list_name"), true));
            this.options = options;
        }

        @Override
        public void doCommand(CommandEvent event) {
            String userId = event.getAuthor().getId();
            if (event.getArgs().isEmpty()) {
                event.reply(event.getClient().getError() + L.t("play.enter_personal_list_name"));
                return;
            }
            MylistLoader.Playlist playlist = bot.getMylistLoader().getPlaylist(userId, event.getArgs());
            if (playlist == null) {
                event.replyError("`" + event.getArgs() + L.t("play.txt_not_found_2"));
                return;
            }
            event.getChannel().sendMessage(L.t("play.calling_loading_personal_list") + event.getArgs() + L.t("play.text_6") + playlist.getItems().size() + L.t("play.tracks")).queue(m ->
            {
                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                playlist.loadTracks(bot.getPlayerManager(), (at) -> handler.addTrack(new QueuedTrack(at, event.getAuthor())), () -> {
                    StringBuilder builder = new StringBuilder(playlist.getTracks().isEmpty()
                            ? event.getClient().getWarning() + L.t("play.no_tracks_could_loaded")
                            : event.getClient().getSuccess() + "**" + playlist.getTracks().size() + L.t("play.tracks_loaded"));
                    if (!playlist.getErrors().isEmpty())
                        builder.append(L.t("play.these_tracks_could_not_loaded"));
                    playlist.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex() + 1)
                            .append("]` **").append(err.getItem()).append("**: ").append(err.getReason()));
                    String str = builder.toString();
                    if (str.length() > 2000)
                        str = str.substring(0, 1994) + L.t("play.truncated");
                    m.editMessage(FormatUtil.filter(str)).queue();
                });
            });
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            String userId = event.getUser().getId();

            String name = event.getOption("name").getAsString();

            MylistLoader.Playlist playlist = bot.getMylistLoader().getPlaylist(userId, name);
            if (playlist == null) {
                event.reply(event.getClient().getError() + "`" + name + L.t("play.txt_not_found_2")).queue();
                return;
            }
            event.reply(L.t("play.calling_loading_personal_list") + name + L.t("play.text_6") + playlist.getItems().size() + L.t("play.tracks")).queue(m ->
            {
                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                playlist.loadTracks(bot.getPlayerManager(), (at) -> handler.addTrack(new QueuedTrack(at, event.getUser())), () -> {
                    StringBuilder builder = new StringBuilder(playlist.getTracks().isEmpty()
                            ? event.getClient().getWarning() + L.t("play.no_tracks_could_loaded")
                            : event.getClient().getSuccess() + "**" + playlist.getTracks().size() + L.t("play.tracks_loaded"));
                    if (!playlist.getErrors().isEmpty())
                        builder.append(L.t("play.these_tracks_could_not_loaded"));
                    playlist.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex() + 1)
                            .append("]` **").append(err.getItem()).append("**: ").append(err.getReason()));
                    String str = builder.toString();
                    if (str.length() > 2000)
                        str = str.substring(0, 1994) + L.t("play.truncated");
                    m.editOriginal(FormatUtil.filter(str)).queue();
                });
            });
        }
    }

    public class PublistCmd extends MusicCommand {
        public PublistCmd(Bot bot) {
            super(bot);
            this.name = "publist";
            this.aliases = new String[]{"pul"};
            this.arguments = L.t("play.name");
            this.help = L.t("play.plays_public_list");
            this.beListening = true;
            this.bePlaying = false;

            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", L.t("play.public_list_name"), true));
            this.options = options;
        }

        @Override
        public void doCommand(CommandEvent event) {
            if (event.getArgs().isEmpty()) {
                event.reply(event.getClient().getError() + L.t("play.enter_playlist_name_2"));
                return;
            }
            PubliclistLoader.Playlist playlist = bot.getPublistLoader().getPlaylist(event.getArgs());
            if (playlist == null) {
                event.replyError("`" + event.getArgs() + L.t("play.txt_not_found_2"));
                return;
            }
            event.getChannel().sendMessage(L.t("play.calling_loading_playlist") + event.getArgs() + L.t("play.text_6") + playlist.getItems().size() + L.t("play.tracks")).queue(m ->
            {
                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                playlist.loadTracks(bot.getPlayerManager(), (at) -> handler.addTrack(new QueuedTrack(at, event.getAuthor())), () -> {
                    StringBuilder builder = new StringBuilder(playlist.getTracks().isEmpty()
                            ? event.getClient().getWarning() + L.t("play.no_tracks_could_loaded")
                            : event.getClient().getSuccess() + "**" + playlist.getTracks().size() + L.t("play.tracks_loaded"));
                    if (!playlist.getErrors().isEmpty())
                        builder.append(L.t("play.these_tracks_could_not_loaded"));
                    playlist.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex() + 1)
                            .append("]` **").append(err.getItem()).append("**: ").append(err.getReason()));
                    String str = builder.toString();
                    if (str.length() > 2000)
                        str = str.substring(0, 1994) + L.t("play.truncated");
                    m.editMessage(FormatUtil.filter(str)).queue();
                });
            });
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            String name = event.getOption("name").getAsString();
            PubliclistLoader.Playlist playlist = bot.getPublistLoader().getPlaylist(name);
            if (playlist == null) {
                event.reply(event.getClient().getError() + "`" + name + L.t("play.txt_not_found_2")).queue();
                return;
            }
            event.reply(L.t("play.calling_loading_playlist") + name + L.t("play.text_6") + playlist.getItems().size() + L.t("play.tracks")).queue(m ->
            {
                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                playlist.loadTracks(bot.getPlayerManager(), (at) -> handler.addTrack(new QueuedTrack(at, event.getUser())), () -> {
                    StringBuilder builder = new StringBuilder(playlist.getTracks().isEmpty()
                            ? event.getClient().getWarning() + L.t("play.no_tracks_could_loaded")
                            : event.getClient().getSuccess() + "**" + playlist.getTracks().size() + L.t("play.tracks_loaded"));
                    if (!playlist.getErrors().isEmpty())
                        builder.append(L.t("play.these_tracks_could_not_loaded"));
                    playlist.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex() + 1)
                            .append("]` **").append(err.getItem()).append("**: ").append(err.getReason()));
                    String str = builder.toString();
                    if (str.length() > 2000)
                        str = str.substring(0, 1994) + L.t("play.truncated");
                    m.editOriginal(FormatUtil.filter(str)).queue();
                });
            });
        }
    }
}
