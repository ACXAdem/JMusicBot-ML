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
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class SettcCmd extends AdminCommand {
    public SettcCmd(Bot bot) {
        this.name = "settc";
        this.help = L.t("settc.sets_bot_s_command_channel");
        this.arguments = L.t("setvc.channel_name_none");
        this.aliases = bot.getConfig().getAliases(this.name);

        this.children = new SlashCommand[]{new Set(), new None()};
    }

    @Override
    protected void execute(SlashCommandEvent event) {
    }

    // This is the normal (prefix) command
    @Override
    protected void execute(CommandEvent event) {
        Logger log = LoggerFactory.getLogger("SettcCmd");
        if (event.getArgs().isEmpty()) {
            event.reply(event.getClient().getError() + L.t("settc.enter_channel_none"));
            return;
        }
        Settings s = event.getClient().getSettingsFor(event.getGuild());
        if (event.getArgs().toLowerCase().matches("(none|yok|hicbiri|hiçbiri)")) {
            s.setTextChannel(null);
            event.reply(event.getClient().getSuccess() + L.t("settc.music_commands_can_now_used"));
        } else {
            List<TextChannel> list = FinderUtil.findTextChannels(event.getArgs(), event.getGuild());
            if (list.isEmpty())
                event.reply(event.getClient().getWarning() + L.t("settc.no_matching_channel_found") + event.getArgs() + "\"");
            else if (list.size() > 1)
                event.reply(event.getClient().getWarning() + FormatUtil.listOfTChannels(list, event.getArgs()));
            else {
                s.setTextChannel(list.get(0));
                log.info(L.t("settc.music_command_channel_set"));
                event.reply(event.getClient().getSuccess() + L.t("settc.music_commands_can_now_only") + list.get(0).getId() + L.t("settc.text"));
            }
        }
    }

    private static class Set extends AdminCommand {
        public Set() {
            this.name = "set";
            this.help = L.t("settc.set_channel_music_commands");

            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.CHANNEL, "channel", L.t("settc.text_channel"), true));

            this.options = options;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            if (checkAdminPermission(event.getClient(), event)) {
                event.reply(event.getClient().getWarning() + L.t("autoplaylist.don_t_have_permission_do")).queue();
                return;
            }
            Settings s = event.getClient().getSettingsFor(event.getGuild());


            if (event.getOption("channel").getChannelType() != ChannelType.TEXT) {
                event.reply(event.getClient().getError() + L.t("settc.please_specify_text_channel")).queue();
                return;
            }
            Long channelId = event.getOption("channel").getAsLong();
            TextChannel tc = event.getGuild().getTextChannelById(channelId);

            s.setTextChannel(tc);
            event.reply(client.getSuccess() + L.t("settc.music_commands_can_now_only") + tc.getId() + L.t("settc.text")).queue();

        }
    }

    private static class None extends AdminCommand {
        public None() {
            this.name = "none";
            this.help = L.t("settc.removes_channel_restriction_music_commands");
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            if (checkAdminPermission(event.getClient(), event)) {
                event.reply(event.getClient().getWarning() + L.t("autoplaylist.don_t_have_permission_do")).queue();
                return;
            }
            Settings s = event.getClient().getSettingsFor(event.getGuild());
            s.setTextChannel(null);
            event.reply(event.getClient().getSuccess() + L.t("settc.music_commands_can_now_used")).queue();
        }

        @Override
        protected void execute(CommandEvent event) {
            Settings s = event.getClient().getSettingsFor(event.getGuild());
            s.setTextChannel(null);
            event.replySuccess(L.t("settc.music_commands_can_now_used"));
        }
    }

}
