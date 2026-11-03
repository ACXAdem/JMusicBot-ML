package dev.cosgy.jmusicbot.slashcommands.general;

import dev.cosgy.jmusicbot.i18n.L;

import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommand;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.commons.utils.FinderUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UserInfo extends SlashCommand {
    Logger log = LoggerFactory.getLogger("UserInfo");

    public UserInfo() {
        this.name = "userinfo";
        this.help = L.t("user_info.shows_information_about_specified_user");
        this.arguments = L.t("user_info.user");
        //this.guildOnly = true;
        this.category = new Category(L.t("cooldown_scope.general"));

        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.USER, "user", L.t("user_info.user_2"), true));
        this.options = options;

    }

    @Override
    protected void execute(SlashCommandEvent event) {
        Member memb = event.getOption("user").getAsMember();

        EmbedBuilder eb = new EmbedBuilder().setColor(memb.getColor());
        String NAME = memb.getEffectiveName();
        String TAG = "#" + memb.getUser().getDiscriminator();
        String GUILD_JOIN_DATE = memb.getTimeJoined().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        String DISCORD_JOINED_DATE = memb.getUser().getTimeCreated().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        String ID = memb.getUser().getId();
        String STATUS = memb.getOnlineStatus().getKey().replace("offline", L.t("user_info.x_offline")).replace("dnd", L.t("user_info.red_circle_do_not_disturb")).replace("idle", L.t("user_info.idle")).replace("online", L.t("user_info.white_check_mark_online"));
        String ROLES;
        String GAME;
        String AVATAR = memb.getUser().getAvatarUrl();

        log.debug(L.t("user_info.username") + memb.getEffectiveName() + "\n" +
                L.t("user_info.tag") + memb.getUser().getDiscriminator() + "\n" +
                L.t("user_info.joined_server")
                + memb.getUser().getTimeCreated()
                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")) + "\n" +
                L.t("user_info.user_id") + memb.getUser().getId() + "\n" +
                L.t("user_info.online_status") + memb.getOnlineStatus());

        try {
            GAME = memb.getActivities().toString();
        } catch (Exception e) {
            GAME = "-/-";
        }

        StringBuilder ROLESBuilder = new StringBuilder();
        for (Role r : memb.getRoles()) {
            ROLESBuilder.append(r.getName()).append(", ");
        }
        ROLES = ROLESBuilder.toString();
        if (ROLES.length() > 0)
            ROLES = ROLES.substring(0, ROLES.length() - 2);
        else
            ROLES = L.t("user_info.no_roles_server");

        if (AVATAR == null) {
            AVATAR = L.t("user_info.no_avatar");
        }

        eb.setAuthor(memb.getUser().getName() + TAG + L.t("user_info.user_info"), null, null)
                .addField(L.t("user_info.pencil2_name_nickname"), "**" + NAME + "**", true)
                .addField(":link: DiscordTag", "**" + TAG + "**", true)
                .addField(":1234: ID", "**" + ID + "**", true)
                .addBlankField(false)
                .addField(L.t("user_info.signal_strength_current_status"), "**" + STATUS + "**", true)
                .addField(L.t("user_info.video_game_playing"), "**" + GAME + "**", true)
                .addField(L.t("user_info.tools_roles"), "**" + ROLES + "**", true)
                .addBlankField(false)
                .addField(L.t("user_info.inbox_tray_joined_server"), "**" + GUILD_JOIN_DATE + "**", true)
                .addField(L.t("user_info.beginner_account_created"), "**" + DISCORD_JOINED_DATE + "**", true)
                .addBlankField(false)
                .addField(L.t("user_info.frame_photo_avatar_url"), AVATAR, false);

        if (!AVATAR.equals(L.t("user_info.no_avatar"))) {
            eb.setAuthor(memb.getUser().getName() + TAG + L.t("user_info.user_info"), null, AVATAR);
        }

        event.replyEmbeds(eb.build()).queue();
    }

    @Override
    public void execute(CommandEvent event) {
        Member memb;

        if (event.getArgs().length() > 0) {
            try {

                if (event.getMessage().getReferencedMessage().getMentions().getMembers().size() != 0) {
                    memb = event.getMessage().getReferencedMessage().getMentions().getMembers().get(0);
                } else {
                    memb = FinderUtil.findMembers(event.getArgs(), event.getGuild()).get(0);
                }
            } catch (Exception e) {
                event.reply(L.t("user_info.user_3") + event.getArgs() + L.t("user_info.not_found"));
                return;
            }
        } else {
            memb = event.getMember();
        }

        EmbedBuilder eb = new EmbedBuilder().setColor(memb.getColor());
        String NAME = memb.getEffectiveName();
        String TAG = "#" + memb.getUser().getDiscriminator();
        String GUILD_JOIN_DATE = memb.getTimeJoined().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        String DISCORD_JOINED_DATE = memb.getUser().getTimeCreated().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        String ID = memb.getUser().getId();
        String STATUS = memb.getOnlineStatus().getKey().replace("offline", L.t("user_info.x_offline")).replace("dnd", L.t("user_info.red_circle_do_not_disturb")).replace("idle", L.t("user_info.idle")).replace("online", L.t("user_info.white_check_mark_online"));
        String ROLES;
        String GAME;
        String AVATAR = memb.getUser().getAvatarUrl();

        log.debug(L.t("user_info.username") + memb.getEffectiveName() + "\n" +
                L.t("user_info.tag") + memb.getUser().getDiscriminator() + "\n" +
                L.t("user_info.joined_server")
                + memb.getUser().getTimeCreated()
                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")) + "\n" +
                L.t("user_info.user_id") + memb.getUser().getId() + "\n" +
                L.t("user_info.online_status") + memb.getOnlineStatus());

        try {
            GAME = memb.getActivities().toString();
        } catch (Exception e) {
            GAME = "-/-";
        }

        StringBuilder ROLESBuilder = new StringBuilder();
        for (Role r : memb.getRoles()) {
            ROLESBuilder.append(r.getName()).append(", ");
        }
        ROLES = ROLESBuilder.toString();
        if (ROLES.length() > 0)
            ROLES = ROLES.substring(0, ROLES.length() - 2);
        else
            ROLES = L.t("user_info.no_roles_server");

        if (AVATAR == null) {
            AVATAR = L.t("user_info.no_avatar");
        }

        eb.setAuthor(memb.getUser().getName() + TAG + L.t("user_info.user_info"), null, null)
                .addField(L.t("user_info.pencil2_name_nickname"), "**" + NAME + "**", true)
                .addField(":link: DiscordTag", "**" + TAG + "**", true)
                .addField(":1234: ID", "**" + ID + "**", true)
                .addBlankField(false)
                .addField(L.t("user_info.signal_strength_current_status"), "**" + STATUS + "**", true)
                .addField(L.t("user_info.video_game_playing"), "**" + GAME + "**", true)
                .addField(L.t("user_info.tools_roles"), "**" + ROLES + "**", true)
                .addBlankField(false)
                .addField(L.t("user_info.inbox_tray_joined_server"), "**" + GUILD_JOIN_DATE + "**", true)
                .addField(L.t("user_info.beginner_account_created"), "**" + DISCORD_JOINED_DATE + "**", true)
                .addBlankField(false)
                .addField(L.t("user_info.frame_photo_avatar_url"), AVATAR, false);

        if (!AVATAR.equals(L.t("user_info.no_avatar"))) {
            eb.setAuthor(memb.getUser().getName() + TAG + L.t("user_info.user_info"), null, AVATAR);
        }

        event.getChannel().sendMessageEmbeds(eb.build()).queue();
    }
}
