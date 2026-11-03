package dev.cosgy.jmusicbot.slashcommands.general;

import dev.cosgy.jmusicbot.i18n.L;

import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommand;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ServerInfo extends SlashCommand {
    public ServerInfo(Bot bot) {
        this.name = "serverinfo";
        this.help = L.t("server_info.shows_information_about_server");
        //this.guildOnly = true;
        this.category = new Category(L.t("cooldown_scope.general"));
        this.aliases = bot.getConfig().getAliases(this.name);
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        String GuildName = event.getGuild().getName();
        String GuildIconURL = event.getGuild().getIconUrl();
        String GuildId = event.getGuild().getId();
        String GuildOwner = Objects.requireNonNull(event.getGuild().getOwner()).getUser().getName() + "#" + event.getGuild().getOwner().getUser().getDiscriminator();
        String GuildCreatedDate = event.getGuild().getTimeCreated().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));

        String GuildRolesCount = String.valueOf(event.getGuild().getRoles().size());
        String GuildMember = String.valueOf(event.getGuild().getMembers().size());
        String GuildCategoryCount = String.valueOf(event.getGuild().getCategories().size());
        String GuildTextChannelCount = String.valueOf(event.getGuild().getTextChannels().size());
        String GuildVoiceChannelCount = String.valueOf(event.getGuild().getVoiceChannels().size());
        String GuildStageChannelCount = String.valueOf(event.getGuild().getStageChannels().size());
        String GuildForumChannelCount = String.valueOf(event.getGuild().getForumChannels().size());
        String GuildLocation = event.getGuild().getLocale().getNativeName();
                /*
                .replace("japan", L.t("server_info.flag_jp_japan"))
                .replace("singapore", L.t("server_info.flag_sg_singapore"))
                .replace("hongkong", ":flag_hk: Hong Kong")
                .replace("Brazil", L.t("server_info.flag_br_brazil"))
                .replace("us-central", L.t("server_info.flag_us_us_central"))
                .replace("us-west", L.t("server_info.flag_us_us_west"))
                .replace("us-east", L.t("server_info.flag_us_us_east"))
                .replace("us-south", L.t("server_info.flag_us_us_south"))
                .replace("sydney", L.t("server_info.flag_au_sydney"))
                .replace("eu-west", L.t("server_info.flag_eu_western_europe"))
                .replace("eu-central", L.t("server_info.flag_eu_central_europe"))
                .replace("russia", L.t("server_info.flag_ru_russia"));
                 */

        EmbedBuilder eb = new EmbedBuilder();

        eb.setAuthor(L.t("server_info.server_info") + GuildName + "", null, GuildIconURL);

        eb.addField(L.t("server_info.server_id"), GuildId, true);
        eb.addField(L.t("server_info.server_s_primary_language"), GuildLocation, true);
        eb.addField(L.t("server_info.server_owner"), GuildOwner, true);
        eb.addField(L.t("server_info.members"), GuildMember, true);
        eb.addField(L.t("server_info.roles"), GuildRolesCount, true);
        eb.addField(L.t("server_info.categories"), GuildCategoryCount, true);
        eb.addField(L.t("server_info.text_channels"), GuildTextChannelCount, true);
        eb.addField(L.t("server_info.voice_channels"), GuildVoiceChannelCount, true);
        eb.addField(L.t("server_info.stage_channels"), GuildStageChannelCount, true);
        eb.addField(L.t("server_info.forum_channels"), GuildForumChannelCount, true);

        eb.setFooter(L.t("server_info.server_created") + GuildCreatedDate, null);

        event.replyEmbeds(eb.build()).queue();
    }

    @Override
    public void execute(CommandEvent event) {
        String GuildName = event.getGuild().getName();
        String GuildIconURL = event.getGuild().getIconUrl();
        String GuildId = event.getGuild().getId();
        String GuildOwner = Objects.requireNonNull(event.getGuild().getOwner()).getUser().getName() + "#" + event.getGuild().getOwner().getUser().getDiscriminator();
        String GuildCreatedDate = event.getGuild().getTimeCreated().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));

        String GuildRolesCount = String.valueOf(event.getGuild().getRoles().size());
        String GuildMember = String.valueOf(event.getGuild().getMembers().size());
        String GuildCategoryCount = String.valueOf(event.getGuild().getCategories().size());
        String GuildTextChannelCount = String.valueOf(event.getGuild().getTextChannels().size());
        String GuildVoiceChannelCount = String.valueOf(event.getGuild().getVoiceChannels().size());
        String GuildStageChannelCount = String.valueOf(event.getGuild().getStageChannels().size());
        String GuildForumChannelCount = String.valueOf(event.getGuild().getForumChannels().size());
        String GuildLocation = event.getGuild().getLocale().getNativeName();
                /*.replace("japan", ":flag_jp: Japonya")
                .replace("singapore", L.t("server_info.flag_sg_singapore"))
                .replace("hongkong", ":flag_hk: Hong Kong")
                .replace("Brazil", L.t("server_info.flag_br_brazil"))
                .replace("us-central", L.t("server_info.flag_us_us_central"))
                .replace("us-west", L.t("server_info.flag_us_us_west"))
                .replace("us-east", L.t("server_info.flag_us_us_east"))
                .replace("us-south", L.t("server_info.flag_us_us_south"))
                .replace("sydney", L.t("server_info.flag_au_sydney"))
                .replace("eu-west", L.t("server_info.flag_eu_western_europe"))
                .replace("eu-central", L.t("server_info.flag_eu_central_europe"))
                .replace("russia", L.t("server_info.flag_ru_russia"));*/


        EmbedBuilder eb = new EmbedBuilder();

        eb.setAuthor(L.t("server_info.server_info") + GuildName + "", null, GuildIconURL);

        eb.addField(L.t("server_info.server_id"), GuildId, true);
        eb.addField(L.t("server_info.server_s_primary_language"), GuildLocation, true);
        eb.addField(L.t("server_info.server_owner"), GuildOwner, true);
        eb.addField(L.t("server_info.members"), GuildMember, true);
        eb.addField(L.t("server_info.roles"), GuildRolesCount, true);
        eb.addField(L.t("server_info.categories"), GuildCategoryCount, true);
        eb.addField(L.t("server_info.text_channels"), GuildTextChannelCount, true);
        eb.addField(L.t("server_info.voice_channels"), GuildVoiceChannelCount, true);
        eb.addField(L.t("server_info.stage_channels"), GuildStageChannelCount, true);
        eb.addField(L.t("server_info.forum_channels"), GuildForumChannelCount, true);

        eb.setFooter(L.t("server_info.server_created") + GuildCreatedDate, null);

        event.getChannel().sendMessageEmbeds(eb.build()).queue();
    }
}
