/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
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
package dev.cosgy.jmusicbot.slashcommands.dj;

import dev.cosgy.jmusicbot.i18n.L;

import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import dev.cosgy.jmusicbot.slashcommands.DJCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class SkipToCmd extends DJCommand {
    Logger log = LoggerFactory.getLogger("Skip");

    public SkipToCmd(Bot bot) {
        super(bot);
        this.name = "skipto";
        this.help = L.t("skip_to.skips_specified_track");
        this.arguments = L.t("skip_to.position");
        this.aliases = bot.getConfig().getAliases(this.name);
        this.bePlaying = true;

        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.INTEGER, "position", L.t("skip_to.position_queue"), true));
        this.options = options;

    }

    @Override
    public void doCommand(CommandEvent event) {
        int index = 0;
        try {
            index = Integer.parseInt(event.getArgs());
        } catch (NumberFormatException e) {
            event.reply(event.getClient().getError() + " `" + event.getArgs() + L.t("skip_to.not_valid_whole_number"));
            return;
        }
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        if (index < 1 || index > handler.getQueue().size()) {
            event.reply(event.getClient().getError() + L.t("skip_to.must_enter_whole_number_between") + handler.getQueue().size() + L.t("skip_to.text"));
            return;
        }
        handler.getQueue().skip(index - 1);
        event.reply(event.getClient().getSuccess() + " **" + handler.getQueue().get(0).getTrack().getInfo().title + L.t("skip_to.skipped_track"));
        handler.getPlayer().stopTrack();
    }

    @Override
    public void doCommand(SlashCommandEvent event) {
        if (!checkDJPermission(event.getClient(), event)) {
            event.reply(event.getClient().getWarning() + L.t("autoplaylist.don_t_have_permission_do")).queue();
            return;
        }
        int index = 0;
        try {
            index = Integer.parseInt(event.getOption("position").getAsString());
        } catch (NumberFormatException e) {
            event.reply(event.getClient().getError() + " `" + event.getOption("position").getAsString() + L.t("skip_to.not_valid_whole_number")).queue();
            return;
        }
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        if (index < 1 || index > handler.getQueue().size()) {
            event.reply(event.getClient().getError() + L.t("skip_to.must_enter_whole_number_between") + handler.getQueue().size() + L.t("skip_to.text")).queue();
            return;
        }
        handler.getQueue().skip(index - 1);
        event.reply(event.getClient().getSuccess() + " **" + handler.getQueue().get(0).getTrack().getInfo().title + L.t("skip_to.skipped_track")).queue();
        handler.getPlayer().stopTrack();
    }
}
