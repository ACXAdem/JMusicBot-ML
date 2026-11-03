package dev.cosgy.jmusicbot.framework.jdautilities.command;

import dev.cosgy.jmusicbot.i18n.L;

import net.dv8tion.jda.api.Permission;

public abstract class Interaction {
    protected boolean ownerCommand = false;
    protected boolean guildOnly = false;
    protected Permission[] userPermissions = new Permission[0];
    protected Permission[] botPermissions = new Permission[0];
    protected int cooldown = 0;
    protected CooldownScope cooldownScope = CooldownScope.USER;
    protected String userMissingPermMessage = L.t("interaction.need_permission_use_command");
    protected String botMissingPermMessage = L.t("interaction.i_need_permission_do");

    public Permission[] getUserPermissions() {
        return userPermissions;
    }

    public Permission[] getBotPermissions() {
        return botPermissions;
    }

    public boolean isOwnerCommand() {
        return ownerCommand;
    }
}
