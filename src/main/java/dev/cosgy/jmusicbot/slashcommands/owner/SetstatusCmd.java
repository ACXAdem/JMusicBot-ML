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
package dev.cosgy.jmusicbot.slashcommands.owner;

import dev.cosgy.jmusicbot.i18n.L;

import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import dev.cosgy.jmusicbot.slashcommands.OwnerCommand;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class SetstatusCmd extends OwnerCommand {
    public SetstatusCmd(Bot bot) {
        this.name = "setstatus";
        this.help = L.t("setstatus.sets_bot_s_status_online");
        this.arguments = L.t("setstatus.status");
        this.aliases = bot.getConfig().getAliases(this.name);
        //this.guildOnly = false;
        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.STRING, "status", L.t("setstatus.one_online_idle_dnd_invisible"), true));
        this.options = options;
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        try {
            OnlineStatus status = OnlineStatus.fromKey(event.getOption("status").getAsString());
            if (status == OnlineStatus.UNKNOWN) {
                event.reply(event.getClient().getError() + L.t("setstatus.enter_one_online_idle_dnd")).queue();
            } else {
                event.getJDA().getPresence().setStatus(status);
                event.reply(event.getClient().getSuccess() + L.t("setstatus.status_set") + status.getKey().toUpperCase() + L.t("prefix.text")).queue();
            }
        } catch (Exception e) {
            event.reply(event.getClient().getError() + L.t("setstatus.could_not_set_status")).queue();
        }
    }

    @Override
    protected void execute(CommandEvent event) {
        try {
            OnlineStatus status = OnlineStatus.fromKey(event.getArgs());
            if (status == OnlineStatus.UNKNOWN) {
                event.replyError(L.t("setstatus.enter_one_online_idle_dnd"));
            } else {
                event.getJDA().getPresence().setStatus(status);
                event.replySuccess(L.t("setstatus.status_set") + status.getKey().toUpperCase() + L.t("prefix.text"));
            }
        } catch (Exception e) {
            event.reply(event.getClient().getError() + L.t("setstatus.could_not_set_status"));
        }
    }
}
