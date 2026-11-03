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

import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.entities.Pair;
import com.jagrosh.jmusicbot.settings.Settings;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class NowplayingHandler {
    private final Bot bot;
    private final java.util.Map<Long, Pair<Long, Long>> lastNP; // server -> channel, message

    public NowplayingHandler(Bot bot) {
        this.bot = bot;
        this.lastNP = new java.util.concurrent.ConcurrentHashMap<>();
    }

    public void init() {
        // Update the progress bar of the last "now playing" card every 15 seconds
        bot.getThreadpool().scheduleWithFixedDelay(this::updateAll, 0, 15, TimeUnit.SECONDS);
    }

    public void setLastNPMessage(Message m) {
        lastNP.put(m.getGuild().getIdLong(), new Pair<>(m.getChannel().getIdLong(), m.getIdLong()));
    }

    public void clearLastNPMessage(Guild guild) {
        lastNP.remove(guild.getIdLong());
    }

    private void updateAll() {
        try {
            for (long guildId : new HashSet<>(lastNP.keySet())) refresh(guildId);
        } catch (Exception e) {
            // the scheduled task must never stop on an error
        }
    }

    /** Updates the server's last "now playing" card right away (when the track changes or a button is pressed). */
    public void refresh(long guildId) {
        Pair<Long, Long> pair = lastNP.get(guildId);
        if (pair == null) return;
        Guild guild = bot.getJDA() == null ? null : bot.getJDA().getGuildById(guildId);
        if (guild == null) {
            lastNP.remove(guildId);
            return;
        }
        GuildMessageChannel ch = guild.getChannelById(GuildMessageChannel.class, pair.getKey());
        AudioHandler handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
        if (ch == null || handler == null) {
            lastNP.remove(guildId);
            return;
        }
        MessageCreateData data;
        try {
            data = handler.getNowPlaying(bot.getJDA());
        } catch (Exception e) {
            return;
        }
        boolean ended = data == null;
        if (ended) data = handler.getNoMusicPlaying(bot.getJDA());
        MessageEditData edit = new MessageEditBuilder().applyCreateData(data).setComponents(data.getComponents()).build();
        try {
            ch.editMessageById(pair.getValue(), edit).queue(m -> {
            }, t -> lastNP.remove(guildId));
        } catch (Exception e) {
            lastNP.remove(guildId);
        }
        if (ended) lastNP.remove(guildId);
    }

    public void updateTopic(long guildId, AudioHandler handler, boolean wait) {
        Guild guild = bot.getJDA().getGuildById(guildId);
        if (guild == null)
            return;
        Settings settings = bot.getSettingsManager().getSettings(guildId);
        TextChannel tchan = settings.getTextChannel(guild);
        if (tchan != null && guild.getSelfMember().hasPermission(tchan, Permission.MANAGE_CHANNEL)) {
            String otherText;
            String topic = tchan.getTopic();
            if (topic == null || topic.isEmpty())
                otherText = "\u200B";
            else if (topic.contains("\u200B"))
                otherText = topic.substring(topic.lastIndexOf("\u200B"));
            else
                otherText = "\u200B\n " + topic;
            String text = handler.getTopicFormat(bot.getJDA()) + otherText;
            if (!text.equals(tchan.getTopic())) {
                try {
                    tchan.getManager().setTopic(text).complete(wait);
                } catch (PermissionException | RateLimitedException ignore) {
                }
            }
        }

        // Update the voice channel status
        GuildVoiceState vChan = guild.getSelfMember().getVoiceState();

        if(vChan == null || !vChan.inAudioChannel()){
            return;
        }

        AudioChannelUnion chan = vChan.getChannel();
        if (!(chan instanceof VoiceChannel)) {
            return;
        }

        VoiceChannel voiceChannel = (VoiceChannel) chan;

        if(settings.getVCStatus() && guild.getSelfMember().hasPermission(voiceChannel, Permission.VOICE_SET_STATUS)){
            String text = handler.getTopicFormat(bot.getJDA());
            if (!text.equals(voiceChannel.getStatus())) {
                try {
                    voiceChannel.modifyStatus(text).complete(wait);
                } catch (PermissionException | RateLimitedException ignore) {
                }
            }
        } else {
            clearVoiceChannelStatus(voiceChannel, guild, wait);
        }


    }

    public void clearVoiceChannelStatus(long guildId, boolean wait) {
        Guild guild = bot.getJDA().getGuildById(guildId);
        if (guild == null) {
            return;
        }

        AudioChannelUnion chan = guild.getAudioManager().getConnectedChannel();
        if (!(chan instanceof VoiceChannel)) {
            GuildVoiceState voiceState = guild.getSelfMember().getVoiceState();
            chan = voiceState != null ? voiceState.getChannel() : null;
            if (!(chan instanceof VoiceChannel)) {
                return;
            }
        }

        clearVoiceChannelStatus((VoiceChannel) chan, guild, wait);
    }

    public void clearVoiceChannelStatus(VoiceChannel voiceChannel, Guild guild, boolean wait) {
        if (voiceChannel == null || guild == null) {
            return;
        }
        if (!guild.getSelfMember().hasPermission(voiceChannel, Permission.VOICE_SET_STATUS)) {
            return;
        }

        String currentStatus = voiceChannel.getStatus();
        if (currentStatus == null || currentStatus.isBlank()) {
            return;
        }

        try {
            voiceChannel.modifyStatus(null).complete(wait);
        } catch (IllegalArgumentException ignored) {
            try {
                voiceChannel.modifyStatus("").complete(wait);
            } catch (PermissionException | RateLimitedException ignored2) {
            }
        } catch (PermissionException | RateLimitedException ignore) {
        }
    }

    // "event"-based methods
    public void onTrackUpdate(long guildId, AudioTrack track, AudioHandler handler) {
        // Updates the bot's status if applicable
        if (bot.getConfig().getSongInStatus()) {
            if (track != null && bot.getJDA().getGuilds().stream().filter(g -> Objects.requireNonNull(g.getSelfMember().getVoiceState()).inAudioChannel()).count() <= 1)

                if (track.getInfo().uri.matches(".*stream.gensokyoradio.net/.*")) {
                    bot.getJDA().getPresence().setActivity(Activity.listening(L.t("queue_paginator_manager.gensokyo_radio")));
                } else {
                    bot.getJDA().getPresence().setActivity(Activity.listening(track.getInfo().title));
                }
            else
                bot.resetGame();
        }

        // Updates the channel topic if applicable
        updateTopic(guildId, handler, false);

        // Update the open "now playing" card for the new track
        try {
            refresh(guildId);
        } catch (Exception ignored) {
        }
    }

    public void onMessageDelete(Guild guild, long messageId) {
        Pair<Long, Long> pair = lastNP.get(guild.getIdLong());
        if (pair == null)
            return;
        if (pair.getValue() == messageId)
            lastNP.remove(guild.getIdLong());
    }
}
