/*
 * Copyright 2016-2018 John Grosh (jagrosh) & Kaidan Gustave (TheMonitorLizard)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.cosgy.jmusicbot.framework.jdautilities.command;

import dev.cosgy.jmusicbot.i18n.L;

import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import dev.cosgy.jmusicbot.util.TurkishCommandNames;


/**
 * Common base class for running slash commands.
 * Responsible for permission checks, cooldowns and building the command data.
 */
public abstract class SlashCommand extends Command
{
    
    protected Map<DiscordLocale, String> nameLocalization = new HashMap<>();

    
    protected Map<DiscordLocale, String> descriptionLocalization = new HashMap<>();

    
    @Deprecated
    protected String requiredRole = null;

    
    protected SlashCommand[] children = new SlashCommand[0];

    
    protected SubcommandGroupData subcommandGroup = null;

    
    protected List<OptionData> options = new ArrayList<>();

    
    protected CommandClient client;

    
    protected abstract void execute(SlashCommandEvent event);

    
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {}

    
    @Override
    protected void execute(CommandEvent event) {}

    
    public final void run(SlashCommandEvent event)
    {
        // Set the client reference
        this.client = event.getClient();

        // Owner permission check
        if(ownerCommand && !(isOwner(event, client)))
        {
            terminate(event, L.t("slash.only_bot_owner_can_use"), client);
            return;
        }

        // Check whether the channel can be used
        try {
            if(event.getTextChannel() != null && !isAllowed(event.getTextChannel()))
            {
                terminate(event, L.t("slash.command_cannot_used_channel"), client);
                return;
            }
        } catch (Exception e) {
            // Channels other than text channels are not checked
        }

        // Required role check
        if(requiredRole!=null)
            if(!(event.getChannelType() == ChannelType.TEXT) || event.getMember().getRoles().stream().noneMatch(r -> r.getName().equalsIgnoreCase(requiredRole)))
            {
                terminate(event, client.getError()+L.t("slash.must_have")+requiredRole+L.t("slash.role_use_command"), client);
                return;
            }

        // Runnability check
        if(event.getChannelType() != ChannelType.PRIVATE)
        {
            // User permission check
            for(Permission p: userPermissions)
            {
                // Normally not null when run inside a server
                if(event.getMember() == null)
                    continue;

                if(p.isChannel())
                {
                    if(!event.getMember().hasPermission(event.getGuildChannel(), p))
                    {
                        terminate(event, String.format(userMissingPermMessage, client.getError(), p.getName(), L.t("slash.channel")), client);
                        return;
                    }
                }
                else
                {
                    if(!event.getMember().hasPermission(p))
                    {
                        terminate(event, String.format(userMissingPermMessage, client.getError(), p.getName(), L.t("slash.server")), client);
                        return;
                    }
                }
            }

            // Bot permission check
            for (Permission p : botPermissions) {
                // Exclude permissions not required to run
                if (p == Permission.VIEW_CHANNEL || p == Permission.MESSAGE_EMBED_LINKS) {
                    continue;
                }

                // Get the server and bot member info
                Member selfMember = event.getGuild() != null ? event.getGuild().getSelfMember() : null;

                if (p.isChannel()) {
                    // Check channel-level permissions
                    GuildVoiceState voiceState = event.getMember().getVoiceState();
                    AudioChannelUnion channel = voiceState != null ? voiceState.getChannel() : null;

                    if (channel == null || !channel.getType().isAudio()) {
                        terminate(event, client.getError() + L.t("slash.join_voice_channel_use_command"), client);
                        return;
                    }

                    // Check the bot's permissions in the voice channel
                    if (!selfMember.hasPermission(channel, p)) {
                        terminate(event, String.format(botMissingPermMessage, client.getError(), p.getName(), L.t("slash.voice_channel")), client);
                        return;
                    }
                } else {
                    // Check server-wide permissions
                    if (!selfMember.hasPermission(p)) {
                        terminate(event, String.format(botMissingPermMessage, client.getError(), p.getName(), L.t("slash.server")), client);
                        return;
                    }
                }
            }

            // NSFW check
            if (nsfwOnly && event.getChannelType() == ChannelType.TEXT && !event.getTextChannel().isNSFW())
            {
                terminate(event, L.t("slash.command_can_only_used_nsfw"), client);
                return;
            }
        }
        else if(guildOnly)
        {
            terminate(event, client.getError()+L.t("slash.command_cannot_used_direct_messages"), client);
            return;
        }

        // Cooldown check (except for the owner)
        if(cooldown>0 && !(isOwner(event, client)))
        {
            String key = getCooldownKey(event);
            int remaining = client.getRemainingCooldown(key);
            if(remaining>0)
            {
                terminate(event, getCooldownError(event, remaining, client), client);
                return;
            }
            else client.applyCooldown(key, cooldown);
        }

        // Run the actual work
        try {
            execute(event);
        } catch(Throwable t) {
            if(client.getListener() != null)
            {
                client.getListener().onSlashCommandException(event, this, t);
                return;
            }
            // Rethrow the error if no listener is set
            throw t;
        }

        if(client.getListener() != null)
            client.getListener().onCompletedSlashCommand(event, this);
    }

    
    public boolean isOwner(SlashCommandEvent event, CommandClient client)
    {
        if(event.getUser().getId().equals(client.getOwnerId()))
            return true;
        if(client.getCoOwnerIds()==null)
            return false;
        for(String id : client.getCoOwnerIds())
            if(id.equals(event.getUser().getId()))
                return true;
        return false;
    }

    
    @Deprecated
    @ForRemoval(deadline = "2.0.0")
    public CommandClient getClient()
    {
        return client;
    }

    
    public SubcommandGroupData getSubcommandGroup()
    {
        return subcommandGroup;
    }

    
    public List<OptionData> getOptions()
    {
        return options;
    }

    
    public CommandData buildCommandData()
    {
        // Build the command definition
        SlashCommandData data = Commands.slash(getName(), getHelp());
        if (!getOptions().isEmpty())
        {
            localizeOptions(getOptions());
            data.addOptions(getOptions());
        }
        // Descriptions in every supported language: each user sees them in their Discord language
        L.localizations(getHelp(), 100).forEach(data::setDescriptionLocalization);
        // Turkish command name (/oynat for /play) for Turkish Discord clients.
        // In Turkish mode the Turkish names are registered as separate commands instead (see CommandClient).
        if (!L.isTurkish())
        {
            String trName = TurkishCommandNames.slashName(getName());
            if (trName != null && !trName.equalsIgnoreCase(getName()))
            {
                try { data.setNameLocalization(DiscordLocale.TURKISH, trName); } catch (IllegalArgumentException ignored) { }
            }
        }

        // Apply name localizations
        if (!getNameLocalization().isEmpty())
        {
            // Localization setting
            data.setNameLocalizations(getNameLocalization());
        }
        // Apply description localizations
        if (!getDescriptionLocalization().isEmpty())
        {
            // Localization setting
            data.setDescriptionLocalizations(getDescriptionLocalization());
        }

        // Apply subcommands
        if (children.length != 0)
        {
            // Map to collect subcommand groups
            Map<String, SubcommandGroupData> groupData = new HashMap<>();
            for (SlashCommand child : children)
            {
                // Build the subcommand definition
                SubcommandData subcommandData = new SubcommandData(child.getName(), child.getHelp());
                L.localizations(child.getHelp(), 100).forEach(subcommandData::setDescriptionLocalization);
                // Apply options
                if (!child.getOptions().isEmpty())
                {
                    localizeOptions(child.getOptions());
                    subcommandData.addOptions(child.getOptions());
                }
                // Subcommand name for Turkish Discord users
                String trSub = TurkishCommandNames.SUBCOMMAND_NAMES.get(child.getName());
                if (trSub != null && child.getNameLocalization().isEmpty())
                {
                    try { subcommandData.setNameLocalization(DiscordLocale.TURKISH, trSub); } catch (IllegalArgumentException ignored) { }
                }

                // Apply subcommand name localizations
                if (!child.getNameLocalization().isEmpty())
                {
                    // Localization setting
                    subcommandData.setNameLocalizations(child.getNameLocalization());
                }
                // Apply subcommand description localizations
                if (!child.getDescriptionLocalization().isEmpty())
                {
                    // Localization setting
                    subcommandData.setDescriptionLocalizations(child.getDescriptionLocalization());
                }

                // If there is a subcommand group
                if (child.getSubcommandGroup() != null)
                {
                    SubcommandGroupData group = child.getSubcommandGroup();

                    SubcommandGroupData newData = groupData.getOrDefault(group.getName(), group)
                            .addSubcommands(subcommandData);

                    groupData.put(group.getName(), newData);
                }
                // Add directly if no group is given
                else
                {
                    data.addSubcommands(subcommandData);
                }
            }
            if (!groupData.isEmpty())
                data.addSubcommandGroups(groupData.values());
        }

        if (this.getUserPermissions() == null)
            data.setDefaultPermissions(DefaultMemberPermissions.DISABLED);
        else
            data.setDefaultPermissions(DefaultMemberPermissions.enabledFor(this.getUserPermissions()));

        //data.setGuildOnly(this.guildOnly);

        return data;
    }

    
    /** Translates option names for Turkish Discord users (the internal name doesn't change). */
    private static void localizeOptions(List<OptionData> options)
    {
        for (OptionData o : options)
        {
            L.localizations(o.getDescription(), 100).forEach(o::setDescriptionLocalization);
            String tr = TurkishCommandNames.OPTION_NAMES.get(o.getName());
            if (tr != null && !tr.equals(o.getName()))
            {
                try { o.setNameLocalization(DiscordLocale.TURKISH, tr); } catch (IllegalArgumentException ignored) { }
            }
        }
    }

