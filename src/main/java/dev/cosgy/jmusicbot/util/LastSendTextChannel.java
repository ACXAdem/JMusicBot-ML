package dev.cosgy.jmusicbot.util;

import dev.cosgy.jmusicbot.i18n.L;

import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandListener;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class LastSendTextChannel implements CommandListener {
    // Gets the text channel ID by server ID.
    private static final java.util.Map<Long, Long> textChannel = new java.util.concurrent.ConcurrentHashMap<>();
    static Logger log = LoggerFactory.getLogger("LastSendTextChannel");

    public static void SetLastTextId(CommandEvent event) {
        textChannel.put(event.getGuild().getIdLong(), event.getChannel().getIdLong());
    }

    public static void SetLastTextId(long guildId, long channelId) {
        textChannel.put(guildId, channelId);
    }

    public static long GetLastTextId(long guildId) {
        long id;
        if (textChannel.containsKey(guildId)) {
            id = textChannel.get(guildId);
        } else {
            id = 0;
        }
        return id;
    }

    public static void SendMessage(Guild guild, String message) {
        log.debug(L.t("last_send_text_channel.sending_message"));
        long textId = GetLastTextId(guild.getIdLong());
        if (textId == 0) {
            log.debug(L.t("last_send_text_channel.could_not_send_message_no"));
            return;
        }
        MessageChannel channel = guild.getChannelById(net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel.class, textId);
        if (channel == null) {
            log.debug(L.t("last_send_text_channel.could_not_send_message_recorded"));
            return;
        }
        channel.sendMessage(message).queue(null, t -> log.debug(L.t("last_send_text_channel.could_not_send_message"), t.toString()));
    }
}
