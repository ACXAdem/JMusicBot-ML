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
package dev.cosgy.jmusicbot.slashcommands;

import dev.cosgy.jmusicbot.i18n.L;

import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommand;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.settings.Settings;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public abstract class MusicCommand extends SlashCommand {
    protected final Bot bot;
    protected boolean bePlaying;
    protected boolean beListening;
    Logger log = LoggerFactory.getLogger("MusicCommand");

    public MusicCommand(Bot bot) {
        this.bot = bot;
        //this.guildOnly = true;
        this.category = new Category(L.t("now_playing_card.music"));
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        Settings settings = event.getClient().getSettingsFor(event.getGuild());
        TextChannel channel = settings.getTextChannel(event.getGuild());

        bot.getPlayerManager().setUpHandler(event.getGuild());
        if (bePlaying && !((AudioHandler) event.getGuild().getAudioManager().getSendingHandler()).isMusicPlaying(event.getJDA())) {
            event.reply(event.getClient().getError() + L.t("music.music_must_playing_use_command")).queue();
            return;
        }
        if (beListening) {
            AudioChannelUnion current = event.getGuild().getSelfMember().getVoiceState().getChannel();

            if (current == null)
                current = (AudioChannelUnion) settings.getVoiceChannel(event.getGuild());
            GuildVoiceState userState = event.getMember().getVoiceState();

            if (!userState.inAudioChannel() || userState.isDeafened() || (current != null && !userState.getChannel().equals(current))) {
                event.reply(event.getClient().getError() + String.format(L.t("music.must_use_command"), (current == null ? L.t("music.voice_channel") : "**" + current.getAsMention() + "**"))).queue();
                return;
            }
            if (!event.getGuild().getSelfMember().getVoiceState().inAudioChannel()) {
                try {
                    event.getGuild().getAudioManager().openAudioConnection(userState.getChannel());
                    //event.getGuild().getAudioManager().setSelfDeafened(true);
                    event.getGuild().getAudioManager().setSelfMuted(false);
                } catch (PermissionException ex) {
                    event.reply(event.getClient().getError() + String.format(L.t("music.cannot_connect"), userState.getChannel().getAsMention())).queue();
                    return;
                }
                if (userState.getChannel().getType() == ChannelType.STAGE) {
                    event.getChannel().sendMessage(event.getClient().getWarning() + String.format(L.t("music.i_joined_stage_channel_use"), event.getGuild().getSelfMember().getNickname())).queue();
                }
            }
        }

        doCommand(event);
    }

    @Override
    protected void execute(CommandEvent event) {
        Settings settings = event.getClient().getSettingsFor(event.getGuild());
        TextChannel channel = settings.getTextChannel(event.getGuild());

        if (channel != null && event.getChannel().getIdLong() != channel.getIdLong()) {
            try {
                event.getMessage().delete().queue();
            } catch (PermissionException ignore) {
            }
            event.replyInDm(event.getClient().getError() + String.format(L.t("music.commands_can_only_used"), channel.getAsMention()));
            return;
        }
        bot.getPlayerManager().setUpHandler(event.getGuild()); // no point constantly checking for this later

        if (bePlaying && !((AudioHandler) event.getGuild().getAudioManager().getSendingHandler()).isMusicPlaying(event.getJDA())) {
            event.reply(event.getClient().getError() + L.t("music.music_must_playing_use_command"));
            return;
        }
        if (beListening) {
            AudioChannelUnion current = event.getGuild().getSelfMember().getVoiceState().getChannel();

            if (current == null)
                current = (AudioChannelUnion) settings.getVoiceChannel(event.getGuild());
            GuildVoiceState userState = event.getMember().getVoiceState();
            if (!userState.inAudioChannel() || userState.isDeafened() || (current != null && !userState.getChannel().equals(current))) {
                event.replyError(String.format(L.t("music.must_use_command"), (current == null ? L.t("music.voice_channel") : "**" + current.getName() + "**")));
                return;
            }
            if (!event.getGuild().getSelfMember().getVoiceState().inAudioChannel()) {
                try {
                    event.getGuild().getAudioManager().openAudioConnection(userState.getChannel());
                } catch (PermissionException ex) {
                    event.reply(event.getClient().getError() + String.format(L.t("music.cannot_connect"), userState.getChannel().getName()));
                    return;
                }
                if (userState.getChannel().getType() == ChannelType.STAGE) {
                    event.getChannel().sendMessage(event.getClient().getWarning() + String.format(L.t("music.i_joined_stage_channel_use"), event.getGuild().getSelfMember().getNickname())).queue();
                }
            }
        }

        doCommand(event);
    }

    public abstract void doCommand(CommandEvent event);

    public abstract void doCommand(SlashCommandEvent event);
}
