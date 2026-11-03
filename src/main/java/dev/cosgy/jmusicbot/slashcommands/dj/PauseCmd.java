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
package dev.cosgy.jmusicbot.slashcommands.dj;

import dev.cosgy.jmusicbot.i18n.L;

import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.PlayStatus;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import dev.cosgy.jmusicbot.slashcommands.DJCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class PauseCmd extends DJCommand {
    Logger log = LoggerFactory.getLogger("Pause");

    public PauseCmd(Bot bot) {
        super(bot);
        this.name = "pause";
        this.help = L.t("pause.pauses_current_track");
        this.aliases = bot.getConfig().getAliases(this.name);
        this.bePlaying = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        if (handler.getPlayer().isPaused()) {
            event.replyWarning(L.t("pause.track_already_paused_resume_use") + event.getClient().getPrefix() + L.t("pause.play"));
            return;
        }
        handler.getPlayer().setPaused(true);
        log.info(event.getGuild().getName() + ": " + handler.getPlayer().getPlayingTrack().getInfo().title + L.t("pause.paused"));
        event.replySuccess("**" + handler.getPlayer().getPlayingTrack().getInfo().title + L.t("pause.paused_resume_use") + event.getClient().getPrefix() + L.t("pause.play_2"));
        bot.getNowplayingHandler().onTrackUpdate(event.getGuild().getIdLong(), handler.getPlayer().getPlayingTrack(), handler);

        Bot.updatePlayStatus(event.getGuild(), event.getGuild().getSelfMember(), PlayStatus.PAUSED);
    }

    @Override
    public void doCommand(SlashCommandEvent event) {
        if (!checkDJPermission(event.getClient(), event)) {
            event.reply(event.getClient().getWarning() + L.t("autoplaylist.don_t_have_permission_do")).queue();
            return;
        }
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        if (handler.getPlayer().isPaused()) {
            event.reply(event.getClient().getWarning() + L.t("pause.track_already_paused_resume_use") + event.getClient().getPrefix() + L.t("pause.play")).queue();
            return;
        }
        handler.getPlayer().setPaused(true);
        log.info(event.getGuild().getName() + ": " + handler.getPlayer().getPlayingTrack().getInfo().title + L.t("pause.paused"));
        event.reply(event.getClient().getSuccess() + "**" + handler.getPlayer().getPlayingTrack().getInfo().title + L.t("pause.paused_resume_use") + event.getClient().getPrefix() + L.t("pause.play_2")).queue();
        bot.getNowplayingHandler().onTrackUpdate(event.getGuild().getIdLong(), handler.getPlayer().getPlayingTrack(), handler);

        Bot.updatePlayStatus(event.getGuild(), event.getGuild().getSelfMember(), PlayStatus.PAUSED);
    }
}
