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
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.settings.Settings;
import dev.cosgy.jmusicbot.slashcommands.MusicCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class RemoveCmd extends MusicCommand {
    public RemoveCmd(Bot bot) {
        super(bot);
        this.name = "remove";
        this.help = L.t("remove.removes_track_from_queue");
        this.arguments = L.t("remove.position_all");
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = true;
        this.bePlaying = true;
        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.STRING, "input", L.t("remove.position_all_2"), true));
        this.options = options;
    }

    @Override
    public void doCommand(CommandEvent event) {
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        if (handler.getQueue().isEmpty()) {
            event.replyError(L.t("remove.there_nothing_queue"));
            return;
        }
        if (event.getArgs().toLowerCase().matches("(all|hepsi|tümü|tumu)")) {
            int count = handler.getQueue().removeAll(event.getAuthor().getIdLong());
            if (count == 0)
                event.replyWarning(L.t("remove.have_no_tracks_queue"));
            else
                event.replySuccess(count + L.t("remove.tracks_removed"));
            return;
        }
        int pos;
        try {
            pos = Integer.parseInt(event.getArgs());
        } catch (NumberFormatException e) {
            pos = 0;
        }
        if (pos < 1 || pos > handler.getQueue().size()) {
            event.replyError(String.format(L.t("remove.please_enter_valid_number_between"), handler.getQueue().size()));
            return;
        }
        Settings settings = event.getClient().getSettingsFor(event.getGuild());
        boolean isDJ = event.getMember().hasPermission(Permission.MANAGE_SERVER);
        if (!isDJ)
            isDJ = event.getMember().getRoles().contains(settings.getRole(event.getGuild()));
        QueuedTrack qt = handler.getQueue().get(pos - 1);
        if (qt.getIdentifier() == event.getAuthor().getIdLong()) {
            handler.getQueue().remove(pos - 1);
            event.replySuccess("**" + (qt.getTrack().getInfo().uri.contains("https://stream.gensokyoradio.net/") ? L.t("queue_paginator_manager.gensokyo_radio") : qt.getTrack().getInfo().title) + L.t("remove.removed_from_queue"));
        } else if (isDJ) {
            handler.getQueue().remove(pos - 1);
            User u;
            try {
                u = event.getJDA().getUserById(qt.getIdentifier());
            } catch (Exception e) {
                u = null;
            }
            event.replySuccess("**" + qt.getTrack().getInfo().title
                    + L.t("remove.removed_from_queue_track_was") + (u == null ? L.t("remove.someone") : "**" + u.getName() + L.t("remove.text")) + ")");
        } else {
            event.replyError("**" + (qt.getTrack().getInfo().uri.contains("https://stream.gensokyoradio.net/") ? L.t("queue_paginator_manager.gensokyo_radio") : qt.getTrack().getInfo().title) + L.t("remove.could_not_removed_reason_do"));
        }
    }

    @Override
    public void doCommand(SlashCommandEvent event) {
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        if (handler.getQueue().isEmpty()) {
            event.reply(event.getClient().getError() + L.t("remove.there_nothing_queue")).queue();
            return;
        }

        if (event.getOption("input").getAsString().toLowerCase().matches("(all|hepsi|tümü|tumu)")) {
            int count = handler.getQueue().removeAll(event.getUser().getIdLong());
            if (count == 0)
                event.reply(event.getClient().getWarning() + L.t("remove.have_no_tracks_queue")).queue();
            else
                event.reply(event.getClient().getSuccess() + count + L.t("remove.tracks_removed")).queue();
            return;
        }
        int pos;
        try {
            pos = Integer.parseInt(event.getOption("input").getAsString());
        } catch (NumberFormatException e) {
            pos = 0;
        }
        if (pos < 1 || pos > handler.getQueue().size()) {
            event.reply(event.getClient().getError() + String.format(L.t("remove.please_enter_valid_number_between"), handler.getQueue().size())).queue();
            return;
        }
        Settings settings = event.getClient().getSettingsFor(event.getGuild());
        boolean isDJ = event.getMember().hasPermission(Permission.MANAGE_SERVER);
        if (!isDJ)
            isDJ = event.getMember().getRoles().contains(settings.getRole(event.getGuild()));
        QueuedTrack qt = handler.getQueue().get(pos - 1);
        if (qt.getIdentifier() == event.getUser().getIdLong()) {
            handler.getQueue().remove(pos - 1);
            event.reply(event.getClient().getSuccess() + "**" + qt.getTrack().getInfo().title + L.t("remove.removed_from_queue")).queue();
        } else if (isDJ) {
            handler.getQueue().remove(pos - 1);
            User u;
            try {
                u = event.getJDA().getUserById(qt.getIdentifier());
            } catch (Exception e) {
                u = null;
            }
            event.reply(event.getClient().getSuccess() + "**" + qt.getTrack().getInfo().title
                    + L.t("remove.removed_from_queue_track_was") + (u == null ? L.t("remove.someone") : "**" + u.getName() + L.t("remove.text")) + ")").queue();
        } else {
            event.reply(event.getClient().getError() + "**" + qt.getTrack().getInfo().title + L.t("remove.could_not_removed_reason_do")).queue();
        }
    }
}
