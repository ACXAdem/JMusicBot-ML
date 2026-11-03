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
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.settings.Settings;
import dev.cosgy.jmusicbot.slashcommands.AdminCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class PrefixCmd extends AdminCommand {
    public PrefixCmd(Bot bot) {
        this.name = "prefix";
        this.help = L.t("prefix.sets_server_specific_command_prefix");
        this.arguments = L.t("prefix.prefix_none");
        this.aliases = bot.getConfig().getAliases(this.name);
        //this.children = new SlashCommand[]{new None()};

        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.STRING, "prefix", L.t("prefix.prefix_set"), true));

        this.options = options;
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        if (checkAdminPermission(event.getClient(), event)) {
            event.reply(event.getClient().getWarning() + L.t("autoplaylist.don_t_have_permission_do")).queue();
            return;
        }
        Settings s = event.getClient().getSettingsFor(event.getGuild());
        String prefix = event.getOption("prefix").getAsString();
        if (prefix.toLowerCase().matches("(none|yok|hicbiri|hiçbiri)")) {
            s.setPrefix(null);
            event.reply(event.getClient().getSuccess() + L.t("prefix.custom_prefix_removed")).queue();
        } else {
            s.setPrefix(prefix);
            event.reply(event.getClient().getSuccess() + "*" + event.getGuild().getName() + L.t("prefix.command_prefix_set") + prefix + L.t("prefix.text")).queue();
        }
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getArgs().isEmpty()) {
            event.replyError(L.t("prefix.enter_prefix_none"));
            return;
        }

        Settings s = event.getClient().getSettingsFor(event.getGuild());
        if (event.getArgs().toLowerCase().matches("(none|yok|hicbiri|hiçbiri)")) {
            s.setPrefix(null);
            event.replySuccess(L.t("prefix.custom_prefix_removed"));
        } else {
            s.setPrefix(event.getArgs());
            event.replySuccess("*" + event.getGuild().getName() + L.t("prefix.command_prefix_set") + event.getArgs() + L.t("prefix.text"));
        }
    }
}
