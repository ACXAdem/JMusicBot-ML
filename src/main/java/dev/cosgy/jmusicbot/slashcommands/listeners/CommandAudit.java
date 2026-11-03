package dev.cosgy.jmusicbot.slashcommands.listeners;

import dev.cosgy.jmusicbot.i18n.L;

import dev.cosgy.jmusicbot.framework.jdautilities.command.Command;
import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandListener;
import com.jagrosh.jmusicbot.JMusicBot;
import dev.cosgy.jmusicbot.util.LastSendTextChannel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandAudit implements CommandListener {
    /**
     * Called when a {@link Command Command} is triggered
     * by a {@link CommandEvent CommandEvent}.
     *
     * @param event   The CommandEvent that triggered the Command
     * @param command the command object being run
     */
    @Override
    public void onCommand(CommandEvent event, Command command) {
        if (JMusicBot.COMMAND_AUDIT_ENABLED) {
            Logger logger = LoggerFactory.getLogger("CommandAudit");
            String textFormat = event.isFromType(ChannelType.PRIVATE) ? L.t("command_audit.ran_command") : L.t("command_audit.ran_command_2");

            logger.info(String.format(textFormat,
                    event.isFromType(ChannelType.PRIVATE) ? "DM" : event.getGuild().getName(),
                    event.isFromType(ChannelType.PRIVATE) ? "" : event.getChannel().getName(),
                    event.getAuthor().getName(), event.getAuthor().getDiscriminator(), event.getAuthor().getId(),
                    event.getMessage().getContentDisplay()));
        }

        LastSendTextChannel.SetLastTextId(event);
    }

    @Override
    public void onSlashCommand(dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent event, dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommand command) {
        if (event.getGuild() != null && event.getChannel() != null)
            LastSendTextChannel.SetLastTextId(event.getGuild().getIdLong(), event.getChannel().getIdLong());
    }
}
