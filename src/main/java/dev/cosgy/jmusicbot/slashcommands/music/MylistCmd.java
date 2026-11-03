package dev.cosgy.jmusicbot.slashcommands.music;

import dev.cosgy.jmusicbot.i18n.L;

import dev.cosgy.jmusicbot.framework.jdautilities.command.Command;
import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import dev.cosgy.jmusicbot.playlist.MylistLoader;
import dev.cosgy.jmusicbot.slashcommands.DJCommand;
import dev.cosgy.jmusicbot.slashcommands.MusicCommand;
import dev.cosgy.jmusicbot.util.StackTraceUtil;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author kosugikun
 */
public class MylistCmd extends MusicCommand {

    public MylistCmd(Bot bot) {
        super(bot);
        //this.guildOnly = false;
        this.name = "mylist";
        this.arguments = L.t("mylist.append_delete_make_all_show");
        this.help = L.t("mylist.manages_personal_playlists");
        this.aliases = bot.getConfig().getAliases(this.name);
        this.children = new MusicCommand[]{
                new PlayCmd(bot),
                new MakelistCmd(bot),
                new DeletelistCmd(bot),
                new AppendlistCmd(bot),
                new ListCmd(bot),
                new ShowTracksCmd(bot)
        };
    }

    @Override
    public void doCommand(CommandEvent event) {

        StringBuilder builder = new StringBuilder(event.getClient().getWarning() + L.t("mylist.personal_list_commands"));
        for (Command cmd : this.children)
            builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" ").append(cmd.getName())
                    .append(" ").append(cmd.getArguments() == null ? "" : cmd.getArguments()).append("` - ").append(cmd.getHelp());
        event.reply(builder.toString());
    }

    @Override
    public void doCommand(SlashCommandEvent slashCommandEvent) {
    }

    public static class ShowTracksCmd extends MusicCommand {
        public ShowTracksCmd(Bot bot) {
            super(bot);
            this.name = "show";
            this.help = L.t("mylist.shows_tracks_specified_personal_list");
            this.arguments = L.t("play.name");
            //this.guildOnly = false;
            this.ownerCommand = false;

            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", L.t("play.playlist_name"), true));
            this.options = options;
        }

        @Override
        public void doCommand(CommandEvent event) {
            String userId = event.getAuthor().getId();
            String playlistName = event.getArgs().trim();

            if (playlistName.isEmpty()) {
                event.reply(event.getClient().getError() + L.t("mylist.specify_list_name"));
                return;
            }

            MylistLoader.Playlist playlist = bot.getMylistLoader().getPlaylist(userId, playlistName);
            if (playlist == null) {
                event.reply(event.getClient().getError() + L.t("mylist.personal_list") + playlistName + L.t("mylist.not_found"));
                return;
            }

            if (playlist.getTracks().isEmpty()) {
                event.reply(event.getClient().getWarning() + L.t("mylist.personal_list") + playlistName + L.t("mylist.has_no_tracks"));
                return;
            }

            StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + L.t("mylist.personal_list") + playlistName + L.t("mylist.tracks"));
            for (int i = 0; i < playlist.getTracks().size(); i++) {
                builder.append(i + 1).append(". ").append(playlist.getTracks().get(i).getInfo().title).append("\n");
            }

            if (builder.length() > 2000) {
                builder.setLength(1997); // To stay within Discord's message limit
                builder.append("...");
            }

            event.reply(builder.toString());
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            String userId = event.getUser().getId();
            String playlistName = event.getOption("name").getAsString();

            MylistLoader.Playlist playlist = bot.getMylistLoader().getPlaylist(userId, playlistName);
            if (playlist == null) {
                event.reply(event.getClient().getError() + L.t("mylist.personal_list") + playlistName + L.t("mylist.not_found")).queue();
                return;
            }

            if (playlist.getTracks().isEmpty()) {
                event.reply(event.getClient().getWarning() + L.t("mylist.personal_list") + playlistName + L.t("mylist.has_no_tracks")).queue();
                return;
            }

            StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + L.t("mylist.personal_list") + playlistName + L.t("mylist.tracks"));
            for (int i = 0; i < playlist.getTracks().size(); i++) {
                builder.append(i + 1).append(". ").append(playlist.getTracks().get(i).getInfo().title).append("\n");
            }

            if (builder.length() > 2000) {
                builder.setLength(1997); // To stay within Discord's message limit
                builder.append("...");
            }

            event.reply(builder.toString()).queue();
        }
    }


    public class PlayCmd extends MusicCommand {
        public PlayCmd(Bot bot) {
            super(bot);
            this.name = "play";
            this.aliases = new String[]{"pl"};
            this.arguments = L.t("play.name");
            this.help = L.t("play.plays_personal_list");
            this.beListening = true;
            this.bePlaying = false;

            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", L.t("play.personal_list_name"), true));
            this.options = options;
        }

        @Override
        public void doCommand(CommandEvent event) {
            String userId = event.getAuthor().getId();
            if (event.getArgs().isEmpty()) {
                event.reply(event.getClient().getError() + L.t("play.enter_personal_list_name"));
                return;
            }
            MylistLoader.Playlist playlist = bot.getMylistLoader().getPlaylist(userId, event.getArgs());
            if (playlist == null) {
                event.replyError("`" + event.getArgs() + L.t("play.txt_not_found_2"));
                return;
            }
            event.getChannel().sendMessage(L.t("play.calling_loading_personal_list") + event.getArgs() + L.t("play.text_6") + playlist.getItems().size() + L.t("play.tracks")).queue(m ->
            {
                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                playlist.loadTracks(bot.getPlayerManager(), (at) -> handler.addTrack(new QueuedTrack(at, event.getAuthor())), () -> {
                    StringBuilder builder = new StringBuilder(playlist.getTracks().isEmpty()
                            ? event.getClient().getWarning() + L.t("play.no_tracks_could_loaded")
                            : event.getClient().getSuccess() + "**" + playlist.getTracks().size() + L.t("play.tracks_loaded"));
                    if (!playlist.getErrors().isEmpty())
                        builder.append(L.t("play.these_tracks_could_not_loaded"));
                    playlist.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex() + 1)
                            .append("]` **").append(err.getItem()).append("**: ").append(err.getReason()));
                    String str = builder.toString();
                    if (str.length() > 2000)
                        str = str.substring(0, 1994) + L.t("play.truncated");
                    m.editMessage(FormatUtil.filter(str)).queue();
                });
            });
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            String userId = event.getUser().getId();

            String name = event.getOption("name").getAsString();

            MylistLoader.Playlist playlist = bot.getMylistLoader().getPlaylist(userId, name);
            if (playlist == null) {
                event.reply(event.getClient().getError() + "`" + name + L.t("play.txt_not_found_2")).queue();
                return;
            }
            event.reply(L.t("play.calling_loading_personal_list") + name + L.t("play.text_6") + playlist.getItems().size() + L.t("play.tracks")).queue(m ->
            {
                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                playlist.loadTracks(bot.getPlayerManager(), (at) -> handler.addTrack(new QueuedTrack(at, event.getUser())), () -> {
                    StringBuilder builder = new StringBuilder(playlist.getTracks().isEmpty()
                            ? event.getClient().getWarning() + L.t("play.no_tracks_could_loaded")
                            : event.getClient().getSuccess() + "**" + playlist.getTracks().size() + L.t("play.tracks_loaded"));
                    if (!playlist.getErrors().isEmpty())
                        builder.append(L.t("play.these_tracks_could_not_loaded"));
                    playlist.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex() + 1)
                            .append("]` **").append(err.getItem()).append("**: ").append(err.getReason()));
                    String str = builder.toString();
                    if (str.length() > 2000)
                        str = str.substring(0, 1994) + L.t("play.truncated");
                    m.editOriginal(FormatUtil.filter(str)).queue();
                });
            });
        }
    }


    public static class MakelistCmd extends DJCommand {
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
            String userId = event.getAuthor().getId();

            if (pName.isEmpty()) {
                event.replyError(L.t("mylist.specify_list_name_2"));
                return;
            }

            if (bot.getMylistLoader().getPlaylist(userId, pName) == null) {
                try {
                    bot.getMylistLoader().createPlaylist(userId, pName);
                    event.reply(event.getClient().getSuccess() + L.t("mylist.personal_list_2") + pName + L.t("mylist.created"));
                } catch (IOException e) {
                    if (event.isOwner() || event.getMember().isOwner()) {
                        event.replyError(L.t("play.error_occurred_while_loading_track_2") +
                                L.t("play.error_details") + e.getLocalizedMessage() + "**");
                        StackTraceUtil.sendStackTrace(event.getChannel(), e);
                        return;
                    }

                    event.reply(event.getClient().getError() + L.t("mylist.could_not_create_personal_list") + e.getLocalizedMessage());
                }
            } else {
                event.reply(event.getClient().getError() + L.t("mylist.personal_list") + pName + L.t("mylist.already_exists"));
            }
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            String pName = event.getOption("name").getAsString().replaceAll("\\s+", "_");
            String userId = event.getUser().getId();

            if (pName.isEmpty()) {
                event.reply(event.getClient().getError() + L.t("mylist.specify_list_name_2")).queue();
                return;
            }

            if (bot.getMylistLoader().getPlaylist(userId, pName) == null) {
                try {
                    bot.getMylistLoader().createPlaylist(userId, pName);
                    event.reply(event.getClient().getSuccess() + L.t("mylist.personal_list_2") + pName + L.t("mylist.created")).queue();
                } catch (IOException e) {
                    if (event.getClient().getOwnerId() == event.getMember().getId() || event.getMember().isOwner()) {
                        event.reply(event.getClient().getError() + L.t("play.error_occurred_while_loading_track_2") +
                                L.t("play.error_details") + e.getLocalizedMessage() + "**").queue();
                        StackTraceUtil.sendStackTrace(event.getChannel(), e);
                        return;
                    }

                    event.reply(event.getClient().getError() + L.t("mylist.could_not_create_personal_list") + e.getLocalizedMessage()).queue();
                }
            } else {
                event.reply(event.getClient().getError() + L.t("mylist.personal_list") + pName + L.t("mylist.already_exists")).queue();
            }
        }
    }

    public static class DeletelistCmd extends MusicCommand {
        public DeletelistCmd(Bot bot) {
            super(bot);
            this.name = "delete";
            this.aliases = new String[]{"remove"};
            this.help = L.t("mylist.deletes_existing_personal_list");
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
            String userId = event.getAuthor().getId();
            if (!pName.equals("")) {
                if (bot.getMylistLoader().getPlaylist(userId, pName) == null)
                    event.reply(event.getClient().getError() + L.t("mylist.no_such_personal_list") + pName + "`");
                else {
                    try {
                        bot.getMylistLoader().deletePlaylist(userId, pName);
                        event.reply(event.getClient().getSuccess() + L.t("mylist.personal_list_deleted") + pName + "`");
                    } catch (IOException e) {
                        event.reply(event.getClient().getError() + L.t("mylist.could_not_delete_personal_list") + e.getLocalizedMessage());
                    }
                }
            } else {
                event.reply(event.getClient().getError() + L.t("mylist.enter_personal_list_name"));
            }
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            String pName = event.getOption("name").getAsString().replaceAll("\\s+", "_");
            String userId = event.getUser().getId();

            if (bot.getMylistLoader().getPlaylist(userId, pName) == null)
                event.reply(event.getClient().getError() + L.t("mylist.no_such_personal_list") + pName + "`").queue();
            else {
                try {
                    bot.getMylistLoader().deletePlaylist(userId, pName);
                    event.reply(event.getClient().getSuccess() + L.t("mylist.personal_list_deleted") + pName + "`").queue();
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + L.t("mylist.could_not_delete_personal_list") + e.getLocalizedMessage()).queue();
                }
            }
        }
    }

    public static class AppendlistCmd extends MusicCommand {
        public AppendlistCmd(Bot bot) {
            super(bot);
            this.name = "append";
            this.aliases = new String[]{"add"};
            this.help = L.t("mylist.adds_tracks_existing_personal_list");
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
            String userId = event.getAuthor().getId();
            if (parts.length < 2) {
                event.reply(event.getClient().getError() + L.t("mylist.enter_personal_list_name_url"));
                return;
            }
            String pName = parts[0];
            MylistLoader.Playlist playlist = bot.getMylistLoader().getPlaylist(userId, pName);
            if (playlist == null)
                event.reply(event.getClient().getError() + L.t("mylist.no_such_personal_list") + pName + "`");
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
                    bot.getMylistLoader().writePlaylist(userId, pName, builder.toString());
                    event.reply(event.getClient().getSuccess() + urls.length + L.t("mylist.item_s_added_personal_list") + pName + "`");
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + L.t("mylist.could_not_add_personal_list") + e.getLocalizedMessage());
                }
            }
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            String userId = event.getUser().getId();
            String pname = event.getOption("name").getAsString();
            MylistLoader.Playlist playlist = bot.getMylistLoader().getPlaylist(userId, pname);
            if (playlist == null)
                event.reply(event.getClient().getError() + L.t("mylist.no_such_personal_list") + pname + "`").queue();
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
                    bot.getMylistLoader().writePlaylist(userId, pname, builder.toString());
                    event.reply(event.getClient().getSuccess() + urls.length + L.t("mylist.item_s_added_personal_list") + pname + "`").queue();
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + L.t("mylist.could_not_add_personal_list") + e.getLocalizedMessage()).queue();
                }
            }
        }
    }

    public static class ListCmd extends MusicCommand {
        public ListCmd(Bot bot) {
            super(bot);
            this.name = "all";
            this.aliases = new String[]{"available", "list"};
            this.help = L.t("mylist.shows_all_available_personal_lists");
            //this.guildOnly = true;
            this.ownerCommand = false;
        }

        @Override
        public void doCommand(CommandEvent event) {
            String userId = event.getAuthor().getId();

            if (!bot.getMylistLoader().folderUserExists(userId))
                bot.getMylistLoader().createUserFolder(userId);
            if (!bot.getMylistLoader().folderUserExists(userId)) {
                event.reply(event.getClient().getWarning() + L.t("mylist.personal_list_folder_did_not"));
                return;
            }
            List<String> list = bot.getMylistLoader().getPlaylistNames(userId);
            if (list == null)
                event.reply(event.getClient().getError() + L.t("mylist.could_not_read_available_personal"));
            else if (list.isEmpty())
                event.reply(event.getClient().getWarning() + L.t("mylist.there_are_no_lists_personal"));
            else {
                StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + L.t("mylist.available_personal_lists"));
                list.forEach(str -> builder.append("`").append(str).append("` "));
                event.reply(builder.toString());
            }
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            String userId = event.getUser().getId();

            if (!bot.getMylistLoader().folderUserExists(userId))
                bot.getMylistLoader().createUserFolder(userId);
            if (!bot.getMylistLoader().folderUserExists(userId)) {
                event.reply(event.getClient().getWarning() + L.t("mylist.personal_list_folder_did_not")).queue();
                return;
            }
            List<String> list = bot.getMylistLoader().getPlaylistNames(userId);
            if (list == null)
                event.reply(event.getClient().getError() + L.t("mylist.could_not_read_available_personal")).queue();
            else if (list.isEmpty())
                event.reply(event.getClient().getWarning() + L.t("mylist.there_are_no_lists_personal")).queue();
            else {
                StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + L.t("mylist.available_personal_lists"));
                list.forEach(str -> builder.append("`").append(str).append("` "));
                event.reply(builder.toString()).queue();
            }
        }
    }
}