    public SlashCommand[] getChildren()
    {
        return children;
    }

    private void terminate(SlashCommandEvent event, String message, CommandClient client)
    {
        if(message!=null)
            event.reply(message).setEphemeral(true).queue();
        if(client.getListener()!=null)
            client.getListener().onTerminatedSlashCommand(event, this);
    }

    
    public String getCooldownKey(SlashCommandEvent event)
    {
        switch (cooldownScope)
        {
            case USER:         return cooldownScope.genKey(name,event.getUser().getIdLong());
            case USER_GUILD:   return event.getGuild()!=null ? cooldownScope.genKey(name,event.getUser().getIdLong(),event.getGuild().getIdLong()) :
                    CooldownScope.USER_CHANNEL.genKey(name,event.getUser().getIdLong(), event.getChannel().getIdLong());
            case USER_CHANNEL: return cooldownScope.genKey(name,event.getUser().getIdLong(),event.getChannel().getIdLong());
            case GUILD:        return event.getGuild()!=null ? cooldownScope.genKey(name,event.getGuild().getIdLong()) :
                    CooldownScope.CHANNEL.genKey(name,event.getChannel().getIdLong());
            case CHANNEL:      return cooldownScope.genKey(name,event.getChannel().getIdLong());
            case SHARD:
                event.getJDA().getShardInfo();
                return cooldownScope.genKey(name, event.getJDA().getShardInfo().getShardId());
            case USER_SHARD:
                event.getJDA().getShardInfo();
                return cooldownScope.genKey(name,event.getUser().getIdLong(),event.getJDA().getShardInfo().getShardId());
            case GLOBAL:       return cooldownScope.genKey(name, 0);
            default:           return "";
        }
    }

    
    public String getCooldownError(SlashCommandEvent event, int remaining, CommandClient client)
    {
        if(remaining<=0)
            return null;
        String front = client.getWarning()+L.t("slash.must_wait")+remaining+L.t("slash.seconds_before_using_command_again");
        if(cooldownScope.equals(CooldownScope.USER))
            return front+"!";
        else if(cooldownScope.equals(CooldownScope.USER_GUILD) && event.getGuild()==null)
            return front+" "+ CooldownScope.USER_CHANNEL.errorSpecification+"!";
        else if(cooldownScope.equals(CooldownScope.GUILD) && event.getGuild()==null)
            return front+" "+ CooldownScope.CHANNEL.errorSpecification+"!";
        else
            return front+" "+cooldownScope.errorSpecification+"!";
    }

    
    public Map<DiscordLocale, String> getNameLocalization() {
        return nameLocalization;
    }

    
    public Map<DiscordLocale, String> getDescriptionLocalization() {
        return descriptionLocalization;
    }
}
