package dev.cosgy.jmusicbot.slashcommands.owner;

import dev.cosgy.jmusicbot.i18n.L;

import dev.cosgy.jmusicbot.framework.jdautilities.command.Command;
import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import dev.cosgy.jmusicbot.playlist.MylistLoader;
import dev.cosgy.jmusicbot.playlist.PubliclistLoader;
import dev.cosgy.jmusicbot.playlist.PubliclistLoader.Playlist;
import dev.cosgy.jmusicbot.slashcommands.OwnerCommand;
import dev.cosgy.jmusicbot.slashcommands.admin.AutoplaylistCmd;
import dev.cosgy.jmusicbot.slashcommands.music.MylistCmd;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author kosugikun
 */
public class PublistCmd extends OwnerCommand {
    private final Bot bot;

    public PublistCmd(Bot bot) {
        this.bot = bot;
        //this.guildOnly = false;
        this.name = "publist";
        this.arguments = L.t("mylist.append_delete_make_all_show");
        this.help = L.t("publist.manages_public_playlists");
        this.aliases = bot.getConfig().getAliases(this.name);
        this.children = new OwnerCommand[]{
                new ListCmd(),
                new AppendlistCmd(),
                new DeletelistCmd(),
                new MakelistCmd(),
                new ShowTracksCmd()
        };
    }

    @Override
    protected void execute(SlashCommandEvent slashCommandEvent) {

    }

