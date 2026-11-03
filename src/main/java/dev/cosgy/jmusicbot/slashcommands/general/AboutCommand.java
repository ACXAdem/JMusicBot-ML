/*
 * Copyright 2018-2020 Cosgy Dev
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package dev.cosgy.jmusicbot.slashcommands.general;

import dev.cosgy.jmusicbot.i18n.L;

import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommand;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.commons.JDAUtilitiesInfo;
import dev.cosgy.jmusicbot.framework.jdautilities.doc.standard.CommandInfo;
import dev.cosgy.jmusicbot.framework.jdautilities.examples.doc.Author;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Objects;

/**
 * @author Cosgy Dev
 */
@CommandInfo(
        name = "About",
        description = "Shows information about the bot"
)
@Author("Cosgy Dev")
public class AboutCommand extends SlashCommand {
    private final Color color;
    private final String description;
    private final Permission[] perms;
    private final String[] features;
    private boolean IS_AUTHOR = true;
    private String REPLACEMENT_ICON = "+";
    private String oauthLink;

    public AboutCommand(Color color, String description, String[] features, Permission... perms) {
        this.color = color;
        this.description = description;
        this.features = features;
        this.name = "about";
        this.help = L.t("about.shows_information_about_bot");
        this.aliases = new String[]{"botinfo", "info"};
        //this.guildOnly = false;
        this.perms = perms;
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    public void setIsAuthor(boolean value) {
        this.IS_AUTHOR = value;
    }

    public void setReplacementCharacter(String value) {
        this.REPLACEMENT_ICON = value;
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        if (oauthLink == null) {
            try {
                ApplicationInfo info = event.getJDA().retrieveApplicationInfo().complete();
                oauthLink = info.isBotPublic() ? info.getInviteUrl(0L, perms) : "";
            } catch (Exception e) {
                Logger log = LoggerFactory.getLogger("OAuth2");
                log.error(L.t("about.could_not_create_invite_link"), e);
                oauthLink = "";
            }
        }
        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(event.getGuild() == null ? color : event.getGuild().getSelfMember().getColor());
        builder.setAuthor("" + event.getJDA().getSelfUser().getName() + L.t("about.info"), null, event.getJDA().getSelfUser().getAvatarUrl());
        String CosgyOwner = L.t("about.developed_by_cosgy_dev");
        String author = event.getJDA().getUserById(event.getClient().getOwnerId()) == null ? "<@" + event.getClient().getOwnerId() + ">"
                : Objects.requireNonNull(event.getJDA().getUserById(event.getClient().getOwnerId())).getName();
        StringBuilder descr = new StringBuilder().append(L.t("about.hi_i_m")).append(event.getJDA().getSelfUser().getName()).append("**. ")
                .append(description).append(" — ").append("[" + JDAUtilitiesInfo.AUTHOR + L.t("about.https_github_com_jda_applications") + JDAUtilitiesInfo.GITHUB + ") (")
                .append(JDAUtilitiesInfo.VERSION).append(L.t("about.jda_library_https_github_com"))
                .append(JDAInfo.VERSION).append(L.t("about.text")).append((IS_AUTHOR ? CosgyOwner : author + L.t("about.runs_bot")))
                .append(event.getJDA().getSelfUser().getName()).append(L.t("about.questions_about_can_join_official"))
                .append(L.t("about.learn_how_use_bot_with")).append("/help")
                .append(L.t("about.text_2")).append(L.t("about.features_css"));
        for (String feature : features)
            descr.append("\n").append(event.getClient().getSuccess().startsWith("<") ? REPLACEMENT_ICON : event.getClient().getSuccess()).append(" ").append(feature);
        descr.append(" ```");
        builder.setDescription(descr);

        if (event.getJDA().getShardInfo().getShardTotal() == 1) {
            builder.addField(L.t("about.status"), event.getJDA().getGuilds().size() + L.t("about.servers_1_shard"), true);
            builder.addField(L.t("about.users"), event.getJDA().getUsers().size() + L.t("about.unique") + event.getJDA().getGuilds().stream().mapToInt(g -> g.getMembers().size()).sum() + L.t("about.total"), true);
            builder.addField(L.t("about.channels"), event.getJDA().getTextChannels().size() + L.t("about.text_3") + event.getJDA().getVoiceChannels().size() + L.t("about.voice"), true);
        } else {
            builder.addField(L.t("about.status"), (event.getClient()).getTotalGuilds() + L.t("about.servers_shard") + (event.getJDA().getShardInfo().getShardId() + 1)
                    + "/" + event.getJDA().getShardInfo().getShardTotal(), true);
            builder.addField("", event.getJDA().getUsers().size() + L.t("about.users_shard") + event.getJDA().getGuilds().size() + L.t("about.servers"), true);
            builder.addField("", event.getJDA().getTextChannels().size() + L.t("about.text_channels") + event.getJDA().getVoiceChannels().size() + L.t("about.voice_channels"), true);
        }
        builder.setFooter(L.t("about.last_restart"), "https://www.cosgy.dev/wp-content/uploads/2020/03/restart.jpg");
        builder.setTimestamp(event.getClient().getStartTime());
        event.replyEmbeds(builder.build()).queue();
    }

    @Override
    protected void execute(CommandEvent event) {
        if (oauthLink == null) {
            try {
                ApplicationInfo info = event.getJDA().retrieveApplicationInfo().complete();
                oauthLink = info.isBotPublic() ? info.getInviteUrl(0L, perms) : "";
            } catch (Exception e) {
                Logger log = LoggerFactory.getLogger("OAuth2");
                log.error(L.t("about.could_not_create_invite_link"), e);
                oauthLink = "";
            }
        }
        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(event.isFromType(ChannelType.TEXT) ? event.getGuild().getSelfMember().getColor() : color);
        builder.setAuthor("" + event.getSelfUser().getName() + L.t("about.text_4"), null, event.getSelfUser().getAvatarUrl());
        String CosgyOwner = L.t("about.developed_by_cosgy_dev");
        String author = event.getJDA().getUserById(event.getClient().getOwnerId()) == null ? "<@" + event.getClient().getOwnerId() + ">"
                : Objects.requireNonNull(event.getJDA().getUserById(event.getClient().getOwnerId())).getName();
        StringBuilder descr = new StringBuilder().append(L.t("about.hi_i_m")).append(event.getSelfUser().getName()).append("**. ")
                .append(description).append(" — ").append(JDAUtilitiesInfo.AUTHOR + L.t("about.s_commands_extension") + JDAUtilitiesInfo.GITHUB + ") (")
                .append(JDAUtilitiesInfo.VERSION).append(L.t("about.jda_library_https_github_com"))
                .append(JDAInfo.VERSION).append(L.t("about.text")).append((IS_AUTHOR ? CosgyOwner : author + L.t("about.runs_bot")))
                .append(event.getSelfUser().getName()).append(L.t("about.questions_about_can_join_official"))
                .append(L.t("about.learn_how_use_bot_with")).append(event.getClient().getTextualPrefix()).append(event.getClient().getHelpWord())
                .append(L.t("about.text_2")).append(L.t("about.features_css"));
        for (String feature : features)
            descr.append("\n").append(event.getClient().getSuccess().startsWith("<") ? REPLACEMENT_ICON : event.getClient().getSuccess()).append(" ").append(feature);
        descr.append(" ```");
        builder.setDescription(descr);

        if (event.getJDA().getShardInfo().getShardTotal() == 1) {
            builder.addField(L.t("about.status"), event.getJDA().getGuilds().size() + L.t("about.servers_1_shard"), true);
            builder.addField(L.t("about.users"), event.getJDA().getUsers().size() + L.t("about.unique") + event.getJDA().getGuilds().stream().mapToInt(g -> g.getMembers().size()).sum() + L.t("about.total"), true);
            builder.addField(L.t("about.channels"), event.getJDA().getTextChannels().size() + L.t("about.text_3") + event.getJDA().getVoiceChannels().size() + L.t("about.voice"), true);
        } else {
            builder.addField(L.t("about.status"), (event.getClient()).getTotalGuilds() + L.t("about.servers_shard") + (event.getJDA().getShardInfo().getShardId() + 1)
                    + "/" + event.getJDA().getShardInfo().getShardTotal(), true);
            builder.addField("", event.getJDA().getUsers().size() + L.t("about.users_shard") + event.getJDA().getGuilds().size() + L.t("about.servers"), true);
            builder.addField("", event.getJDA().getTextChannels().size() + L.t("about.text_channels") + event.getJDA().getVoiceChannels().size() + L.t("about.voice_channels"), true);
        }
        builder.setFooter(L.t("about.last_restart"), "https://www.cosgy.dev/wp-content/uploads/2020/03/restart.jpg");
        builder.setTimestamp(event.getClient().getStartTime());
        event.reply(builder.build());
    }

}
