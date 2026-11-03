/*
 *  Copyright 2021 Cosgy Dev (info@cosgy.dev).
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

package dev.cosgy.jmusicbot.slashcommands.general;

import dev.cosgy.jmusicbot.i18n.L;

import com.jagrosh.jmusicbot.Bot;
import dev.cosgy.jmusicbot.framework.jdautilities.command.Command;
import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandClient;
import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommand;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import dev.cosgy.jmusicbot.util.TurkishCommandNames;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows the command list. In Turkish mode the English and Turkish names are listed together.
 * The list is long, so it is split into several embeds (Discord limits).
 */
public class HelpCmd extends SlashCommand {
    private static final int PAGE_LIMIT = 3800;
    public Bot bot;

    public HelpCmd(Bot bot) {
        this.bot = bot;
        this.name = "help";
        this.help = L.t("help.shows_command_list");
        this.aliases = bot.getConfig().getAliases(this.name);
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        List<MessageEmbed> pages = buildPages(event.getClient(), event.isOwner(), event.getJDA().getSelfUser().getName());
        event.replyEmbeds(pages.get(0)).setEphemeral(true).queue(hook -> {
            for (int i = 1; i < pages.size(); i++) {
                hook.sendMessageEmbeds(pages.get(i)).setEphemeral(true).queue();
            }
        });
    }

    @Override
    public void execute(CommandEvent event) {
        List<MessageEmbed> pages = buildPages(event.getClient(), event.isOwner(), event.getSelfUser().getName());
        if (bot.getConfig().getHelpToDm()) {
            event.getAuthor().openPrivateChannel().queue(dm -> {
                for (MessageEmbed page : pages) dm.sendMessageEmbeds(page).queue();
                if (event.isFromType(ChannelType.TEXT)) event.reactSuccess();
            }, t -> event.replyWarning(L.t("help.could_not_send_help_because")));
        } else {
            for (MessageEmbed page : pages) event.reply(page);
        }
    }

    private List<MessageEmbed> buildPages(CommandClient client, boolean isBotOwner, String botName) {
        String prefix = client.getTextualPrefix();
        // Group by category (keep the order of first appearance)
        java.util.Map<String, StringBuilder> groups = new java.util.LinkedHashMap<>();

        for (Command command : client.getCommands()) {
            if (command.isHidden() || (command.isOwnerCommand() && !isBotOwner)) continue;
            String catName = command.getCategory() == null ? L.t("cooldown_scope.general") : command.getCategory().getName();
            StringBuilder cur = groups.computeIfAbsent(catName, k -> new StringBuilder("\n__**").append(k).append("**__\n"));

            // Turkish names are only listed when the bot runs in Turkish
            String trSlash = L.isTurkish() ? TurkishCommandNames.slashName(command.getName()) : null;
            String[] trAll = L.isTurkish() ? TurkishCommandNames.aliases(command.getName()) : new String[0];
            boolean isSlash = command instanceof SlashCommand && !command.isOwnerCommand();

            StringBuilder line = new StringBuilder();
            if (isSlash) {
                line.append("`/").append(command.getName()).append("`");
                if (trSlash != null) line.append(" `/").append(trSlash).append("`");
            } else {
                line.append("`").append(prefix).append(command.getName()).append("`");
                if (trAll.length > 0) line.append(" `").append(prefix).append(trAll[0]).append("`");
            }
            if (command.getArguments() != null && !command.getArguments().isBlank()) {
                line.append(" ").append(command.getArguments());
            }
            line.append(" — ").append(command.getHelp()).append("\n");
            cur.append(line);
        }
        List<String> blocks = new ArrayList<>();
        for (StringBuilder sb : groups.values()) blocks.add(sb.toString());

        String intro = L.t("help.can_use_commands_with_with") + prefix + L.t("help.prefix")
                + (L.isTurkish() ? L.t("help.commands_also_work_with_their") + prefix + L.t("help.al") : "\n")
                + L.t("help.can_play_spotify_links_directly");
        if (client.getServerInvite() != null) {
            intro += L.t("help.more_help") + client.getServerInvite() + "\n";
        }

        List<MessageEmbed> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder(intro);
        for (String block : blocks) {
            if (page.length() + block.length() > PAGE_LIMIT && page.length() > 0) {
                pages.add(embed(botName, page.toString(), pages.isEmpty()));
                page = new StringBuilder();
            }
            if (block.length() > PAGE_LIMIT) block = block.substring(0, PAGE_LIMIT - 1) + "…";
            page.append(block);
        }
        if (page.length() > 0) pages.add(embed(botName, page.toString(), pages.isEmpty()));
        return pages;
    }

    private static MessageEmbed embed(String botName, String text, boolean firstPage) {
        EmbedBuilder eb = new EmbedBuilder().setColor(new Color(88, 101, 242)).setDescription(text);
        if (firstPage) eb.setTitle(botName + L.t("help.command_list"));
        return eb.build();
    }
}
