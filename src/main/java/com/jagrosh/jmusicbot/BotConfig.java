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
package com.jagrosh.jmusicbot;

import dev.cosgy.jmusicbot.i18n.L;

import com.jagrosh.jmusicbot.entities.Prompt;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.jagrosh.jmusicbot.utils.OtherUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.parser.ConfigDocument;
import com.typesafe.config.parser.ConfigDocumentFactory;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * @author John Grosh (jagrosh)
 */
public class BotConfig {
    private final static String CONTEXT = "Config";
    private final static String START_TOKEN = "/// START OF JMUSICBOT-JP CONFIG ///";
    private final static String END_TOKEN = "/// END OF JMUSICBOT-JP CONFIG ///";
    private final static String CONFIG_VERSION_KEY = "configversion";
    private final Prompt prompt;
    private Path path = null;
    // [JMusicBot-JP] added nicoEmail, nicoPass
    private String token;
    private String prefix;
    private String altprefix;
    private String helpWord;
    private String playlistsFolder;
    private String mylistfolder;
    private String publistFolder;
    private String successEmoji;
    private String warningEmoji;
    private String errorEmoji;
    private String loadingEmoji;
    private String searchingEmoji;
    private String nicoEmail;
    private String nicoPass;
    private static String nicoTwoFactor;
    private String ytEmail;
    private String ytPass;
    private boolean youtubeOauth2;
    private String youtubeOauth2RefreshToken;
    private long cacheLimitMb;
    private String failNotice;
    private String spClientId;
    private String spClientSecret;
    // [JMusicBot-JP] added useNicoNico, changeNickName, pauseNoUsers, resumeJoined, stopNoUsers, cosgyDevHost, helpToDm, officialInvite
    private boolean useNicoNico, changeNickName, stayInChannel, pauseNoUsers, resumeJoined, stopNoUsers, songInGame, npImages, updatealerts, useEval, dbots, cosgyDevHost, helpToDm, autoStopQueueSave, auditCommands, officialInvite, useinvitecommand;
    private long owner, maxSeconds, aloneTimeUntilStop;
    private OnlineStatus status;
    private Activity game;
    private Config aliases, transforms;

    private boolean valid = false;

    public BotConfig(Prompt prompt) {
        this.prompt = prompt;
    }

