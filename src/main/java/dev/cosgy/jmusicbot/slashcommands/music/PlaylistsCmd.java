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
package dev.cosgy.jmusicbot.slashcommands.music;

import dev.cosgy.jmusicbot.i18n.L;

import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.playlist.PlaylistLoader;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import dev.cosgy.jmusicbot.slashcommands.MusicCommand;
import dev.cosgy.jmusicbot.util.StackTraceUtil;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static dev.cosgy.jmusicbot.slashcommands.DJCommand.checkDJPermission;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class PlaylistsCmd extends MusicCommand {

    public PlaylistsCmd(Bot bot) {
        super(bot);
        this.name = "playlists";
        this.help = L.t("playlists.shows_available_playlists");
        this.arguments = L.t("playlists.play_append_delete_make_show");
        this.aliases = bot.getConfig().getAliases(this.name);
        //this.guildOnly = true;
        this.beListening = false;
        this.children = new MusicCommand[]{
                new PlayCmd(bot),
                new ListCmd(bot),
                new AppendlistCmd(bot),
                new DeletelistCmd(bot),
                new MakelistCmd(bot),
                new ShowTracksCmd(bot)};
    }

    @Override
    public void doCommand(CommandEvent event) {
        handlePlaylistsCommand(event.getGuild().getId(), event, null);
    }

    @Override
    public void doCommand(SlashCommandEvent event) {
        handlePlaylistsCommand(event.getGuild().getId(), null, event);
    }

    private void handlePlaylistsCommand(String guildID, CommandEvent cmdEvent, SlashCommandEvent slashEvent) {
        ensureFoldersExist(guildID);

        List<String> playlists = bot.getPlaylistLoader().getPlaylistNames(guildID);

        String prefix = (cmdEvent != null ? cmdEvent.getClient().getTextualPrefix() : slashEvent.getClient().getTextualPrefix());
        String message;
        if (playlists == null) {
            message = (cmdEvent != null ? cmdEvent.getClient().getError() : slashEvent.getClient().getError()) + L.t("playlists.could_not_read_available_playlists");
        } else if (playlists.isEmpty()) {
            message = (cmdEvent != null ? cmdEvent.getClient().getWarning() : slashEvent.getClient().getWarning()) + L.t("playlists.there_are_no_lists_playlist");
        } else {
            StringBuilder builder = new StringBuilder((cmdEvent != null ? cmdEvent.getClient().getSuccess() : slashEvent.getClient().getSuccess()) + L.t("playlists.available_playlists"));
            playlists.forEach(name -> builder.append("`").append(name).append("` "));
            builder.append("\n`").append(prefix).append(L.t("playlists.playlists_play_name_play_playlist"));
            message = builder.toString();
        }

        reply(cmdEvent, slashEvent, message);
    }

    private void ensureFoldersExist(String guildID) {
        if (!bot.getPlaylistLoader().folderExists()) {
            bot.getPlaylistLoader().createFolder();
        }
        if (!bot.getPlaylistLoader().folderGuildExists(guildID)) {
            bot.getPlaylistLoader().createGuildFolder(guildID);
        }
    }

    private void reply(CommandEvent cmdEvent, SlashCommandEvent slashEvent, String message) {
        if (cmdEvent != null) {
            cmdEvent.reply(message); // CommandEvent'e queue() eklenmez
        } else if (slashEvent != null) {
            slashEvent.reply(message).queue(); // SlashCommandEvent'e queue() eklenir
        }
    }

    public class PlayCmd extends MusicCommand {
        public PlayCmd(Bot bot) {
            super(bot);
            this.name = "play";
            this.aliases = new String[]{"play"};
            this.arguments = L.t("play.name");
            this.help = L.t("play.plays_specified_playlist");
            this.beListening = true;
            this.bePlaying = false;

            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", L.t("play.personal_list_name"), true));
            this.options = options;
        }
        @Override
        public void doCommand(CommandEvent event) {
            playPlaylist(event.getGuild().getId(), event.getArgs(), event, null);
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            String playlistName = event.getOption("name").getAsString();
            playPlaylist(event.getGuild().getId(), playlistName, null, event);
        }

        private void playPlaylist(String guildID, String playlistName, CommandEvent cmdEvent, SlashCommandEvent slashEvent) {
            if (playlistName == null || playlistName.isEmpty()) {
                reply(cmdEvent, slashEvent, (cmdEvent != null ? cmdEvent.getClient().getError() : slashEvent.getClient().getError()) + L.t("playlists.specify_playlist_name"));
                return;
            }

            PlaylistLoader.Playlist playlist = bot.getPlaylistLoader().getPlaylist(guildID, playlistName);
            if (playlist == null) {
                reply(cmdEvent, slashEvent, (cmdEvent != null ? cmdEvent.getClient().getError() : slashEvent.getClient().getError()) + L.t("playlists.playlist") + playlistName + L.t("mylist.not_found"));
                return;
            }

            reply(cmdEvent, slashEvent, (cmdEvent != null ? cmdEvent.getClient().getSuccess() : slashEvent.getClient().getSuccess()) + L.t("play.calling_loading_playlist") + playlistName + L.t("play.text_6") + playlist.getItems().size() + L.t("play.tracks"));

            AudioHandler handler = (AudioHandler) (cmdEvent != null
                    ? cmdEvent.getGuild().getAudioManager().getSendingHandler()
                    : slashEvent.getGuild().getAudioManager().getSendingHandler());

            playlist.loadTracks(bot.getPlayerManager(), track -> handler.addTrack(new QueuedTrack(track, cmdEvent != null ? cmdEvent.getAuthor() : slashEvent.getUser())), () -> {
                StringBuilder builder = new StringBuilder();
                if (playlist.getTracks().isEmpty()) {
                    builder.append((cmdEvent != null ? cmdEvent.getClient().getWarning() : slashEvent.getClient().getWarning())).append(L.t("play.no_tracks_could_loaded"));
                } else {
                    builder.append((cmdEvent != null ? cmdEvent.getClient().getSuccess() : slashEvent.getClient().getSuccess())).append(" **").append(playlist.getTracks().size()).append(L.t("play.tracks_loaded"));
                }
                if (!playlist.getErrors().isEmpty()) {
                    builder.append(L.t("play.these_tracks_could_not_loaded"));
                    playlist.getErrors().forEach(error -> builder.append("\n`[").append(error.getIndex() + 1).append("]` **").append(error.getItem()).append("**: ").append(error.getReason()));
                }

                String result = FormatUtil.filter(builder.toString());
                if (result.length() > 2000) {
                    result = result.substring(0, 1994) + L.t("play.truncated");
                }

                reply(cmdEvent, slashEvent, result);
            });
        }
    }

    public class ShowTracksCmd extends MusicCommand {
        public ShowTracksCmd(Bot bot) {
            super(bot);
            this.name = "show";
            this.help = L.t("playlists.shows_tracks_specified_playlist");
            this.arguments = L.t("play.name");
            //this.guildOnly = true;

            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", L.t("play.playlist_name"), true));
            this.options = options;
        }

        @Override
        public void doCommand(CommandEvent event) {
            String guildId = event.getGuild().getId();
            String playlistName = event.getArgs().trim();

            if (playlistName.isEmpty()) {
                event.reply(event.getClient().getError() + L.t("mylist.specify_list_name"));
                return;
            }

            PlaylistLoader.Playlist playlist = bot.getPlaylistLoader().getPlaylist(guildId, playlistName);
            if (playlist == null) {
                event.reply(event.getClient().getError() + L.t("playlists.playlist") + playlistName + L.t("mylist.not_found"));
                return;
            }

            if (playlist.getItems().isEmpty()) {
                event.reply(event.getClient().getWarning() + L.t("playlists.playlist") + playlistName + L.t("mylist.has_no_tracks"));
                return;
            }

            StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + L.t("playlists.playlist") + playlistName + L.t("mylist.tracks"));
            for (int i = 0; i < playlist.getItems().size(); i++) {
                builder.append(i + 1).append(". ").append(playlist.getItems().get(i)).append("\n");
            }

            if (builder.length() > 2000) {
                builder.setLength(1997); // To stay within Discord's message limit
                builder.append("...");
            }

            event.reply(builder.toString());
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            String guildId = event.getGuild().getId();
            String playlistName = event.getOption("name").getAsString();

            PlaylistLoader.Playlist playlist = bot.getPlaylistLoader().getPlaylist(guildId, playlistName);
            if (playlist == null) {
                event.reply(event.getClient().getError() + L.t("playlists.playlist") + playlistName + L.t("mylist.not_found")).queue();
                return;
            }

            if (playlist.getItems().isEmpty()) {
                event.reply(event.getClient().getWarning() + L.t("playlists.playlist") + playlistName + L.t("mylist.has_no_tracks")).queue();
                return;
            }

            StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + L.t("playlists.playlist") + playlistName + L.t("mylist.tracks"));
            for (int i = 0; i < playlist.getItems().size(); i++) {
                builder.append(i + 1).append(". ").append(playlist.getItems().get(i)).append("\n");
            }

            if (builder.length() > 2000) {
                builder.setLength(1997); // To stay within Discord's message limit
                builder.append("...");
            }

            event.reply(builder.toString()).queue();
        }
    }

    public class MakelistCmd extends MusicCommand {
        public MakelistCmd(Bot bot) {
            super(bot);
            this.name = "make";
            this.aliases = new String[]{"create"};
            this.help = L.t("mylist.creates_new_list");
            this.arguments = L.t("play.name");
            //this.guildOnly = true;
            this.ownerCommand = false;

            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", L.t("play.playlist_name"), true));
            this.options = options;
        }

        @Override
        public void doCommand(CommandEvent event) {

            String pName = event.getArgs().replaceAll("\\s+", "_");
            String guildId = event.getGuild().getId();

            if (pName == null || pName.isEmpty()) {
                event.replyError(L.t("playlists.enter_playlist_name"));
            } else if (bot.getPlaylistLoader().getPlaylist(guildId, pName) == null) {
                try {
                    bot.getPlaylistLoader().createPlaylist(guildId, pName);
                    event.reply(event.getClient().getSuccess() + L.t("playlists.playlist_2") + pName + L.t("mylist.created"));
                } catch (IOException e) {
                    if (event.isOwner() || event.getMember().isOwner()) {
                        event.replyError(L.t("play.error_occurred_while_loading_track_2") +
                                L.t("play.error_details") + e.getLocalizedMessage() + "**");
                        StackTraceUtil.sendStackTrace(event.getChannel(), e);
                        return;
                    }

                    event.reply(event.getClient().getError() + L.t("playlists.could_not_create_playlist") + e.getLocalizedMessage());
                }
            } else {
                event.reply(event.getClient().getError() + L.t("playlists.playlist") + pName + L.t("mylist.already_exists"));
            }
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            if (!checkDJPermission(event.getClient(), event)) {
                event.reply(event.getClient().getWarning() + L.t("autoplaylist.don_t_have_permission_do")).queue();
                return;
            }
            String pname = event.getOption("name").getAsString();
            String guildId = event.getGuild().getId();
            if (pname == null || pname.isEmpty()) {
                event.reply(event.getClient().getError() + L.t("playlists.enter_playlist_name")).queue();
            } else if (bot.getPlaylistLoader().getPlaylist(guildId, pname) == null) {
                try {
                    bot.getPlaylistLoader().createPlaylist(guildId, pname);
                    event.reply(event.getClient().getSuccess() + L.t("playlists.playlist_2") + pname + L.t("mylist.created")).queue();
                } catch (IOException e) {
                    if (event.getClient().getOwnerId() == event.getMember().getId() || event.getMember().isOwner()) {
                        event.reply(event.getClient().getError() + L.t("play.error_occurred_while_loading_track_2") +
                                L.t("play.error_details") + e.getLocalizedMessage() + "**").queue();
                        StackTraceUtil.sendStackTrace(event.getChannel(), e);
                        return;
                    }

                    event.reply(event.getClient().getError() + L.t("playlists.could_not_create_playlist") + e.getLocalizedMessage()).queue();
                }
            } else {
                event.reply(event.getClient().getError() + L.t("playlists.playlist") + pname + L.t("mylist.already_exists")).queue();
            }
        }
    }

    public class DeletelistCmd extends MusicCommand {
        public DeletelistCmd(Bot bot) {
            super(bot);
            this.name = "delete";
            this.aliases = new String[]{"remove"};
            this.help = L.t("playlists.deletes_existing_playlist");
            this.arguments = L.t("play.name");
            //this.guildOnly = true;
            this.ownerCommand = false;
            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", L.t("play.playlist_name"), true));
            this.options = options;
        }

        @Override
        public void doCommand(CommandEvent event) {

            String pname = event.getArgs().replaceAll("\\s+", "_");
            String guildid = event.getGuild().getId();
            if (!pname.equals("")) {
                if (bot.getPlaylistLoader().getPlaylist(guildid, pname) == null)
                    event.reply(event.getClient().getError() + L.t("playlists.no_such_playlist") + pname + "`");
                else {
                    try {
                        bot.getPlaylistLoader().deletePlaylist(guildid, pname);
                        event.reply(event.getClient().getSuccess() + L.t("playlists.playlist_deleted") + pname + "`");
                    } catch (IOException e) {
                        event.reply(event.getClient().getError() + L.t("playlists.could_not_delete_playlist") + e.getLocalizedMessage());
                    }
                }
            } else {
                event.reply(event.getClient().getError() + L.t("playlists.enter_playlist_name_2"));
            }
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            if (!checkDJPermission(event.getClient(), event)) {
                event.reply(event.getClient().getWarning() + L.t("autoplaylist.don_t_have_permission_do")).queue();
                return;
            }
            String pname = event.getOption("name").getAsString();
            String guildid = event.getGuild().getId();
            if (bot.getPlaylistLoader().getPlaylist(guildid, pname) == null)
                event.reply(event.getClient().getError() + L.t("playlists.no_such_playlist") + pname + "`").queue();
            else {
                try {
                    bot.getPlaylistLoader().deletePlaylist(guildid, pname);
                    event.reply(event.getClient().getSuccess() + L.t("playlists.playlist_deleted") + pname + "`").queue();
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + L.t("playlists.could_not_delete_playlist") + e.getLocalizedMessage()).queue();
                }
            }
        }
    }

    public class AppendlistCmd extends MusicCommand {
        public AppendlistCmd(Bot bot) {
            super(bot);
            this.name = "append";
            this.aliases = new String[]{"add"};
            this.help = L.t("playlists.adds_tracks_existing_playlist");
            this.arguments = L.t("mylist.name_url_url");
            //this.guildOnly = true;
            this.ownerCommand = false;
            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", L.t("play.playlist_name"), true));
            options.add(new OptionData(OptionType.STRING, "url", "URL", true));
            this.options = options;
        }

        @Override
        public void doCommand(CommandEvent event) {

            String[] parts = event.getArgs().split("\\s+", 2);
            String guildid = event.getGuild().getId();
            if (parts.length < 2) {
                event.reply(event.getClient().getError() + L.t("playlists.enter_playlist_name_url_add"));
                return;
            }
            String pname = parts[0];
            PlaylistLoader.Playlist playlist = bot.getPlaylistLoader().getPlaylist(guildid, pname);
            if (playlist == null)
                event.reply(event.getClient().getError() + L.t("playlists.no_such_playlist") + pname + "`");
            else {
                StringBuilder builder = new StringBuilder();
                playlist.getItems().forEach(item -> builder.append("\r\n").append(item));
                String[] urls = parts[1].split("\\|");
                for (String url : urls) {
                    String u = url.trim();
                    if (u.startsWith("<") && u.endsWith(">"))
                        u = u.substring(1, u.length() - 1);
                    builder.append("\r\n").append(u);
                }
                try {
                    bot.getPlaylistLoader().writePlaylist(guildid, pname, builder.toString());
                    event.reply(event.getClient().getSuccess() + urls.length + L.t("playlists.item_s_added_playlist") + pname + "`");
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + L.t("playlists.could_not_add_playlist") + e.getLocalizedMessage());
                }
            }
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            if (!checkDJPermission(event.getClient(), event)) {
                event.reply(event.getClient().getWarning() + L.t("autoplaylist.don_t_have_permission_do")).queue();
                return;
            }

            String guildid = event.getGuild().getId();
            String pname = event.getOption("name").getAsString();
            PlaylistLoader.Playlist playlist = bot.getPlaylistLoader().getPlaylist(guildid, pname);
            if (playlist == null)
                event.reply(event.getClient().getError() + L.t("playlists.no_such_playlist") + pname + "`").queue();
            else {
                StringBuilder builder = new StringBuilder();
                playlist.getItems().forEach(item -> builder.append("\r\n").append(item));
                String[] urls = event.getOption("url").getAsString().split("\\|");
                for (String url : urls) {
                    String u = url.trim();
                    if (u.startsWith("<") && u.endsWith(">"))
                        u = u.substring(1, u.length() - 1);
                    builder.append("\r\n").append(u);
                }
                try {
                    bot.getPlaylistLoader().writePlaylist(guildid, pname, builder.toString());
                    event.reply(event.getClient().getSuccess() + urls.length + L.t("playlists.item_s_added_playlist") + pname + "`").queue();
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + L.t("playlists.could_not_add_playlist") + e.getLocalizedMessage()).queue();
                }
            }
        }
    }

    public class ListCmd extends MusicCommand {
        public ListCmd(Bot bot) {
            super(bot);
            this.name = "all";
            this.aliases = new String[]{"available", "list"};
            this.help = L.t("playlists.shows_all_available_playlists");
            //this.guildOnly = true;
            this.ownerCommand = false;
        }

        @Override
        public void doCommand(CommandEvent event) {
            String guildId = event.getGuild().getId();

            if (!bot.getPlaylistLoader().folderGuildExists(guildId))
                bot.getPlaylistLoader().createGuildFolder(guildId);
            if (!bot.getPlaylistLoader().folderGuildExists(guildId)) {
                event.reply(event.getClient().getWarning() + L.t("playlists.playlist_folder_did_not_exist"));
                return;
            }
            List<String> list = bot.getPlaylistLoader().getPlaylistNames(guildId);
            if (list == null)
                event.reply(event.getClient().getError() + L.t("playlists.could_not_read_available_playlists"));
            else if (list.isEmpty())
                event.reply(event.getClient().getWarning() + L.t("playlists.there_are_no_lists_playlist"));
            else {
                StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + L.t("playlists.available_playlists"));
                list.forEach(str -> builder.append("`").append(str).append("` "));
                event.reply(builder.toString());
            }
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            if (!checkDJPermission(event.getClient(), event)) {
                event.reply(event.getClient().getWarning() + L.t("autoplaylist.don_t_have_permission_do")).queue();
                return;
            }
            String guildId = event.getGuild().getId();
            if (!bot.getPlaylistLoader().folderGuildExists(guildId))
                bot.getPlaylistLoader().createGuildFolder(guildId);
            if (!bot.getPlaylistLoader().folderGuildExists(guildId)) {
                event.reply(event.getClient().getWarning() + L.t("playlists.playlist_folder_did_not_exist")).queue();
                return;
            }
            List<String> list = bot.getPlaylistLoader().getPlaylistNames(guildId);
            if (list == null)
                event.reply(event.getClient().getError() + L.t("playlists.could_not_read_available_playlists")).queue();
            else if (list.isEmpty())
                event.reply(event.getClient().getWarning() + L.t("playlists.there_are_no_lists_playlist")).queue();
            else {
                StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + L.t("playlists.available_playlists"));
                list.forEach(str -> builder.append("`").append(str).append("` "));
                event.reply(builder.toString()).queue();
            }
        }
    }
}