/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
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
package dev.cosgy.jmusicbot.slashcommands.admin;

import dev.cosgy.jmusicbot.i18n.L;

import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommand;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.settings.Settings;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import dev.cosgy.jmusicbot.slashcommands.AdminCommand;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class SetvcCmd extends AdminCommand {
    public SetvcCmd(Bot bot) {
        this.name = "setvc";
        this.help = L.t("setvc.locks_playback_specific_voice_channel");
        this.arguments = L.t("setvc.channel_name_none");
        this.aliases = bot.getConfig().getAliases(this.name);

        this.children = new SlashCommand[]{new Set(), new None()};
    }

    @Override
    protected void execute(SlashCommandEvent slashCommandEvent) {
    }

    @Override
    protected void execute(CommandEvent event) {
        Logger log = LoggerFactory.getLogger("SetVcCmd");
        if (event.getArgs().isEmpty()) {
            event.reply(event.getClient().getError() + L.t("setvc.enter_voice_channel_none"));
            return;
        }
        Settings s = event.getClient().getSettingsFor(event.getGuild());
        if (event.getArgs().toLowerCase().matches("(none|yok|hicbiri|hiçbiri)")) {
            s.setVoiceChannel(null);
            event.reply(event.getClient().getSuccess() + L.t("setvc.music_can_now_played_any"));
        } else {
            List<VoiceChannel> list = FinderUtil.findVoiceChannels(event.getArgs(), event.getGuild());
            if (list.isEmpty())
                event.reply(event.getClient().getWarning() + L.t("setvc.no_matching_voice_channel_found") + event.getArgs() + "\"");
            else if (list.size() > 1)
                event.reply(event.getClient().getWarning() + FormatUtil.listOfVChannels(list, event.getArgs()));
            else {
                s.setVoiceChannel(list.get(0));
                log.info(L.t("setvc.music_channel_set"));
                event.reply(event.getClient().getSuccess() + L.t("setvc.music_can_now_only_played") + list.get(0).getAsMention() + L.t("setvc.text"));
            }
        }
    }

    private static class Set extends AdminCommand {
        public Set() {
            this.name = "set";
            this.help = L.t("setvc.set_voice_channel_used_playback");

            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.CHANNEL, "channel", L.t("setvc.voice_channel"), true));

            this.options = options;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            if (checkAdminPermission(event.getClient(), event)) {
                event.reply(event.getClient().getWarning() + L.t("autoplaylist.don_t_have_permission_do")).queue();
                return;
            }
            Settings s = event.getClient().getSettingsFor(event.getGuild());
            Long channel = event.getOption("channel").getAsLong();

            if (event.getOption("channel").getChannelType() != ChannelType.VOICE) {
                event.reply(event.getClient().getError() + L.t("setvc.please_specify_voice_channel")).queue();
            }

            VoiceChannel vc = event.getGuild().getVoiceChannelById(channel);
            s.setVoiceChannel(vc);
            event.reply(event.getClient().getSuccess() + L.t("setvc.music_can_now_only_played") + vc.getAsMention() + L.t("setvc.text")).queue();
        }
    }

    private static class None extends AdminCommand {
        public None() {
            this.name = "none";
            this.help = L.t("setvc.resets_playback_voice_channel_setting");
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            if (checkAdminPermission(event.getClient(), event)) {
                event.reply(event.getClient().getWarning() + L.t("autoplaylist.don_t_have_permission_do")).queue();
                return;
            }
            Settings s = event.getClient().getSettingsFor(event.getGuild());
            s.setVoiceChannel(null);
            event.reply(event.getClient().getSuccess() + L.t("setvc.music_can_now_played_any")).queue();
        }

        @Override
        protected void execute(CommandEvent event) {
            Settings s = event.getClient().getSettingsFor(event.getGuild());
            s.setVoiceChannel(null);
            event.replySuccess(L.t("setvc.music_can_now_played_any"));
        }
    }
}