    public void load() {
        valid = false;

        // Read the settings from the file
        try {
            // Get the path of the config file (default: config.txt)
            path = OtherUtil.getPath(System.getProperty("config.file", System.getProperty("config", "config.txt")));
            if (path.toFile().exists()) {
                try {
                    autoUpdateConfigIfNeeded();
                } catch (Exception ex) {
                    prompt.alert(Prompt.Level.WARNING, CONTEXT, L.t("bot_config.config_txt_could_not_updated") + ex.getMessage());
                }
                if (System.getProperty("config.file") == null)
                    System.setProperty("config.file", System.getProperty("config", path.toAbsolutePath().toString()));
                ConfigFactory.invalidateCaches();
            }

            // The config file is loaded and default values are added
            //Config config = ConfigFactory.parseFile(path.toFile()).withFallback(ConfigFactory.load());
            Config config = ConfigFactory.load();
            // Setting values
            token = config.getString("token");
            prefix = config.getString("prefix");
            altprefix = config.getString("altprefix");
            helpWord = config.getString("help");
            owner = (config.getAnyRef("owner") instanceof String ? 0L : config.getLong("owner"));
            successEmoji = config.getString("success");
            warningEmoji = config.getString("warning");
            errorEmoji = config.getString("error");
            loadingEmoji = config.getString("loading");
            searchingEmoji = config.getString("searching");
            game = OtherUtil.parseGame(config.getString("game"));
            status = OtherUtil.parseStatus(config.getString("status"));
            stayInChannel = config.getBoolean("stayinchannel");
            songInGame = config.getBoolean("songinstatus");
            npImages = config.getBoolean("npimages");
            updatealerts = config.getBoolean("updatealerts");
            useEval = config.getBoolean("eval");
            maxSeconds = config.getLong("maxtime");
            aloneTimeUntilStop = config.getLong("alonetimeuntilstop");
            playlistsFolder = config.getString("playlistsfolder");
            mylistfolder = config.getString("mylistfolder");
            publistFolder = config.getString("publistfolder");
            aliases = config.getConfig("aliases");
            transforms = config.getConfig("transforms");
            dbots = owner == 334091398263341056L;


            // [JMusicBot-JP]
            useNicoNico = config.getBoolean("useniconico");
            nicoEmail = config.getString("nicomail");
            nicoPass = config.getString("nicopass");
            nicoTwoFactor = config.getString("nicotwofactor");
            pauseNoUsers = config.getBoolean("pausenousers");
            resumeJoined = config.getBoolean("resumejoined");
            stopNoUsers = config.getBoolean("stopnousers");
            changeNickName = config.getBoolean("changenickname");
            helpToDm = config.getBoolean("helptodm");
            autoStopQueueSave = config.getBoolean("autostopqueuesave");
            auditCommands = config.getBoolean("auditcommands");
            officialInvite = config.getBoolean("officialinvite");
            useinvitecommand = config.getBoolean("useinvitecommand");
            ytEmail = config.getString("ytemail");
            ytPass = config.getString("ytpass");
            youtubeOauth2 = config.getBoolean("youtubeoauth2");
            youtubeOauth2RefreshToken = config.getString("youtubeoauth2refreshtoken");
            cacheLimitMb = config.hasPath("cachelimitmb") ? config.getLong("cachelimitmb") : 1024L;
            failNotice = config.hasPath("failnotice") ? config.getString("failnotice").trim().toLowerCase(java.util.Locale.ROOT) : "none";
            spClientId = config.getString("spclient");
            spClientSecret = config.getString("spsecret");


            cosgyDevHost = false;
            // [JMusicBot-JP] End

            // we may need to write a new config file
            boolean write = false;

            // validate bot token
            if (token == null || token.isEmpty() || token.matches("(BOT_TOKEN_HERE|BOT_TOKEN_BURAYA)")) {
                token = prompt.prompt(L.t("bot_config.please_enter_bot_token")
                        + L.t("bot_config.learn_how_get_token_here")
                        + "\nhttps://github.com/jagrosh/MusicBot/wiki/Getting-a-Bot-Token."
                        + L.t("bot_config.bot_token"));
                if (token == null) {
                    prompt.alert(Prompt.Level.WARNING, CONTEXT, L.t("bot_config.no_token_entered_shutting_down") + path.toAbsolutePath());
                    return;
                } else {
                    write = true;
                }
            }

            // validate bot owner
            if (owner <= 0) {
                try {
                    owner = Long.parseLong(prompt.prompt(L.t("bot_config.owner_user_id_not_set")
                            + L.t("bot_config.please_enter_bot_owner_s")
                            + L.t("bot_config.learn_how_find_user_id")
                            + "\nhttps://github.com/jagrosh/MusicBot/wiki/Finding-Your-User-ID"
                            + L.t("bot_config.owner_user_id")));
                } catch (NumberFormatException | NullPointerException ex) {
                    owner = 0;
                }
                if (owner <= 0) {
                    prompt.alert(Prompt.Level.ERROR, CONTEXT, L.t("bot_config.invalid_user_id_shutting_down") + path.toAbsolutePath());
                    System.exit(0);
                } else {
                    write = true;
                }
            }

            if (write) {
                String template;
                try {
                    template = extractTemplateConfig();
                } catch (IOException e) {
                    template = null;
                }
                String mod;
                if (template == null) {
                    mod = ("token = " + token + "\r\nowner = " + owner);
                } else {
                    mod = template
                            .replace("BOT_TOKEN_HERE", token).replace("BOT_TOKEN_BURAYA", token)
                            .replace("OWNER_ID_HERE", Long.toString(owner)).replace("SAHIP_ID_BURAYA", Long.toString(owner))
                            .trim() + "\n";
                }

                FileUtils.writeStringToFile(path.toFile(), mod, StandardCharsets.UTF_8);
            }

            // if we get through the whole config, it's good to go
            valid = true;
        } catch (ConfigException | IOException ex) {
            prompt.alert(Prompt.Level.ERROR, CONTEXT, ex + ": " + ex.getMessage() + L.t("bot_config.config_file_location") + path.toAbsolutePath());
        }
    }

