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

package dev.cosgy.jmusicbot.slashcommands.dj;

import dev.cosgy.jmusicbot.i18n.L;


import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.queue.FairQueue;
import dev.cosgy.jmusicbot.slashcommands.DJCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Command that lets the user change the position of tracks in the queue.
 */
public class MoveTrackCmd extends DJCommand {

    public MoveTrackCmd(Bot bot) {
        super(bot);
        this.name = "movetrack";
        this.help = L.t("move_track.changes_order_tracks_queue");
        this.arguments = L.t("move_track.current_position_new_position");
        this.aliases = bot.getConfig().getAliases(this.name);
        this.bePlaying = true;

        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.INTEGER, "from", L.t("move_track.current_position_track"), true));
        options.add(new OptionData(OptionType.INTEGER, "to", L.t("move_track.new_position_move"), true));
        this.options = options;

    }

    private static boolean isUnavailablePosition(FairQueue<QueuedTrack> queue, int position) {
        return (position < 1 || position > queue.size());
    }

    @Override
    public void doCommand(CommandEvent event) {
        Logger log = LoggerFactory.getLogger("MoveTrack");
        int from;
        int to;

        String[] parts = event.getArgs().split("\\s+", 2);
        if (parts.length < 2) {
            event.replyError(L.t("move_track.please_enter_two_valid_numbers"));
            return;
        }

        try {
            // Validate the args
            from = Integer.parseInt(parts[0]);
            to = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            event.replyError(L.t("move_track.please_enter_two_valid_numbers"));
            return;
        }

        if (from == to) {
            event.replyError(L.t("move_track.track_cannot_moved_same_position"));
            return;
        }

        // Validate that from and to are available
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        FairQueue<QueuedTrack> queue = handler.getQueue();
        if (isUnavailablePosition(queue, from)) {
            String reply = String.format(L.t("move_track.not_valid_position_queue"), from);
            event.replyError(reply);
            return;
        }
        if (isUnavailablePosition(queue, to)) {
            String reply = String.format(L.t("move_track.not_valid_position_queue"), to);
            event.replyError(reply);
            return;
        }

        // Move the track
        QueuedTrack track = queue.moveItem(from - 1, to - 1);
        String trackTitle = track.getTrack().getInfo().title;
        String reply = String.format(L.t("move_track.moved_from_position"), trackTitle, from, to);
        log.info(event.getGuild().getName() + L.t("move_track.track_moved_from_position"), trackTitle, from, to);
        event.replySuccess(reply);
    }

    @Override
    public void doCommand(SlashCommandEvent event) {
        if (!checkDJPermission(event.getClient(), event)) {
            event.reply(event.getClient().getWarning() + L.t("autoplaylist.don_t_have_permission_do")).queue();
            return;
        }
        int from;
        int to;

        from = Integer.parseInt(event.getOption("from").getAsString());
        to = Integer.parseInt(event.getOption("to").getAsString());

        if (from == to) {
            event.reply(event.getClient().getError() + L.t("move_track.track_cannot_moved_same_position")).queue();
            return;
        }

        // Validate that from and to are available
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        FairQueue<QueuedTrack> queue = handler.getQueue();
        if (isUnavailablePosition(queue, from)) {
            String reply = String.format(L.t("move_track.not_valid_position_queue"), from);
            event.reply(event.getClient().getError() + reply).queue();
            return;
        }
        if (isUnavailablePosition(queue, to)) {
            String reply = String.format(L.t("move_track.not_valid_position_queue"), to);
            event.reply(event.getClient().getError() + reply).queue();
            return;
        }

        // Move the track
        QueuedTrack track = queue.moveItem(from - 1, to - 1);
        String trackTitle = track.getTrack().getInfo().title;
        String reply = String.format(L.t("move_track.moved_from_position"), trackTitle, from, to);
        event.reply(event.getClient().getSuccess() + reply).queue();
    }
}
