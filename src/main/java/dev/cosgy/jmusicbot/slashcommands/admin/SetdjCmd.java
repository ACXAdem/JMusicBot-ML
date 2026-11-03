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
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class SetdjCmd extends AdminCommand {
    public SetdjCmd(Bot bot) {
        this.name = "setdj";
        this.help = L.t("setdj.sets_dj_role_can_use");
        this.arguments = L.t("setdj.role_name_none");
        this.aliases = bot.getConfig().getAliases(this.name);

        this.children = new SlashCommand[]{new SetRole(), new None()};
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        if (checkAdminPermission(event.getClient(), event)) {
            event.reply(event.getClient().getWarning() + L.t("autoplaylist.don_t_have_permission_do")).queue();
            return;
        }
        Settings s = event.getClient().getSettingsFor(event.getGuild());

        if (event.getOption("role") != null) {
            s.setDJRole(event.getOption("role").getAsRole());
            event.reply(event.getClient().getSuccess() + L.t("setdj.dj_commands_can_now_used") + event.getOption("role").getAsRole().getName() + L.t("setdj.role")).queue();
            return;
        }
        if (event.getOption("none").getAsString().toLowerCase().matches("(none|yok|hicbiri|hiçbiri)")) {
            s.setDJRole(null);
            event.reply(event.getClient().getSuccess() + L.t("setdj.dj_role_reset_only_administrators")).queue();
        } else {
            event.reply(L.t("setdj.invalid_command")).queue();
        }
    }

    @Override
    protected void execute(CommandEvent event) {
        Logger log = LoggerFactory.getLogger("SetDjCmd");
        if (event.getArgs().isEmpty()) {
            event.reply(event.getClient().getError() + L.t("setdj.enter_role_name_none"));
            return;
        }
        Settings s = event.getClient().getSettingsFor(event.getGuild());
        if (event.getArgs().toLowerCase().matches("(none|yok|hicbiri|hiçbiri)")) {
            s.setDJRole(null);
            event.reply(event.getClient().getSuccess() + L.t("setdj.dj_role_reset_only_administrators"));
        } else {
            List<Role> list = FinderUtil.findRoles(event.getArgs(), event.getGuild());
            if (list.isEmpty())
                event.reply(event.getClient().getWarning() + L.t("setdj.role_not_found") + event.getArgs() + "\"");
            else if (list.size() > 1)
                event.reply(event.getClient().getWarning() + FormatUtil.listOfRoles(list, event.getArgs()));
            else {
                s.setDJRole(list.get(0));
                log.info(L.t("setdj.added_role_can_use_dj") + list.get(0).getName() + ")");
                event.reply(event.getClient().getSuccess() + L.t("setdj.dj_commands_can_now_used") + list.get(0).getName() + L.t("setdj.role"));
            }
        }
    }

    private static class SetRole extends AdminCommand {
        public SetRole() {
            this.name = "set";
            this.help = L.t("setdj.sets_role_grant_dj_permissions");

            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.ROLE, "role", L.t("setdj.role_grant"), true));
            this.options = options;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            Settings s = event.getClient().getSettingsFor(event.getGuild());
            Role role = event.getOption("role").getAsRole();

            s.setDJRole(role);
            event.reply(event.getClient().getSuccess() + L.t("setdj.dj_commands_can_now_used") + role.getName() + L.t("setdj.role")).queue();
        }
    }

    private static class None extends AdminCommand {
        public None() {
            this.name = "none";
            this.help = L.t("setdj.reset_dj_role");
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            Settings s = event.getClient().getSettingsFor(event.getGuild());
            s.setDJRole(null);
            event.reply(event.getClient().getSuccess() + L.t("setdj.dj_role_reset_only_administrators")).queue();
        }

        @Override
        protected void execute(CommandEvent event) {
            Settings s = event.getClient().getSettingsFor(event.getGuild());
            s.setDJRole(null);
            event.replySuccess(L.t("setdj.dj_role_reset_only_administrators"));
        }
    }

}
