/*
 * Copyright 2020 John Grosh <john.a.grosh@gmail.com>.
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
import com.jagrosh.jmusicbot.audio.RequestMetadata;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.cosgy.jmusicbot.slashcommands.DJCommand;
import dev.cosgy.jmusicbot.slashcommands.MusicCommand;
import dev.cosgy.jmusicbot.util.TimeUtil;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Whew., Inc.
 */
public class SeekCmd extends MusicCommand
{
    private final static Logger LOG = LoggerFactory.getLogger("Seeking");

    public SeekCmd(Bot bot)
    {
        super(bot);
        this.name = "seek";
        this.help = L.t("seek.changes_position_current_track_seek");
        this.arguments = "[+ | -] <SS:DD:ss | DD:ss | ss>|<0h0m0s | 0m0s | 0s>";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = true;
        this.bePlaying = true;

        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.STRING, "time", L.t("seek.format_1_02_23_1"), true));
        this.options = options;
    }

    @Override
    public void doCommand(CommandEvent event)
    {
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        AudioTrack playingTrack = handler.getPlayer().getPlayingTrack();
        if (!playingTrack.isSeekable())
        {
            event.replyError(L.t("seek.track_cannot_seeked"));
            return;
        }


        if (!DJCommand.checkDJPermission(event) && com.jagrosh.jmusicbot.audio.AudioHandler.extractRequestMetadata(playingTrack).getOwner() != event.getAuthor().getIdLong())
        {
            event.replyError("**" + playingTrack.getInfo().title + L.t("seek.was_not_added_by_so"));
            return;
        }

        String args = event.getArgs();
        TimeUtil.SeekTime seekTime = TimeUtil.parseTime(args);
        if (seekTime == null)
        {
            event.replyError(L.t("seek.invalid_position_expected_format") + arguments + L.t("seek.example_1_02_23_1"));
            return;
        }

        long currentPosition = playingTrack.getPosition();
        long trackDuration = playingTrack.getDuration();

        long seekMilliseconds = seekTime.relative ? currentPosition + seekTime.milliseconds : seekTime.milliseconds;
        if (seekMilliseconds > trackDuration)
        {
            event.replyError(L.t("seek.current_track") + TimeUtil.formatTime(trackDuration) + L.t("seek.long_so_cannot_seek") + TimeUtil.formatTime(seekMilliseconds) + L.t("seek.text"));
            return;
        }

        try
        {
            playingTrack.setPosition(seekMilliseconds);
        }
        catch (Exception e)
        {
            event.replyError(L.t("seek.error_while_seeking") + e.getMessage());
            LOG.warn(L.t("seek.could_not_seek_track") + playingTrack.getIdentifier() + L.t("seek.text_2"), e);
            return;
        }
        event.replySuccess("`" + TimeUtil.formatTime(playingTrack.getPosition()) + "/" + TimeUtil.formatTime(playingTrack.getDuration()) + L.t("seek.text_3"));
    }

    @Override
    public void doCommand(SlashCommandEvent event) {
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        AudioTrack playingTrack = handler.getPlayer().getPlayingTrack();
        if (!playingTrack.isSeekable())
        {
            event.reply(L.t("seek.track_cannot_seeked")).queue();
            return;
        }

        if (!DJCommand.checkDJPermission(event.getClient(), event) && com.jagrosh.jmusicbot.audio.AudioHandler.extractRequestMetadata(playingTrack).getOwner() != event.getUser().getIdLong())
        {
            event.reply("**" + playingTrack.getInfo().title + L.t("seek.was_not_added_by_so")).queue();
            return;
        }

        String args = event.getOption("time").getAsString();
        TimeUtil.SeekTime seekTime = TimeUtil.parseTime(args);
        if (seekTime == null)
        {
            event.reply(L.t("seek.invalid_position_expected_format") + arguments + L.t("seek.example_1_02_23_1")).queue();
            return;
        }

        long currentPosition = playingTrack.getPosition();
        long trackDuration = playingTrack.getDuration();

        long seekMilliseconds = seekTime.relative ? currentPosition + seekTime.milliseconds : seekTime.milliseconds;
        if (seekMilliseconds > trackDuration)
        {
            event.reply(L.t("seek.current_track") + TimeUtil.formatTime(trackDuration) + L.t("seek.long_so_cannot_seek") + TimeUtil.formatTime(seekMilliseconds) + L.t("seek.text")).queue();
            return;
        }

        try
        {
            playingTrack.setPosition(seekMilliseconds);
        }
        catch (Exception e)
        {
            event.reply(L.t("seek.error_while_seeking") + e.getMessage()).queue();
            LOG.warn(L.t("seek.could_not_seek_track_2"), playingTrack.getIdentifier(), e);
            return;
        }
        event.reply("`" + TimeUtil.formatTime(playingTrack.getPosition()) + "/" + TimeUtil.formatTime(playingTrack.getDuration()) + L.t("seek.text_3")).queue();
    }

}