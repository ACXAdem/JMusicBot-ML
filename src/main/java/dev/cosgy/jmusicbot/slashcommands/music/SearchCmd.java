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

import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.menu.OrderedMenu;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.cosgy.jmusicbot.slashcommands.MusicCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class SearchCmd extends MusicCommand {
    private final OrderedMenu.Builder builder;
    private final String searchingEmoji;
    protected String searchPrefix = "ytsearch:";

    public SearchCmd(Bot bot) {
        super(bot);
        this.searchingEmoji = bot.getConfig().getSearching();
        this.name = "search";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.arguments = L.t("search.search");
        this.help = L.t("search.searches_youtube_videos_with_given");
        this.beListening = true;
        this.bePlaying = false;
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};

        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.STRING, "input", L.t("search.search_term"), true));
        this.options = options;

        builder = new OrderedMenu.Builder()
                .allowTextInput(true)
                .useNumbers()
                .useCancelButton(true)
                .setEventWaiter(bot.getWaiter())
                .setTimeout(1, TimeUnit.MINUTES);
    }

    @Override
    public void doCommand(CommandEvent event) {
        if (event.getArgs().isEmpty()) {
            event.replyError(L.t("search.please_enter_something_search"));
            return;
        }
        event.reply(searchingEmoji + "`[" + event.getArgs() + L.t("search.searching"),
                m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), searchPrefix + event.getArgs(), new ResultHandler(m, event)));
    }

    @Override
    public void doCommand(SlashCommandEvent event) {
        event.reply(searchingEmoji + "`[" + event.getOption("input").getAsString() + L.t("search.searching")).queue(
                m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), searchPrefix + event.getOption("input").getAsString(), new SlashResultHandler(m, event)));
    }

    private class SlashResultHandler implements AudioLoadResultHandler {
        private final InteractionHook m;
        private final SlashCommandEvent event;

        private SlashResultHandler(InteractionHook m, SlashCommandEvent event) {
            this.m = m;
            this.event = event;
        }

        @Override
        public void trackLoaded(AudioTrack track) {
            if (bot.getConfig().isTooLong(track)) {
                m.editOriginal(FormatUtil.filter(event.getClient().getWarning() + "**" + track.getInfo().title + L.t("search.exceeds_maximum_allowed_length")
                        + FormatUtil.formatTime(track.getDuration()) + "` > `" + bot.getConfig().getMaxTime() + "`")).queue();
                return;
            }
            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            int pos = handler.addTrack(new QueuedTrack(track, event.getUser())) + 1;
            m.editOriginal(FormatUtil.filter(event.getClient().getSuccess() + "**" + track.getInfo().title
                    + "**(`" + FormatUtil.formatTime(track.getDuration()) + "`) " + (pos == 0 ? L.t("play.started_playing")
                    : L.t("play.was_added_queue_at_position") + pos + L.t("play.text_3")))).queue();
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            builder.setColor(event.getGuild().getSelfMember().getColor())
                    .setText(FormatUtil.filter(event.getClient().getSuccess() + " `" + event.getOption("input").getAsString() + L.t("search.search_results")))
                    .setChoices()
                    .setSelection((msg, i) ->
                    {
                        AudioTrack track = playlist.getTracks().get(i - 1);
                        if (bot.getConfig().isTooLong(track)) {
                            event.reply(event.getClient().getWarning() + "**" + track.getInfo().title + L.t("search.exceeds_maximum_allowed_length")
                                    + FormatUtil.formatTime(track.getDuration()) + "` > `" + bot.getConfig().getMaxTime() + "`").queue();
                            return;
                        }
                        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                        int pos = handler.addTrack(new QueuedTrack(track, event.getUser())) + 1;
                        event.reply(event.getClient().getSuccess() + "**" + track.getInfo().title
                                + "**(`" + FormatUtil.formatTime(track.getDuration()) + "`) " + (pos == 0 ? L.t("play.started_playing")
                                : L.t("play.was_added_queue_at_position") + pos + L.t("search.text"))).queue();
                    })
                    .setCancel((msg) -> {
                    })
                    .setUsers(event.getUser())
            ;
            for (int i = 0; i < 4 && i < playlist.getTracks().size(); i++) {
                AudioTrack track = playlist.getTracks().get(i);
                builder.addChoices("`[" + FormatUtil.formatTime(track.getDuration()) + "]` [**" + track.getInfo().title + "**](" + track.getInfo().uri + ")");
            }
            builder.build().display(event.getChannel());
        }

        @Override
        public void noMatches() {
            m.editOriginal(FormatUtil.filter(event.getClient().getWarning() + L.t("play.no_search_results_found") + event.getOption("input").getAsString() + "`.")).queue();
        }

        @Override
        public void loadFailed(FriendlyException throwable) {

            if (throwable.severity == Severity.COMMON)
                m.editOriginal(event.getClient().getError() + L.t("play.error_occurred_while_loading") + dev.cosgy.jmusicbot.util.ErrorTranslator.tr(throwable.getMessage())).queue();
            else
                m.editOriginal(event.getClient().getError() + L.t("search.error_occurred_while_loading")).queue();
        }
    }

    private class ResultHandler implements AudioLoadResultHandler {
        private final Message m;
        private final CommandEvent event;

        private ResultHandler(Message m, CommandEvent event) {
            this.m = m;
            this.event = event;
        }

        @Override
        public void trackLoaded(AudioTrack track) {
            if (bot.getConfig().isTooLong(track)) {
                m.editMessage(FormatUtil.filter(event.getClient().getWarning() + L.t("search.track") + track.getInfo().title + L.t("search.exceeds_maximum_allowed_length_2")
                        + FormatUtil.formatTime(track.getDuration()) + "` > `" + bot.getConfig().getMaxTime() + "`")).queue();
                return;
            }
            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            int pos = handler.addTrack(new QueuedTrack(track, event.getAuthor())) + 1;
            m.editMessage(FormatUtil.filter(event.getClient().getSuccess() + " **" + track.getInfo().title
                    + "** (`" + FormatUtil.formatTime(track.getDuration()) + "`) " + (pos == 0 ? L.t("play.started_playing")
                    : L.t("play.was_added_queue_at_position") + pos + L.t("play.text_3")))).queue();
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            builder.setColor(event.getSelfMember().getColor())
                    .setText(FormatUtil.filter(event.getClient().getSuccess() + " `" + event.getArgs() + L.t("search.search_results")))
                    .setChoices()
                    .setSelection((msg, i) ->
                    {
                        AudioTrack track = playlist.getTracks().get(i - 1);
                        if (bot.getConfig().isTooLong(track)) {
                            event.replyWarning(L.t("search.track_2") + track.getInfo().title + L.t("search.exceeds_maximum_allowed_length_3")
                                    + FormatUtil.formatTime(track.getDuration()) + "` > `" + bot.getConfig().getMaxTime() + "`");
                            return;
                        }
                        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                        int pos = handler.addTrack(new QueuedTrack(track, event.getAuthor())) + 1;
                        event.replySuccess("**" + FormatUtil.filter(track.getInfo().title)
                                + "** (`" + FormatUtil.formatTime(track.getDuration()) + "`) " + (pos == 0 ? L.t("play.started_playing")
                                : L.t("play.was_added_queue_at_position") + pos + L.t("play.text_3")));
                    })
                    .setCancel((msg) -> {
                    })
                    .setUsers(event.getAuthor())
            ;
            for (int i = 0; i < 4 && i < playlist.getTracks().size(); i++) {
                AudioTrack track = playlist.getTracks().get(i);
                builder.addChoices("`[" + FormatUtil.formatTime(track.getDuration()) + "]` [**" + track.getInfo().title + "**](" + track.getInfo().uri + ")");
            }
            builder.build().display(m);
        }

        @Override
        public void noMatches() {
            m.editMessage(FormatUtil.filter(event.getClient().getWarning() + " `" + event.getArgs() + L.t("search.returned_no_results"))).queue();
        }

        @Override
        public void loadFailed(FriendlyException throwable) {
            if (throwable.severity == Severity.COMMON)
                m.editMessage(event.getClient().getError() + L.t("search.load_error") + dev.cosgy.jmusicbot.util.ErrorTranslator.tr(throwable.getMessage())).queue();
            else
                m.editMessage(event.getClient().getError() + L.t("search.could_not_load_track")).queue();
        }
    }
}