    @Override
    public void execute(CommandEvent event) {
        StringBuilder builder = new StringBuilder(event.getClient().getWarning() + L.t("publist.playlist_management_commands"));
        for (Command cmd : this.children)
            builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" ").append(cmd.getName())
                    .append(" ").append(cmd.getArguments() == null ? "" : cmd.getArguments()).append("` - ").append(cmd.getHelp());
        event.reply(builder.toString());
    }

    public static class DefaultlistCmd extends AutoplaylistCmd {
        public DefaultlistCmd(Bot bot) {
            super(bot);
            this.name = "setdefault";
            this.aliases = new String[]{"default"};
            this.arguments = L.t("publist.list_name_none");
            //this.guildOnly = true;
        }
    }

    public class ShowTracksCmd extends OwnerCommand {
        public ShowTracksCmd() {
            this.name = "show";
            this.help = L.t("playlists.shows_tracks_specified_playlist");
            this.arguments = L.t("play.name");
            //this.guildOnly = false;

            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", L.t("play.playlist_name"), true));
            this.options = options;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            String playlistName = event.getOption("name").getAsString();

            PubliclistLoader.Playlist playlist = bot.getPublistLoader().getPlaylist(playlistName);
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

        @Override
        protected void execute(CommandEvent event) {
            String playlistName = event.getArgs().trim();

            if (playlistName.isEmpty()) {
                event.reply(event.getClient().getError() + L.t("mylist.specify_list_name"));
                return;
            }

            PubliclistLoader.Playlist playlist = bot.getPublistLoader().getPlaylist(playlistName);
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
    }

    public class MakelistCmd extends OwnerCommand {
        public MakelistCmd() {
            this.name = "make";
            this.aliases = new String[]{"create"};
            this.help = L.t("publist.creates_new_playlist");
            this.arguments = L.t("play.name");
            //this.guildOnly = false;

            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", L.t("play.playlist_name"), true));
            this.options = options;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            String pname = event.getOption("name").getAsString().replaceAll("\\s+", "_");
            if (bot.getPublistLoader().getPlaylist(pname) == null) {
                try {
                    bot.getPublistLoader().createPlaylist(pname);
                    event.reply(event.getClient().getSuccess() + " `" + pname + L.t("publist.playlist_created")).queue();
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + L.t("playlists.could_not_create_playlist") + e.getLocalizedMessage()).queue();
                }
            } else
                event.reply(event.getClient().getError() + L.t("playlists.playlist") + pname + L.t("publist.already_exists")).queue();
        }

        @Override
        protected void execute(CommandEvent event) {
            String pname = event.getArgs().replaceAll("\\s+", "_");
            if (bot.getPublistLoader().getPlaylist(pname) == null) {
                try {
                    bot.getPublistLoader().createPlaylist(pname);
                    event.reply(event.getClient().getSuccess() + " `" + pname + L.t("publist.playlist_created"));
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + L.t("playlists.could_not_create_playlist") + e.getLocalizedMessage());
                }
            } else
                event.reply(event.getClient().getError() + L.t("playlists.playlist") + pname + L.t("publist.already_exists"));
        }
    }

    public class DeletelistCmd extends OwnerCommand {
        public DeletelistCmd() {
            this.name = "delete";
            this.aliases = new String[]{"remove"};
            this.help = L.t("playlists.deletes_existing_playlist");
            this.arguments = L.t("play.name");
            //this.guildOnly = false;

            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", L.t("play.playlist_name"), true));
            this.options = options;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            String pname = event.getOption("name").getAsString().replaceAll("\\s+", "_");
            if (bot.getPublistLoader().getPlaylist(pname) == null)
                event.reply(event.getClient().getError() + L.t("playlists.playlist") + pname + L.t("publist.does_not_exist")).queue();
            else {
                try {
                    bot.getPublistLoader().deletePlaylist(pname);
                    event.reply(event.getClient().getSuccess() + L.t("playlists.playlist") + pname + L.t("publist.deleted")).queue();
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + L.t("playlists.could_not_delete_playlist") + e.getLocalizedMessage()).queue();
                }
            }
        }

        @Override
        protected void execute(CommandEvent event) {
            String pname = event.getArgs().replaceAll("\\s+", "_");
            if (bot.getPublistLoader().getPlaylist(pname) == null)
                event.reply(event.getClient().getError() + L.t("playlists.playlist") + pname + L.t("publist.does_not_exist"));
            else {
                try {
                    bot.getPublistLoader().deletePlaylist(pname);
                    event.reply(event.getClient().getSuccess() + L.t("playlists.playlist") + pname + L.t("publist.deleted"));
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + L.t("playlists.could_not_delete_playlist") + e.getLocalizedMessage());
                }
            }
        }
    }

    public class AppendlistCmd extends OwnerCommand {
        public AppendlistCmd() {
            this.name = "append";
            this.aliases = new String[]{"add"};
            this.help = L.t("playlists.adds_tracks_existing_playlist");
            this.arguments = L.t("mylist.name_url_url");
            //this.guildOnly = false;
            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", L.t("play.playlist_name"), true));
            options.add(new OptionData(OptionType.STRING, "url", "URL", true));
            this.options = options;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            String pname = event.getOption("name").getAsString();
            Playlist playlist = bot.getPublistLoader().getPlaylist(pname);
            if (playlist == null)
                event.reply(event.getClient().getError() + L.t("playlists.playlist") + pname + L.t("publist.does_not_exist")).queue();
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
                    bot.getPublistLoader().writePlaylist(pname, builder.toString());
                    event.reply(event.getClient().getSuccess() + urls.length + L.t("publist.items_added_playlist") + pname + "`!").queue();
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + L.t("playlists.could_not_add_playlist") + e.getLocalizedMessage()).queue();
                }
            }
        }

        @Override
        protected void execute(CommandEvent event) {
            String[] parts = event.getArgs().split("\\s+", 2);
            if (parts.length < 2) {
                event.reply(event.getClient().getError() + L.t("playlists.enter_playlist_name_url_add"));
                return;
            }
            String pname = parts[0];
            Playlist playlist = bot.getPublistLoader().getPlaylist(pname);
            if (playlist == null)
                event.reply(event.getClient().getError() + L.t("playlists.playlist") + pname + L.t("publist.does_not_exist"));
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
                    bot.getPublistLoader().writePlaylist(pname, builder.toString());
                    event.reply(event.getClient().getSuccess() + urls.length + L.t("publist.items_added_playlist") + pname + "`!");
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + L.t("playlists.could_not_add_playlist") + e.getLocalizedMessage());
                }
            }
        }
    }

    public class ListCmd extends OwnerCommand {
        public ListCmd() {
            this.name = "all";
            this.aliases = new String[]{"available", "list"};
            this.help = L.t("publist.shows_all_available_playlists");
            //this.guildOnly = true;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            if (!bot.getPublistLoader().folderExists())
                bot.getPublistLoader().createFolder();
            if (!bot.getPublistLoader().folderExists()) {
                event.reply(event.getClient().getWarning() + L.t("playlists.playlist_folder_did_not_exist")).queue();
                return;
            }
            List<String> list = bot.getPublistLoader().getPlaylistNames();
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

        @Override
        protected void execute(CommandEvent event) {
            if (!bot.getPublistLoader().folderExists())
                bot.getPublistLoader().createFolder();
            if (!bot.getPublistLoader().folderExists()) {
                event.reply(event.getClient().getWarning() + L.t("playlists.playlist_folder_did_not_exist"));
                return;
            }
            List<String> list = bot.getPublistLoader().getPlaylistNames();
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
    }
}
