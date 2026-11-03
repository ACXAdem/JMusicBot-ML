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
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class SetgameCmd extends OwnerCommand {
    public SetgameCmd(Bot bot) {
        this.name = "setgame";
        this.help = L.t("setgame.sets_game_bot_playing");
        this.arguments = L.t("setgame.action_game");
        this.aliases = bot.getConfig().getAliases(this.name);
        //this.guildOnly = false;
        this.children = new OwnerCommand[]{
                new PlayingCmd(),
                new SetlistenCmd(),
                new SetstreamCmd(),
                new SetwatchCmd(),
                new SetCompetingCmd(),
                new NoneCmd()
        };
    }

    @Override
    protected void execute(SlashCommandEvent slashCommandEvent) {
    }

    @Override
    protected void execute(CommandEvent event) {
        String title = event.getArgs().toLowerCase().startsWith("playing") ? event.getArgs().substring(7).trim() : event.getArgs();
        try {
            event.getJDA().getPresence().setActivity(title.isEmpty() ? null : Activity.playing(title));
            event.reply(event.getClient().getSuccess() + " **" + event.getSelfUser().getName()
                    + L.t("setgame.now") + (title.isEmpty() ? L.t("setgame.not_playing_anything") : L.t("setgame.playing") + title + L.t("setgame.text")));
        } catch (Exception e) {
            event.reply(event.getClient().getError() + L.t("setstatus.could_not_set_status"));
        }
    }

    private class NoneCmd extends OwnerCommand {
        private NoneCmd() {
            this.name = "none";
            this.aliases = new String[]{"none"};
            this.help = L.t("setgame.resets_status");
            //this.guildOnly = false;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            event.getJDA().getPresence().setActivity(null);
            event.reply(L.t("setgame.status_reset")).queue();
        }

        @Override
        protected void execute(CommandEvent event) {
            event.getJDA().getPresence().setActivity(null);
            event.reply(L.t("setgame.status_reset"));
        }
    }

    private class PlayingCmd extends OwnerCommand {
        private PlayingCmd() {
            this.name = "playing";
            this.aliases = new String[]{"twitch", "streaming"};
            this.help = L.t("setgame.sets_game_bot_playing_2");
            this.arguments = L.t("setgame.title");
            //this.guildOnly = false;
            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "title", L.t("setgame.name_game"), true));
            this.options = options;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            String title = event.getOption("title").getAsString();
            try {
                event.getJDA().getPresence().setActivity(Activity.playing(title));
                event.reply(event.getClient().getSuccess() + " **" + event.getJDA().getSelfUser().getName()
                        + L.t("setgame.now") + L.t("setgame.playing") + title + L.t("setgame.text"));
            } catch (Exception e) {
                event.reply(event.getClient().getError() + L.t("setstatus.could_not_set_status")).queue();
            }
        }

        @Override
        protected void execute(CommandEvent event) {
        }
    }

    private class SetstreamCmd extends OwnerCommand {
        private SetstreamCmd() {
            this.name = "stream";
            this.aliases = new String[]{"twitch", "streaming"};
            this.help = L.t("setgame.sets_bot_s_status_streaming");
            this.arguments = L.t("setgame.username_game");
            //this.guildOnly = false;
            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "user", L.t("setgame.username"), true));
            options.add(new OptionData(OptionType.STRING, "game", L.t("setgame.name_game"), true));
            this.options = options;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            try {
                event.getJDA().getPresence().setActivity(Activity.streaming(event.getOption("game").getAsString(), "https://twitch.tv/" + event.getOption("user").getAsString()));
                event.reply(event.getClient().getSuccess() + "**" + event.getJDA().getSelfUser().getName()
                        + L.t("setgame.now_2") + event.getOption("game").getAsString() + L.t("setgame.streaming")).queue();
            } catch (Exception e) {
                event.reply(event.getClient().getError() + L.t("setgame.could_not_set_game")).queue();
            }
        }

        @Override
        protected void execute(CommandEvent event) {
            String[] parts = event.getArgs().split("\\s+", 2);
            if (parts.length < 2) {
                event.replyError(L.t("setgame.enter_username_name_game_stream"));
                return;
            }
            try {
                event.getJDA().getPresence().setActivity(Activity.streaming(parts[1], "https://twitch.tv/" + parts[0]));
                event.replySuccess("**" + event.getSelfUser().getName()
                        + L.t("setgame.now_2") + parts[1] + L.t("setgame.streaming"));
            } catch (Exception e) {
                event.reply(event.getClient().getError() + L.t("setgame.could_not_set_game"));
            }
        }
    }

    private class SetlistenCmd extends OwnerCommand {
        private SetlistenCmd() {
            this.name = "listen";
            this.aliases = new String[]{"listening"};
            this.help = L.t("setgame.sets_bot_s_listening_status");
            this.arguments = L.t("setgame.title");
            //this.guildOnly = false;
            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "title", L.t("setgame.title_2"), true));
            this.options = options;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            String title = event.getOption("title").getAsString();
            try {
                event.getJDA().getPresence().setActivity(Activity.listening(title));
                event.reply(event.getClient().getSuccess() + "**" + event.getJDA().getSelfUser().getName() + L.t("setgame.now_2") + title + L.t("setgame.listening")).queue();
            } catch (Exception e) {
                event.reply(event.getClient().getError() + L.t("setgame.could_not_set_game")).queue();
            }
        }

        @Override
        protected void execute(CommandEvent event) {
            if (event.getArgs().isEmpty()) {
                event.replyError(L.t("setgame.enter_what_listening"));
                return;
            }
            String title = event.getArgs().toLowerCase().startsWith("to") ? event.getArgs().substring(2).trim() : event.getArgs();
            try {
                event.getJDA().getPresence().setActivity(Activity.listening(title));
                event.replySuccess("**" + event.getSelfUser().getName() + L.t("setgame.now_2") + title + L.t("setgame.listening"));
            } catch (Exception e) {
                event.reply(event.getClient().getError() + L.t("setgame.could_not_set_game"));
            }
        }
    }

    private class SetwatchCmd extends OwnerCommand {
        private SetwatchCmd() {
            this.name = "watch";
            this.aliases = new String[]{"watching"};
            this.help = L.t("setgame.sets_bot_s_watching_status");
            this.arguments = L.t("setgame.title");
            //this.guildOnly = false;
            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "title", L.t("setgame.title_2"), true));
            this.options = options;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            String title = event.getOption("title").getAsString();
            try {
                event.getJDA().getPresence().setActivity(Activity.watching(title));
                event.reply(event.getClient().getSuccess() + "**" + event.getJDA().getSelfUser().getName() + L.t("setgame.now_2") + title + L.t("setgame.watching")).queue();
            } catch (Exception e) {
                event.reply(event.getClient().getError() + L.t("setgame.could_not_set_game")).queue();
            }
        }

        @Override
        protected void execute(CommandEvent event) {
            if (event.getArgs().isEmpty()) {
                event.replyError(L.t("setgame.enter_what_watching"));
                return;
            }
            String title = event.getArgs();
            try {
                event.getJDA().getPresence().setActivity(Activity.watching(title));
                event.replySuccess("**" + event.getSelfUser().getName() + L.t("setgame.now_2") + title + L.t("setgame.watching"));
            } catch (Exception e) {
                event.reply(event.getClient().getError() + L.t("setgame.could_not_set_game"));
            }
        }
    }

    private class SetCompetingCmd extends OwnerCommand {
        private SetCompetingCmd() {
            this.name = "competing";
            this.help = L.t("setgame.sets_bot_s_competing_status");
            this.arguments = L.t("setgame.title");
            //this.guildOnly = false;
            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "title", L.t("setgame.game_name"), true));
            this.options = options;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            String title = event.getOption("title").getAsString();
            try {
                event.getJDA().getPresence().setActivity(Activity.competing(title));
                event.reply(event.getClient().getSuccess() + "**" + event.getJDA().getSelfUser().getName() + L.t("setgame.now_2") + title + L.t("setgame.competing")).queue();
            } catch (Exception e) {
                event.reply(event.getClient().getError() + L.t("setgame.could_not_set_game")).queue();
            }
        }

        @Override
        protected void execute(CommandEvent event) {
            if (event.getArgs().isEmpty()) {
                event.replyError(L.t("setgame.enter_what_competing"));
                return;
            }
            String title = event.getArgs();
            try {
                event.getJDA().getPresence().setActivity(Activity.watching(title));
                event.replySuccess("**" + event.getSelfUser().getName() + L.t("setgame.now_2") + title + L.t("setgame.competing"));
            } catch (Exception e) {
                event.reply(event.getClient().getError() + L.t("setgame.could_not_set_game"));
            }
        }
    }
}
