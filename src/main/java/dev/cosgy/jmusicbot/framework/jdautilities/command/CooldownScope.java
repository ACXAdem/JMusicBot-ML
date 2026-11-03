package dev.cosgy.jmusicbot.framework.jdautilities.command;

import dev.cosgy.jmusicbot.i18n.L;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum CooldownScope {
    USER(L.t("cooldown_scope.per_user")),
    USER_GUILD(L.t("cooldown_scope.per_user_server")),
    USER_CHANNEL(L.t("cooldown_scope.per_user_channel")),
    GUILD(L.t("cooldown_scope.per_server")),
    CHANNEL(L.t("cooldown_scope.per_channel")),
    SHARD(L.t("cooldown_scope.per_shard")),
    USER_SHARD(L.t("cooldown_scope.per_user_shard")),
    GLOBAL(L.t("cooldown_scope.general"));

    public final String errorSpecification;

    CooldownScope(String errorSpecification) {
        this.errorSpecification = errorSpecification;
    }

    public String genKey(String name, long... ids) {
        String suffix = Arrays.stream(ids)
                .mapToObj(Long::toString)
                .collect(Collectors.joining("_"));
        return name + ':' + suffix;
    }
}
