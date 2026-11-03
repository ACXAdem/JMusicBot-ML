/*
 *  Copyright 2021 Cosgy Dev (info@cosgy.dev).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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

public class SkipratioCmd extends AdminCommand {
    public SkipratioCmd(Bot bot) {
        this.name = "setskip";
        this.help = L.t("skipratio.sets_server_specific_skip_ratio");
        this.arguments = "<0 - 100>";
        this.aliases = bot.getConfig().getAliases(this.name);

        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.INTEGER, "percent", L.t("skipratio.skip_ratio"), true));

        this.options = options;
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        try {
            int val = Integer.parseInt(event.getOption("percent").getAsString());
            if (val < 0 || val > 100) {
                event.reply(event.getClient().getError() + L.t("skipratio.value_must_between_0_100")).queue();
                return;
            }
            Settings s = event.getClient().getSettingsFor(event.getGuild());
            s.setSkipRatio(val / 100.0);

            event.reply(event.getClient().getSuccess() + "*" + event.getGuild().getName() + L.t("skipratio.listener_skip_ratio_set") + val + L.t("skipratio.text")).queue();
        } catch (NumberFormatException ex) {
            event.reply(event.getClient().getError() + L.t("skipratio.enter_whole_number_between_0")).queue();
        }
    }

    @Override
    protected void execute(CommandEvent event) {
        try {
            int val = Integer.parseInt(event.getArgs().endsWith("%") ? event.getArgs().substring(0, event.getArgs().length() - 1) : event.getArgs());
            if (val < 0 || val > 100) {
                event.replyError(L.t("skipratio.value_must_between_0_100"));
                return;
            }
            Settings s = event.getClient().getSettingsFor(event.getGuild());
            s.setSkipRatio(val / 100.0);

            event.replySuccess("*" + event.getGuild().getName() + L.t("skipratio.listener_skip_ratio_set") + val + L.t("skipratio.text"));
        } catch (NumberFormatException ex) {
            event.replyError(L.t("skipratio.enter_whole_number_between_0"));
        }
    }
}
