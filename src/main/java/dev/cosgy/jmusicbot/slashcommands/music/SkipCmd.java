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

import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.RequestMetadata;
import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandClient;
import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import dev.cosgy.jmusicbot.slashcommands.MusicCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;

/**
 * Votes to skip the current track. The person who requested the track can skip directly;
 * others need as many votes as the server's skip ratio (default 55%).
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class SkipCmd extends MusicCommand {
    public SkipCmd(Bot bot) {
        super(bot);
        this.name = "skip";
        this.help = L.t("skip.votes_skip_current_track");
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = true;
        this.bePlaying = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        event.reply(vote(event.getGuild(), event.getAuthor(), event.getClient()));
    }

    @Override
    public void doCommand(SlashCommandEvent event) {
        event.reply(vote(event.getGuild(), event.getUser(), event.getClient())).queue();
    }

    private String vote(Guild guild, User user, CommandClient client) {
        AudioHandler handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
        RequestMetadata rm = handler.getRequestMetadata();
        String title = currentTitle(handler);

        // The person who requested the track can skip without a vote
        if (user.getIdLong() == rm.getOwner()) {
            handler.getPlayer().stopTrack();
            return client.getSuccess() + " **" + title + L.t("skip.skipped");
        }

        AudioChannel channel = guild.getSelfMember().getVoiceState().getChannel();
        if (channel == null) {
            return client.getError() + L.t("skip.bot_not_voice_channel_right");
        }

        // Listeners in the voice channel (excluding bots, deafened users and the requester)
        int listeners = (int) channel.getMembers().stream()
                .filter(m -> !m.getUser().isBot() && !m.getVoiceState().isDeafened() && m.getUser().getIdLong() != rm.getOwner())
                .count();

        String msg;
        if (handler.getVotes().contains(user.getId())) {
            msg = client.getWarning() + L.t("skip.already_voted_skip_track");
        } else {
            handler.getVotes().add(user.getId());
            msg = client.getSuccess() + L.t("skip.voted_skip_track");
        }

        int skippers = (int) channel.getMembers().stream()
                .filter(m -> handler.getVotes().contains(m.getUser().getId()))
                .count();
        double ratio = bot.getSettingsManager().getSettings(guild).getSkipRatio();
        int required = Math.max(1, (int) Math.ceil(listeners * ratio));

        msg += " `[" + skippers + L.t("skip.votes") + required + "/" + listeners + L.t("skip.needed");

        if (skippers >= required) {
            msg += "\n" + client.getSuccess() + " **" + title + L.t("skip.skipped_2")
                    + (rm.getOwner() == 0L ? L.t("skip.autoplay") : "(**" + rm.user.username + L.t("skip.requested"));
            handler.getPlayer().stopTrack();
        }
        return msg;
    }

    private static String currentTitle(AudioHandler handler) {
        if (handler.getPlayer().getPlayingTrack() == null) return L.t("spotify_link.track");
        return handler.getPlayer().getPlayingTrack().getInfo().uri.contains("https://stream.gensokyoradio.net/")
                ? L.t("queue_paginator_manager.gensokyo_radio")
                : handler.getPlayer().getPlayingTrack().getInfo().title;
    }
}
