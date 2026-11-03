package dev.cosgy.jmusicbot.framework.jdautilities.command;

import dev.cosgy.jmusicbot.i18n.L;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import dev.cosgy.jmusicbot.util.TurkishCommandNames;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommandClient extends ListenerAdapter {
    private final String prefix;
    private final String altPrefix;
    private final String ownerId;
    private final String success;
    private final String warning;
    private final String error;
    private final int linkedCacheSize;
    private final GuildSettingsManager guildSettingsManager;
    private final CommandListener listener;
    private final String serverInvite;
    private final OnlineStatus status;
    private final Activity activity;
    private final List<Command> commands;
    private final List<SlashCommand> slashCommands;
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private final List<String> coOwnerIds = new ArrayList<>();
    private final OffsetDateTime startTime = OffsetDateTime.now();
    private JDA jda;

    CommandClient(CommandClientBuilder builder) {
        this.prefix = builder.prefix;
        this.altPrefix = builder.altPrefix;
        this.ownerId = builder.ownerId;
        this.success = builder.success;
        this.warning = builder.warning;
        this.error = builder.error;
        this.linkedCacheSize = builder.linkedCacheSize;
        this.guildSettingsManager = builder.guildSettingsManager;
        this.listener = builder.listener;
        this.serverInvite = builder.serverInvite;
        this.status = builder.status;
        this.activity = builder.activity;
        this.commands = List.copyOf(builder.commands);
        this.slashCommands = List.copyOf(builder.slashCommands);
    }

    @Override
    public void onReady(ReadyEvent event) {
        this.jda = event.getJDA();

        // Move from the starting state to the final state
        OnlineStatus targetStatus = status == null ? OnlineStatus.ONLINE : status;
        if (targetStatus == OnlineStatus.UNKNOWN) {
            targetStatus = OnlineStatus.ONLINE;
        }
        event.getJDA().getPresence().setPresence(targetStatus, activity, false);

        // Slash commands are registered internally.
        if (!slashCommands.isEmpty()) {
            var update = event.getJDA().updateCommands();
            int count = 0;
            for (SlashCommand command : slashCommands) {
                update = update.addCommands(command.buildCommandData());
                count++;
                // Also register the same command under its Turkish name (e.g. /play and /oynat)
                // Turkish mode: also register each command under its Turkish name (e.g. /play and /oynat).
                // Other languages get the Turkish name as a Discord name localization instead.
                String trName = L.isTurkish() ? TurkishCommandNames.slashName(command.getName()) : null;
                if (trName != null && !trName.equalsIgnoreCase(command.getName())) {
                    CommandData trData = command.buildCommandData();
                    if (trData instanceof SlashCommandData) {
                        ((SlashCommandData) trData).setName(trName);
                        update = update.addCommands(trData);
                        count++;
                    }
                }
            }
            final int total = count;
            update.queue(
                    ok -> LoggerFactory.getLogger("CommandClient").info(L.t("command_client.registered_slash_commands_with_discord"), total),
                    err -> LoggerFactory.getLogger("CommandClient").error(L.t("command_client.could_not_register_slash_commands"), err.getMessage()));
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String raw = event.getMessage().getContentRaw();
        String matchedPrefix = null;
        if (raw.startsWith(prefix)) {
            matchedPrefix = prefix;
        } else if (altPrefix != null && !altPrefix.isEmpty() && raw.startsWith(altPrefix)) {
            matchedPrefix = altPrefix;
        }
        if (matchedPrefix == null) return;

        String commandLine = raw.substring(matchedPrefix.length()).trim();
        if (commandLine.isEmpty()) return;

        String[] parts = commandLine.split("\\s+", 2);
        String invoke = parts[0].toLowerCase(Locale.ROOT);
        String args = parts.length > 1 ? parts[1] : "";

        for (Command command : commands) {
            if (command.isCommandFor(invoke)) {
                CommandEvent commandEvent = new CommandEvent(this, event.getMessage(), args);
                if (listener != null) listener.onCommand(commandEvent, command);
                command.run(commandEvent);
                return;
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String rootName = event.getName();
        SlashCommand root = null;
        for (SlashCommand command : slashCommands) {
            if (command.getName().equalsIgnoreCase(rootName) || rootName.equalsIgnoreCase(TurkishCommandNames.slashName(command.getName()))) {
                root = command;
                break;
            }
        }
        if (root == null) return;

        SlashCommand target = root;
        if (event.getSubcommandName() != null) {
            for (SlashCommand child : root.getChildren()) {
                if (!child.getName().equalsIgnoreCase(event.getSubcommandName())) continue;
                if (event.getSubcommandGroup() != null && child.getSubcommandGroup() != null
                        && !event.getSubcommandGroup().equalsIgnoreCase(child.getSubcommandGroup().getName())) {
                    continue;
                }
                target = child;
                break;
            }
        }

        SlashCommandEvent wrapped = new SlashCommandEvent(this, event);
        if (listener != null) listener.onSlashCommand(wrapped, target);
        target.run(wrapped);
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        for (SlashCommand command : slashCommands) {
            if (command.getName().equalsIgnoreCase(event.getName()) || event.getName().equalsIgnoreCase(TurkishCommandNames.slashName(command.getName()))) {
                command.onAutoComplete(event);
                return;
            }
        }
    }

    public String getOwnerId() {
        return ownerId;
    }

    public List<String> getCoOwnerIds() {
        return Collections.unmodifiableList(coOwnerIds);
    }

    public String getSuccess() {
        return success;
    }

    public String getWarning() {
        return warning;
    }

    public String getError() {
        return error;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getTextualPrefix() {
        return prefix;
    }

    public String getHelpWord() {
        return "help";
    }

    public int getLinkedCacheSize() {
        return linkedCacheSize;
    }

    public int getRemainingCooldown(String key) {
        Long expiresAt = cooldowns.get(key);
        if (expiresAt == null) return 0;
        long remaining = (expiresAt - System.currentTimeMillis()) / 1000L;
        if (remaining <= 0) {
            cooldowns.remove(key);
            return 0;
        }
        return (int) remaining;
    }

    public void applyCooldown(String key, int seconds) {
        cooldowns.put(key, System.currentTimeMillis() + (seconds * 1000L));
    }

    public CommandListener getListener() {
        return listener;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public int getTotalGuilds() {
        return jda == null ? 0 : jda.getGuilds().size();
    }

    @SuppressWarnings("unchecked")
    public <T extends GuildSettingsProvider> T getSettingsFor(Guild guild) {
        if (guildSettingsManager == null || guild == null) return null;
        return (T) guildSettingsManager.getSettings(guild);
    }

    public String getServerInvite() {
        return serverInvite;
    }

    public Activity getActivity() {
        return activity;
    }

    public List<Command> getCommands() {
        return commands;
    }
}
