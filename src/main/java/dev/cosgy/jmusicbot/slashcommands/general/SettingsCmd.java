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
package dev.cosgy.jmusicbot.slashcommands.general;

import dev.cosgy.jmusicbot.i18n.L;

import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommand;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.settings.Settings;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import dev.cosgy.jmusicbot.settings.RepeatMode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.Objects;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class SettingsCmd extends SlashCommand {
    private final static String EMOJI = "\uD83C\uDFA7"; // 🎧

    public SettingsCmd(Bot bot) {
        this.name = "settings";
        this.help = L.t("settings.shows_bot_s_settings_server");
        this.aliases = bot.getConfig().getAliases(this.name);
        //this.guildOnly = true;
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        Settings s = event.getClient().getSettingsFor(event.getGuild());
        MessageCreateBuilder builder = new MessageCreateBuilder()
                .addContent(EMOJI + " **")
                .addContent(FormatUtil.filter(event.getJDA().getSelfUser().getName()))
                .addContent(L.t("settings.settings"));
        TextChannel tChan = s.getTextChannel(event.getGuild());
        VoiceChannel vChan = s.getVoiceChannel(event.getGuild());
        Role role = s.getRole(event.getGuild());
        EmbedBuilder ebuilder = new EmbedBuilder()
                .setDescription(L.t("settings.command_channel") + (tChan == null ? L.t("settings.none") : "**#" + tChan.getName() + "**")
                        + L.t("settings.fixed_voice_channel") + (vChan == null ? L.t("settings.none") : "**" + vChan.getAsMention() + "**")
                        + L.t("settings.dj_role") + (role == null ? L.t("settings.not_set") : "**" + role.getName() + "**")
                        + L.t("settings.repeat") + (s.getRepeatMode() == RepeatMode.ALL ? L.t("settings.whole_queue") : (s.getRepeatMode() == RepeatMode.SINGLE ? L.t("settings.single_track") : L.t("now_playing_card.off"))) + "**"
                        + L.t("settings.volume") + (s.getVolume()) + "**"
                        + L.t("settings.queue_mode") + (s.isForceToEndQue() ? L.t("settings.normal_mode") : L.t("settings.fair_mode")) + "**"
                        + L.t("settings.default_playlist") + (s.getDefaultPlaylist() == null ? L.t("settings.none") : "**" + s.getDefaultPlaylist() + "**")
                )
                .setFooter(String.format(
                                L.t("settings.servers_connected_voice_channels"),
                                event.getJDA().getGuilds().size(),
                                event.getJDA().getGuilds().stream().filter(g -> Objects.requireNonNull(g.getSelfMember().getVoiceState()).inAudioChannel()).count()),
                        null);
        event.reply(builder.addEmbeds(ebuilder.build()).build()).queue();
    }

    @Override
    protected void execute(CommandEvent event) {
        Settings s = event.getClient().getSettingsFor(event.getGuild());
        MessageCreateBuilder builder = new MessageCreateBuilder()
                .addContent(EMOJI + " **")
                .addContent(FormatUtil.filter(event.getSelfUser().getName()))
                .addContent(L.t("settings.settings"));
        TextChannel tChan = s.getTextChannel(event.getGuild());
        VoiceChannel vChan = s.getVoiceChannel(event.getGuild());
        Role role = s.getRole(event.getGuild());
        EmbedBuilder ebuilder = new EmbedBuilder()
                .setColor(event.getSelfMember().getColor())
                .setDescription(L.t("settings.command_channel") + (tChan == null ? L.t("settings.none") : "**#" + tChan.getName() + "**")
                        + L.t("settings.fixed_voice_channel") + (vChan == null ? L.t("settings.none") : "**" + vChan.getName() + "**")
                        + L.t("settings.dj_role") + (role == null ? L.t("settings.not_set") : "**" + role.getName() + "**")
                        + L.t("settings.repeat") + (s.getRepeatMode() == RepeatMode.ALL ? L.t("settings.whole_queue") : (s.getRepeatMode() == RepeatMode.SINGLE ? L.t("settings.single_track") : L.t("now_playing_card.off"))) + "**"
                        + L.t("settings.queue_mode") + (s.isForceToEndQue() ? L.t("settings.normal_mode") : L.t("settings.fair_mode")) + "**"
                        + L.t("settings.default_playlist") + (s.getDefaultPlaylist() == null ? L.t("settings.none") : "**" + s.getDefaultPlaylist() + "**")
                )
                .setFooter(String.format(
                                L.t("settings.servers_connected_voice_channels"),
                                event.getJDA().getGuilds().size(),
                                event.getJDA().getGuilds().stream().filter(g -> Objects.requireNonNull(g.getSelfMember().getVoiceState()).inAudioChannel()).count()),
                        null);
        event.getChannel().sendMessage(builder.addEmbeds(ebuilder.build()).build()).queue();
    }

}