    public boolean isValid() {
        return valid;
    }

    public String getConfigLocation() {
        return path.toFile().getAbsolutePath();
    }

    public String getPrefix() {
        return prefix;
    }

    public String getAltPrefix() {
        if (altprefix == null) return null;
        String a = altprefix.trim();
        // "NONE", "YOK" and the old Japanese version's default ("nashi") disable the second prefix
        return a.isEmpty() || a.equalsIgnoreCase("NONE") || a.equalsIgnoreCase("YOK") || a.equals("\u306a\u3057") ? null : altprefix;
    }

    public String getToken() {
        return token;
    }

    public long getOwnerId() {
        return owner;
    }

    public String getSuccess() {
        return successEmoji;
    }

    public String getWarning() {
        return warningEmoji;
    }

    public String getError() {
        return errorEmoji;
    }

    public String getLoading() {
        return loadingEmoji;
    }

    public String getSearching() {
        return searchingEmoji;
    }

    public Activity getGame() {
        return game;
    }

    public OnlineStatus getStatus() {
        return status;
    }

    public String getHelp() {
        return helpWord;
    }

    public boolean getStay() {
        return stayInChannel;
    }

    public boolean getNoUserPause() {
        return pauseNoUsers;
    }

    public boolean getResumeJoined() {
        return resumeJoined;
    }

    public boolean getNoUserStop() {
        return stopNoUsers;
    }

    public long getAloneTimeUntilStop() {
        return aloneTimeUntilStop;
    }

    public boolean getChangeNickName() {
        return changeNickName;
    }

    public boolean getSongInStatus() {
        return songInGame;
    }

    public String getPlaylistsFolder() {
        return playlistsFolder;
    }

    public String getMylistfolder() {
        return mylistfolder;
    }

    public String getPublistFolder() {
        return publistFolder;
    }

    public boolean getDBots() {
        return dbots;
    }

    public boolean useUpdateAlerts() {
        return updatealerts;
    }

    public boolean useEval() {
        return useEval;
    }

    public boolean useNPImages() {
        return npImages;
    }

    public long getMaxSeconds() {
        return maxSeconds;
    }

    public String getMaxTime() {
        return FormatUtil.formatTime(maxSeconds * 1000);
    }

    public boolean isTooLong(AudioTrack track) {
        if (maxSeconds <= 0)
            return false;
        return Math.round(track.getDuration() / 1000.0) > maxSeconds;
    }

    public String[] getAliases(String command) {
        try {
            return aliases.getStringList(command).toArray(new String[0]);
        } catch (NullPointerException | ConfigException.Missing e) {
            return new String[0];
        }
    }

    // [JMusicBot-JP] new function: support niconico play
    public boolean isNicoNicoEnabled() {
        return useNicoNico;
    }

    public String getNicoNicoEmailAddress() {
        return nicoEmail;
    }

    public String getNicoNicoPassword() {
        return nicoPass;
    }

    public static String getNicoNicoTwoFactor(){ return nicoTwoFactor; }

    public String getYouTubeEmailAddress() {
        return ytEmail;
    }

    public String getYouTubePassword() {
        return ytPass;
    }

    public boolean isYouTubeOauth2Enabled() {
        return youtubeOauth2;
    }

    /** Maximum size of the cache folder (MB). At least 100. */
    /** Notice for tracks that cannot be played: none / dm / channel */
    public String getFailNotice() {
        return failNotice == null || !(failNotice.equals("dm") || failNotice.equals("channel")) ? "none" : failNotice;
    }

