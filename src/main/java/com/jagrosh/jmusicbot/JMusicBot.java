/*
 * Copyright 2016 John Grosh (jagrosh).
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
package com.jagrosh.jmusicbot;

import dev.cosgy.jmusicbot.i18n.L;

import com.github.lalyos.jfiglet.FigletFont;
import club.minnced.discord.jdave.interop.JDaveSessionFactory;
import dev.cosgy.jmusicbot.framework.jdautilities.command.Command;
import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandClientBuilder;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommand;
import dev.cosgy.jmusicbot.framework.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jmusicbot.entities.Prompt;
import com.jagrosh.jmusicbot.gui.GUI;
import com.jagrosh.jmusicbot.settings.SettingsManager;
import com.jagrosh.jmusicbot.utils.OtherUtil;
import dev.cosgy.agent.GensokyoInfoAgent;
import dev.cosgy.jmusicbot.slashcommands.admin.*;
import dev.cosgy.jmusicbot.slashcommands.dj.*;
import dev.cosgy.jmusicbot.slashcommands.general.*;
import dev.cosgy.jmusicbot.slashcommands.listeners.CommandAudit;
import dev.cosgy.jmusicbot.slashcommands.listeners.buttons.QueueButtonListener;
import dev.cosgy.jmusicbot.slashcommands.music.*;
import dev.cosgy.jmusicbot.slashcommands.owner.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author John Grosh (jagrosh)
 */
public class JMusicBot {
    /** Version shown in the console banner and on cards. Bumped on every change. */
    public final static String BOT_VERSION = "v1.0.0";
    public final static String PLAY_EMOJI = "▶"; // ▶
    public final static String PAUSE_EMOJI = "⏸"; // ⏸
    public final static String STOP_EMOJI = "⏹"; // ⏹
    public final static Permission[] RECOMMENDED_PERMS = {Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_MANAGE, Permission.MESSAGE_EXT_EMOJI,
            Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.NICKNAME_CHANGE, Permission.VOICE_SET_STATUS};
    public final static GatewayIntent[] INTENTS = {GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.MESSAGE_CONTENT}; // , GatewayIntent.MESSAGE_CONTENT
    public static boolean CHECK_UPDATE = false; // It checked the original project's (Cosgy-Dev) releases; disabled because it is meaningless for this build
    public static boolean COMMAND_AUDIT_ENABLED = false;

