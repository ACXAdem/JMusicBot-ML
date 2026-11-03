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

import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import dev.cosgy.jmusicbot.lyrics.LyricsService;
import dev.cosgy.jmusicbot.slashcommands.MusicCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows lyrics. If no track name is given, searches the lyrics of the current track.
 * Source: LRCLIB (fallback: MusicMatch / AZLyrics etc.)
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class LyricsCmd extends MusicCommand {
    /** Single instance for the whole bot; the "Lyrics" button on the now-playing card uses it too. */
    public static final LyricsService SERVICE = new LyricsService();
    private static final Color COLOR = new Color(233, 30, 99);

    public LyricsCmd(Bot bot) {
        super(bot);
        this.name = "lyrics";
        this.arguments = L.t("lyrics.artist_title");
        this.help = L.t("lyrics.shows_lyrics_current_track_track");
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.aliases = bot.getConfig().getAliases(this.name);
        this.bePlaying = false;

        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.STRING, "name", L.t("lyrics.artist_title_leave_empty_current"), false));
        this.options = options;
    }

    /** Determines the query: the given text if any, otherwise the current track. Null if nothing is playing. */
    private static LyricsService.Query query(AudioHandler handler, String text) {
        if (text != null && !text.isBlank()) return LyricsService.fromText(text, null, 0);
        if (handler == null || handler.getPlayer().getPlayingTrack() == null) return null;
        return LyricsService.fromTrack(handler.getPlayer().getPlayingTrack());
    }

    @Override
    public void doCommand(SlashCommandEvent event) {
        String text = event.getOption("name") == null ? null : event.getOption("name").getAsString();
        LyricsService.Query q = query((AudioHandler) event.getGuild().getAudioManager().getSendingHandler(), text);
        if (q == null) {
            event.reply(event.getClient().getError() + L.t("lyrics.nothing_playing_right_now_search")).setEphemeral(true).queue();
            return;
        }
        // The search can take a few seconds; don't let the Discord interaction time out
        event.deferReply().queue();
        SERVICE.find(q).thenAccept(r -> {
            if (r == null) {
                event.getHook().sendMessage(event.getClient().getError() + " `" + q.display() + L.t("lyrics.no_lyrics_found")
                        + (text == null ? L.t("lyrics.try_typing_track_name_manually") : "")).queue();
                return;
            }
            for (MessageEmbed e : LyricsService.toEmbeds(r, COLOR)) event.getHook().sendMessageEmbeds(e).queue();
        });
    }

    @Override
    public void doCommand(CommandEvent event) {
        String text = event.getArgs();
        LyricsService.Query q = query((AudioHandler) event.getGuild().getAudioManager().getSendingHandler(), text);
        if (q == null) {
            event.replyError(L.t("lyrics.nothing_playing_right_now_search_2") + event.getClient().getPrefix() + L.t("lyrics.lyrics_coldplay_yellow"));
            return;
        }
        event.getChannel().sendTyping().queue();
        SERVICE.find(q).thenAccept(r -> {
            if (r == null) {
                event.replyError("`" + q.display() + L.t("lyrics.no_lyrics_found")
                        + (text.isBlank() ? L.t("lyrics.try_typing_track_name_manually_2") + event.getClient().getPrefix() + L.t("lyrics.lyrics_artist_title") : ""));
                return;
            }
            for (MessageEmbed e : LyricsService.toEmbeds(r, COLOR)) event.reply(e);
        });
    }
}
