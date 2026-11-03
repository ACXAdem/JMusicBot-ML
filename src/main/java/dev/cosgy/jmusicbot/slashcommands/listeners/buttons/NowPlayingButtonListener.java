/*
 *  Copyright 2026 JMusicBot-ML contributors
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
package dev.cosgy.jmusicbot.slashcommands.listeners.buttons;

import dev.cosgy.jmusicbot.i18n.L;

import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.PlayStatus;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.audio.RequestMetadata;
import com.jagrosh.jmusicbot.settings.Settings;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.cosgy.jmusicbot.lyrics.LyricsService;
import dev.cosgy.jmusicbot.settings.RepeatMode;
import dev.cosgy.jmusicbot.slashcommands.music.LyricsCmd;
import dev.cosgy.jmusicbot.util.NowPlayingCard;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Handles the buttons on the "Now playing" card.
 *
 * <p>Permissions are the same as the commands: pause/resume and skip (without a vote) are open to the requester or a DJ;
 * previous, stop and repeat are DJ-only; when others press skip, it counts as a vote.
 * The control buttons require being in the same voice channel as the bot.</p>
 */
public class NowPlayingButtonListener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger("NowPlayingButtons");
    private final Bot bot;

    public NowPlayingButtonListener(Bot bot) {
        this.bot = bot;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith(NowPlayingCard.PREFIX)) return;
        String action = id.substring(NowPlayingCard.PREFIX.length());

        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null) {
            event.reply(L.t("now_playing_button_listener.button_can_only_used_server")).setEphemeral(true).queue();
            return;
        }
        AudioHandler handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
        boolean playing = handler != null && handler.getPlayer().getPlayingTrack() != null;

        try {
            // Info buttons: no voice channel requirement
            if (action.equals("queue")) {
                showQueue(event, handler);
                return;
            }
            if (action.equals("lyrics")) {
                showLyrics(event, handler);
                return;
            }

            if (!playing) {
                event.reply(L.t("now_playing_button_listener.nothing_playing_right_now")).setEphemeral(true).queue();
                if (handler != null) {
                    MessageCreateData none = handler.getNoMusicPlaying(event.getJDA());
                    event.getMessage().editMessage(new MessageEditBuilder().applyCreateData(none).setComponents().build()).queue(null, t -> { });
                }
                return;
            }

            AudioChannel botChannel = guild.getSelfMember().getVoiceState().getChannel();
            AudioChannel userChannel = member.getVoiceState() == null ? null : member.getVoiceState().getChannel();
            if (botChannel == null || userChannel == null || botChannel.getIdLong() != userChannel.getIdLong()) {
                event.reply(L.t("now_playing_button_listener.must_same_voice_channel_as")
                        + (botChannel == null ? "." : ": " + botChannel.getAsMention())).setEphemeral(true).queue();
                return;
            }

            boolean dj = isDJ(member);
            RequestMetadata rm = handler.getRequestMetadata();
            boolean requester = rm.getOwner() == member.getIdLong();
            Settings settings = bot.getSettingsManager().getSettings(guild);

            switch (action) {
                case "pause": {
                    if (!dj && !requester) {
                        deny(event, L.t("now_playing_button_listener.must_dj_have_requested_track"));
                        return;
                    }
                    boolean nowPaused = !handler.getPlayer().isPaused();
                    handler.getPlayer().setPaused(nowPaused);
                    Bot.updatePlayStatus(guild, guild.getSelfMember(), nowPaused ? PlayStatus.PAUSED : PlayStatus.PLAYING);
                    refreshCard(event, handler);
                    break;
                }
                case "skip": {
                    if (dj || requester) {
                        event.deferEdit().queue();
                        handler.getPlayer().stopTrack();
                        // The card updates itself when a new track starts
                    } else {
                        voteSkip(event, guild, handler, rm);
                    }
                    break;
                }
                case "prev": {
                    if (!dj) {
                        deny(event, L.t("now_playing_button_listener.must_dj_go_back_previous"));
                        return;
                    }
                    if (!handler.playPrevious()) {
                        event.reply(L.t("now_playing_button_listener.there_are_no_previously_played")).setEphemeral(true).queue();
                        return;
                    }
                    event.deferEdit().queue();
                    break;
                }
                case "stop": {
                    if (!dj) {
                        deny(event, L.t("now_playing_button_listener.must_dj_stop_playback"));
                        return;
                    }
                    event.deferEdit().queue();
                    handler.stopAndClear();
                    bot.closeAudioConnection(guild.getIdLong());
                    event.getChannel().sendMessage("⏹ " + member.getAsMention() + L.t("now_playing_button_listener.stopped_music_cleared_queue")).queue();
                    break;
                }
                case "repeat": {
                    if (!dj) {
                        deny(event, L.t("now_playing_button_listener.must_dj_change_repeat_mode"));
                        return;
                    }
                    RepeatMode cur = settings.getRepeatMode();
                    RepeatMode next = cur == RepeatMode.OFF ? RepeatMode.ALL : cur == RepeatMode.ALL ? RepeatMode.SINGLE : RepeatMode.OFF;
                    settings.setRepeatMode(next);
                    refreshCard(event, handler);
                    break;
                }
                case "voldown":
                case "volup": {
                    int v = handler.getPlayer().getVolume() + (action.equals("volup") ? 10 : -10);
                    v = Math.max(0, Math.min(150, v));
                    handler.getPlayer().setVolume(v);
                    settings.setVolume(v);
                    refreshCard(event, handler);
                    break;
                }
                case "shuffle": {
                    int s = handler.getQueue().shuffle(member.getIdLong());
                    if (s <= 1) {
                        event.reply(s == 0 ? L.t("now_playing_button_listener.have_no_tracks_queue_shuffle") : L.t("now_playing_button_listener.only_have_1_track_queue")).setEphemeral(true).queue();
                    } else {
                        event.reply(L.t("now_playing_button_listener.shuffled") + s + L.t("now_playing_button_listener.tracks_queue")).setEphemeral(true).queue();
                        bot.getNowplayingHandler().refresh(guild.getIdLong());
                    }
                    break;
                }
                default:
                    event.reply(L.t("now_playing_button_listener.unknown_button")).setEphemeral(true).queue();
            }
        } catch (Exception e) {
            log.warn(L.t("now_playing_button_listener.error_while_handling_button"), action, e.toString());
            if (!event.isAcknowledged()) event.reply(L.t("now_playing_button_listener.error_occurred") + e.getMessage()).setEphemeral(true).queue();
        }
    }

    /** Updates the card in place for the new state and remembers this message for live updates. */
    private void refreshCard(ButtonInteractionEvent event, AudioHandler handler) throws Exception {
        MessageCreateData data = handler.getNowPlaying(event.getJDA());
        if (data == null) data = handler.getNoMusicPlaying(event.getJDA());
        event.editMessage(new MessageEditBuilder().applyCreateData(data).setComponents(data.getComponents()).build()).queue();
        bot.getNowplayingHandler().setLastNPMessage(event.getMessage());
    }

    private void voteSkip(ButtonInteractionEvent event, Guild guild, AudioHandler handler, RequestMetadata rm) {
        AudioChannel channel = guild.getSelfMember().getVoiceState().getChannel();
        String uid = event.getUser().getId();
        boolean already = !handler.getVotes().add(uid);
        int listeners = (int) channel.getMembers().stream()
                .filter(m -> !m.getUser().isBot() && !m.getVoiceState().isDeafened() && m.getUser().getIdLong() != rm.getOwner())
                .count();
        int skippers = (int) channel.getMembers().stream().filter(m -> handler.getVotes().contains(m.getUser().getId())).count();
        int required = Math.max(1, (int) Math.ceil(listeners * bot.getSettingsManager().getSettings(guild).getSkipRatio()));
        if (skippers >= required) {
            String title = handler.getPlayer().getPlayingTrack().getInfo().title;
            event.reply(L.t("now_playing_button_listener.enough_votes") + FormatUtil.filter(title) + L.t("now_playing_button_listener.skipped") + skippers + "/" + required + "]`").queue();
            handler.getPlayer().stopTrack();
        } else {
            event.reply((already ? L.t("now_playing_button_listener.already_voted") : L.t("now_playing_button_listener.voted_skip")) + " `[" + skippers + L.t("skip.votes") + required + "/" + listeners + L.t("skip.needed"))
                    .setEphemeral(true).queue();
        }
    }

    private void showQueue(ButtonInteractionEvent event, AudioHandler handler) {
        if (handler == null || (handler.getQueue().isEmpty() && handler.getPlayer().getPlayingTrack() == null)) {
            event.reply(L.t("now_playing_button_listener.queue_empty")).setEphemeral(true).queue();
            return;
        }
        List<QueuedTrack> list = handler.getQueue().getList();
        StringBuilder sb = new StringBuilder();
        AudioTrack cur = handler.getPlayer().getPlayingTrack();
        if (cur != null) sb.append(L.t("now_playing_button_listener.now_playing")).append(FormatUtil.filter(cur.getInfo().title)).append("\n\n");
        if (list.isEmpty()) sb.append(L.t("now_playing_button_listener.no_tracks_up_next"));
        long total = 0;
        for (int i = 0; i < list.size(); i++) {
            long d = list.get(i).getTrack().getDuration();
            if (d > 0 && d != Long.MAX_VALUE) total += d;
            if (i < 15) {
                sb.append("`").append(i + 1).append(".` ").append(list.get(i).toString()).append("\n");
            }
        }
        if (list.size() > 15) sb.append(L.t("now_playing_button_listener.and")).append(list.size() - 15).append(L.t("now_playing_button_listener.more"));
        String text = sb.toString();
        if (text.length() > 3900) text = text.substring(0, 3899) + "…";
        MessageEmbed embed = new net.dv8tion.jda.api.EmbedBuilder()
                .setColor(new Color(88, 101, 242))
                .setTitle(L.t("now_playing_card.queue"))
                .setDescription(text)
                .setFooter(list.size() + L.t("now_playing_card.tracks") + FormatUtil.formatTime(total) + L.t("now_playing_button_listener.full_list_queue"))
                .build();
        event.replyEmbeds(embed).setEphemeral(true).queue();
    }

    private void showLyrics(ButtonInteractionEvent event, AudioHandler handler) {
        if (handler == null || handler.getPlayer().getPlayingTrack() == null) {
            event.reply(L.t("now_playing_button_listener.nothing_playing_right_now_2")).setEphemeral(true).queue();
            return;
        }
        LyricsService.Query q = LyricsService.fromTrack(handler.getPlayer().getPlayingTrack());
        event.deferReply(true).queue();
        LyricsCmd.SERVICE.find(q).orTimeout(30, TimeUnit.SECONDS).whenComplete((r, err) -> {
            if (err != null || r == null) {
                event.getHook().sendMessage("`" + q.display() + L.t("now_playing_button_listener.no_lyrics_found_can_search"))
                        .setEphemeral(true).queue();
                return;
            }
            for (MessageEmbed e : LyricsService.toEmbeds(r, new Color(233, 30, 99))) {
                event.getHook().sendMessageEmbeds(e).setEphemeral(true).queue();
            }
        });
    }

    private void deny(ButtonInteractionEvent event, String message) {
        event.reply("🚫 " + message).setEphemeral(true).queue();
    }

    private boolean isDJ(Member member) {
        if (member.getIdLong() == bot.getConfig().getOwnerId()) return true;
        if (member.hasPermission(Permission.MANAGE_SERVER)) return true;
        Role dj = bot.getSettingsManager().getSettings(member.getGuild()).getRole(member.getGuild());
        return dj != null && (member.getRoles().contains(dj) || dj.getIdLong() == member.getGuild().getIdLong());
    }
}