    /**
     * Builds the DAVE voice encryption settings.
     * Returns null if initialization fails, and the old encryption method is used.
     */
    private static AudioModuleConfig createDaveAudioModuleConfig(Logger log, Prompt prompt) {
        try {
            AudioModuleConfig config = new AudioModuleConfig()
                    .withDaveSessionFactory(new JDaveSessionFactory());
            log.info(L.t("jmusic_bot.dave_voice_encryption_enabled"));
            return config;
        } catch (Throwable t) {
            log.warn(L.t("jmusic_bot.could_not_start_dave_voice"), t.toString());
            prompt.alert(Prompt.Level.WARNING, "DAVE",
                    L.t("jmusic_bot.dave_could_not_started_continuing") + t.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * @param args the command line arguments
     */
    /** Deletes log files older than 7 days in the logs folder (including leftovers from old versions). */
    private static void cleanOldLogs(Logger log) {
        java.nio.file.Path dir = java.nio.file.Paths.get("logs");
        if (!java.nio.file.Files.isDirectory(dir)) return;
        long limit = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        int removed = 0;
        long bytes = 0;
        try (var st = java.nio.file.Files.list(dir)) {
            for (java.nio.file.Path p : (Iterable<java.nio.file.Path>) st::iterator) {
                String n = p.getFileName().toString();
                if (!java.nio.file.Files.isRegularFile(p) || !(n.endsWith(".log") || n.endsWith(".gz"))) continue;
                if (n.equals("JMusicBotJP.log")) continue; // the file currently being written
                if (java.nio.file.Files.getLastModifiedTime(p).toMillis() >= limit) continue;
                long sz = java.nio.file.Files.size(p);
                if (java.nio.file.Files.deleteIfExists(p)) { removed++; bytes += sz; }
            }
        } catch (Exception e) {
            log.debug(L.t("jmusic_bot.could_not_clean_old_logs"), e.toString());
        }
        if (removed > 0) log.info(L.t("jmusic_bot.deleted_log_files_older_than"), removed, bytes / (1024 * 1024));
    }

    public static void main(String[] args) {
        // startup log
        Logger log = getLogger("Startup");

        try {
            System.out.println(FigletFont.convertOneLine("JMusicBotML" + BOT_VERSION) + "\n" + "based on JMusicBot-JP by Cosgy Dev");
        } catch (IOException e) {
            System.out.println("JMusicBotML" + BOT_VERSION + "\nbased on JMusicBot-JP by Cosgy Dev");
        }
        cleanOldLogs(log);


        // create prompt to handle startup
        Prompt prompt = new Prompt("JMusicBot", L.t("jmusic_bot.switching_nogui_mode_can_start"));

        // check deprecated nogui mode (new way of setting it is -Dnogui=true)
        for (String arg : args)
            if ("-nogui".equalsIgnoreCase(arg)) {
                prompt.alert(Prompt.Level.WARNING, "GUI", L.t("jmusic_bot.nogui_parameter_will_removed")
                        + L.t("jmusic_bot.please_use_dnogui_true_before")
                        + L.t("jmusic_bot.example_java_enable_native_access"));
            } else if ("-nocheckupdates".equalsIgnoreCase(arg)) {
                CHECK_UPDATE = false;
                log.info(L.t("jmusic_bot.update_check_disabled"));
            } else if ("-auditcommands".equalsIgnoreCase(arg)) {
                COMMAND_AUDIT_ENABLED = true;
                log.info(L.t("jmusic_bot.command_audit_logging_enabled"));
            }

        // get and check latest version
        String version = OtherUtil.checkVersion(prompt);

        if (!System.getProperty("java.vm.name").contains("64"))
            prompt.alert(Prompt.Level.WARNING, "Java Version", L.t("jmusic_bot.are_using_unsupported_java_version"));



        // load config
        BotConfig config = new BotConfig(prompt);
        config.load();

        if (!config.isValid())
            return;


        if (config.getAuditCommands()) {
            COMMAND_AUDIT_ENABLED = true;
            log.info(L.t("jmusic_bot.command_audit_logging_enabled"));
        }

        // set up the listener
        EventWaiter waiter = new EventWaiter();
        SettingsManager settings = new SettingsManager();
        Bot bot = new Bot(waiter, config, settings);
        Bot.INSTANCE = bot;

        AboutCommand aboutCommand = new AboutCommand(Color.BLUE.brighter(),
                "[JMusicBot JP(v" + version + ")](https://github.com/Cosgy-Dev/MusicBot-JP-java)",
                new String[]{L.t("jmusic_bot.high_quality_music_playback"), L.t("jmusic_bot.fairqueue_technology"), L.t("jmusic_bot.easy_host_yourself")},
                RECOMMENDED_PERMS);
        aboutCommand.setIsAuthor(false);
        aboutCommand.setReplacementCharacter("\uD83C\uDFB6"); // 🎶

        // set up the command client
        CommandClientBuilder cb = new CommandClientBuilder()
                .setPrefix(config.getPrefix())
                .setAlternativePrefix(config.getAltPrefix())
                .setOwnerId(Long.toString(config.getOwnerId()))
                .setEmojis(config.getSuccess(), config.getWarning(), config.getError())
                .useHelpBuilder(false)
                .setLinkedCacheSize(200)
                .setGuildSettingsManager(settings)
                .setListener(new CommandAudit());

        if (config.isOfficialInvite()) {
            cb.setServerInvite("https://discord.gg/MjNfC6TK2y");
        }

        // Slash command definitions
        List<SlashCommand> slashCommandList = new ArrayList<>() {{
            add(new HelpCmd(bot));
            add(aboutCommand);
            if (config.isUseInviteCommand()) {
                add(new InviteCommand());
            }
            add(new PingCommand());
            add(new SettingsCmd(bot));
            //if (config.getCosgyDevHost()) add(new InfoCommand(bot));
            // General
            add(new ServerInfo(bot));
            //add(new UserInfo());
            add(new CashCmd(bot));
            // Music
            add(new LyricsCmd(bot));
            add(new NowplayingCmd(bot));
            add(new PlayCmd(bot));
            add(new SpotifyCmd(bot));
            add(new PlaylistsCmd(bot));
            add(new MylistCmd(bot));
            //add(new QueueCmd(bot));
            add(new QueueCmd(bot));
            add(new RemoveCmd(bot));
            add(new SearchCmd(bot));
            add(new SCSearchCmd(bot));
            add(new SeekCmd(bot));
            add(new NicoSearchCmd(bot));
            add(new ShuffleCmd(bot));
            add(new SkipCmd(bot));
            add(new VolumeCmd(bot));
            // DJ
            add(new ForceRemoveCmd(bot));
            add(new ForceskipCmd(bot));
            add(new NextCmd(bot));
            add(new MoveTrackCmd(bot));
            add(new PauseCmd(bot));
            add(new PlaynextCmd(bot));
            //add(new RepeatCmd(bot));
            add(new RepeatCmd(bot));
            add(new SkipToCmd(bot));
            add(new ForceToEnd(bot));
            add(new StopCmd(bot));
            //add(new VolumeCmd(bot));
            // Admin
            //add(new ActivateCmd(bot));
            add(new PrefixCmd(bot));
            add(new SetdjCmd(bot));
            add(new SkipratioCmd(bot));
            add(new SettcCmd(bot));
            add(new SetvcCmd(bot));
            add(new SetvcStatusCmd(bot));
            add(new AutoplaylistCmd(bot));
            add(new ServerListCmd(bot));
            // Owner
            add(new DebugCmd(bot));
            add(new SetavatarCmd(bot));
            add(new SetgameCmd(bot));
            add(new SetnameCmd(bot));
            add(new SetstatusCmd(bot));
            add(new PublistCmd(bot));
            add(new ShutdownCmd(bot));
            //add(new LeaveCmd(bot));
        }};

        cb.addCommands(slashCommandList.toArray(new Command[0]));

        if (config.useEval())
            cb.addCommand(new EvalCmd(bot));
        boolean nogame = false;
        if (config.getStatus() != OnlineStatus.UNKNOWN)
            cb.setStatus(config.getStatus());
        if (config.getGame() == null)
            cb.setActivity(Activity.playing(config.getPrefix() + config.getHelp() + L.t("jmusic_bot.help")));
        else if (config.getGame().getName().toLowerCase().matches("(none|yok|hicbiri|hiçbiri|\u306a\u3057)")) {
            cb.setActivity(null);
            nogame = true;
        } else
            cb.setActivity(config.getGame());
        if (!prompt.isNoGUI()) {
            try {
                GUI gui = new GUI(bot);
                bot.setGUI(gui);
                gui.init();
            } catch (Exception e) {
                log.error(L.t("jmusic_bot.could_not_open_graphical_interface")
                        + L.t("jmusic_bot.bot_running_server")
                        + L.t("jmusic_bot.running_environment_without_display")
                        + L.t("jmusic_bot.hide_error_run_bot_headless"));
            }
        }

        log.info(L.t("jmusic_bot.settings_loaded_from"), config.getConfigLocation());

        // attempt to log in and start
        try {
            JDABuilder jdaBuilder = JDABuilder.create(config.getToken(), Arrays.asList(INTENTS))
                    .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                    .disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.EMOJI, CacheFlag.ONLINE_STATUS,
                            CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS)
                    .setActivity(nogame ? null : Activity.playing(L.t("jmusic_bot.loading")))
                    .setStatus(config.getStatus() == OnlineStatus.INVISIBLE || config.getStatus() == OnlineStatus.OFFLINE
                            ? OnlineStatus.INVISIBLE : OnlineStatus.DO_NOT_DISTURB);

            AudioModuleConfig daveConfig = createDaveAudioModuleConfig(log, prompt);
            if (daveConfig != null) {
                jdaBuilder.setAudioModuleConfig(daveConfig);
            }

            JDA jda = jdaBuilder
                    .addEventListeners(cb.build(), waiter, new Listener(bot), new QueueButtonListener(bot), new dev.cosgy.jmusicbot.slashcommands.listeners.buttons.NowPlayingButtonListener(bot))
                    .setBulkDeleteSplittingEnabled(true)
                    .build();
            bot.setJDA(jda);

            String unsupportedReason = OtherUtil.getUnsupportedBotReason(jda);
            if (unsupportedReason != null)
            {
                prompt.alert(Prompt.Level.ERROR, "JMusicBot", L.t("jmusic_bot.jmusicbot_cannot_run_with_discord") + unsupportedReason);
                try{ Thread.sleep(5000);}catch(InterruptedException ignored){} // this is awful but until we have a better way...
                jda.shutdown();
                System.exit(1);
            }

            // other check that will just be a warning now but may be required in the future
            // check if the user has changed the prefix and provide info about the
            // message content intent
            /*if(!"@mention".equals(config.getPrefix()))
            {
                prompt.alert(Prompt.Level.INFO, "JMusicBot", L.t("jmusic_bot.custom_command_prefix_set")
                        + L.t("jmusic_bot.if_custom_prefix_doesn_t")
                        + "https://discord.com/developers/applications/" + jda.getSelfUser().getId() + "/bot");
            }*/

        }
        catch (InvalidTokenException ex) {
            //ex.getCause().getMessage();
            prompt.alert(Prompt.Level.ERROR, "JMusicBot", ex + "\n" +
                    L.t("jmusic_bot.make_sure_edited_correct_config") +
                    L.t("jmusic_bot.please_enter_correct_bot_token") +
                    L.t("jmusic_bot.config_file_location") + config.getConfigLocation());
            System.exit(1);

        } catch (IllegalArgumentException ex) {

            prompt.alert(Prompt.Level.ERROR, "JMusicBot", L.t("jmusic_bot.some_settings_are_invalid") + ex + "\n" +
                    L.t("jmusic_bot.config_file_location") + config.getConfigLocation());
            System.exit(1);
        }

        new GensokyoInfoAgent().start();
    }
}
