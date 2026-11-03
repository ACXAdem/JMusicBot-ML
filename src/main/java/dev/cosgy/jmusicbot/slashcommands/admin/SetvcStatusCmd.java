/*
 *  Copyright 2024 Cosgy Dev (info@cosgy.dev).
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
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommand;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.settings.Settings;
import dev.cosgy.jmusicbot.slashcommands.AdminCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Objects;

public class SetvcStatusCmd extends AdminCommand {
    public SetvcStatusCmd(Bot bot) {
        this.name = "setvcstatus";
        this.help = L.t("setvc_status.sets_whether_current_track_shown");
        this.arguments = "<true|false>";
        this.aliases = bot.getConfig().getAliases(this.name);

        this.options = List.of(
                new OptionData(OptionType.BOOLEAN, "status", L.t("setvc_status.true_off_false"), true)
        );
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        if (checkAdminPermission(event.getClient(), event)) {
            event.reply(event.getClient().getWarning() + L.t("autoplaylist.don_t_have_permission_do")).queue();
            return;
        }

        var status = Objects.requireNonNull(event.getOption("status")).getAsBoolean();
        Settings s = event.getClient().getSettingsFor(event.getGuild());
        s.setVCStatus(status);

        event.reply(event.getClient().getSuccess() + L.t("setvc_status.show_current_track_voice_channel") + status + L.t("prefix.text")).queue();
    }

    @Override
    protected void execute(CommandEvent event) {

        if (event.getArgs().isEmpty()) {
            event.reply(event.getClient().getError() + L.t("setvc_status.enter_true_false"));
            return;
        }

        Settings s = event.getClient().getSettingsFor(event.getGuild());

        if (event.getArgs().toLowerCase().matches("(false|kapali|kapalı|hayir|hayır|off)")) {
            s.setVCStatus(false);
            event.reply(event.getClient().getSuccess() + L.t("setvc_status.current_track_will_no_longer"));
        }else if(event.getArgs().toLowerCase().matches("(true|acik|açık|evet|on)")) {
            s.setVCStatus(true);
            event.reply(event.getClient().getSuccess() + L.t("setvc_status.current_track_will_now_shown"));
        }else {
            event.reply(event.getClient().getError() + L.t("setvc_status.enter_true_false"));
        }
    }
}