    public long getCacheLimitMb() {
        return Math.max(100L, cacheLimitMb);
    }

    public String getYouTubeOauth2RefreshToken() {
        return youtubeOauth2RefreshToken;
    }

    public String getSpotifyClientId(){return spClientId;}

    public String getSpotifyClientSecret(){return spClientSecret;}

    // [JMusicBot-JP] End

    public boolean getCosgyDevHost() {
        return cosgyDevHost;
    }

    public boolean getHelpToDm() {
        return helpToDm;
    }

    public boolean getAutoStopQueueSave() {
        return autoStopQueueSave;
    }

    public boolean getAuditCommands() {
        return auditCommands;
    }

    public Config getTransforms() {
        return transforms;
    }

    public boolean isOfficialInvite() {
        return officialInvite;
    }

    public boolean isUseInviteCommand() {
        return useinvitecommand;
    }

    private void autoUpdateConfigIfNeeded() throws IOException {
        String template = extractTemplateConfig();
        Config defaultConfig = ConfigFactory.parseString(template);
        int latestVersion = defaultConfig.hasPath(CONFIG_VERSION_KEY) ? defaultConfig.getInt(CONFIG_VERSION_KEY) : 1;

        Config oldConfig = ConfigFactory.parseFile(path.toFile());
        int currentVersion = oldConfig.hasPath(CONFIG_VERSION_KEY) ? oldConfig.getInt(CONFIG_VERSION_KEY) : 0;
        // Also rewrite the file when its comments are in a different language than the selected one
        String oldText = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        boolean languageChanged = !oldText.contains("[lang:" + L.lang() + "]");
        if (currentVersion >= latestVersion && !languageChanged)
            return;

        Path backupPath = OtherUtil.getPath(path.toString() + ".v" + currentVersion + ".bak");
        Files.copy(path, backupPath, StandardCopyOption.REPLACE_EXISTING);

        ConfigDocument updatedDoc = ConfigDocumentFactory.parseString(template);
        for (String entryPath : oldConfig.entrySet().stream().map(java.util.Map.Entry::getKey).toList()) {
            // The version number must not be copied from the old file; otherwise it would be updated again on every start
            if (!defaultConfig.hasPath(entryPath) || CONFIG_VERSION_KEY.equals(entryPath))
                continue;
            ConfigValue oldValue = oldConfig.getValue(entryPath);
            updatedDoc = updatedDoc.withValueText(entryPath, oldValue.render(ConfigRenderOptions.concise()));
        }

        FileUtils.writeStringToFile(path.toFile(), updatedDoc.render(), StandardCharsets.UTF_8);
        if (currentVersion == latestVersion)
            prompt.alert(Prompt.Level.INFO, CONTEXT, L.f("bot_config.config_rewritten_in_language", L.lang()) + L.t("bot_config.backup") + backupPath.toAbsolutePath());
        else
            prompt.alert(Prompt.Level.INFO, CONTEXT, L.t("bot_config.config_txt_was_automatically_updated") + currentVersion + L.t("bot_config.v") + latestVersion
                    + L.t("bot_config.backup") + backupPath.toAbsolutePath());
    }

    private String extractTemplateConfig() throws IOException {
        // Template in the selected language (comments translated); English is the fallback
        String localized = OtherUtil.loadResource(this, "/lang/config/" + L.lang() + ".conf");
        if (localized != null && !localized.isBlank()) return localized.trim();
        String original = OtherUtil.loadResource(this, "/reference.conf");
        if (original == null) {
            throw new IOException(L.t("bot_config.could_not_read_reference_conf"));
        }
        int start = original.indexOf(START_TOKEN);
        int end = original.indexOf(END_TOKEN);
        if (start < 0 || end < 0 || end <= start) {
            throw new IOException(L.t("bot_config.template_section_not_found_reference"));
        }
        return original.substring(start + START_TOKEN.length(), end).trim();
    }
}
