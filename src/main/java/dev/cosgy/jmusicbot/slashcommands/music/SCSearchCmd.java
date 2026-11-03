/*
 * Copyright 2018 John Grosh (jagrosh).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.cosgy.jmusicbot.slashcommands.music;

import dev.cosgy.jmusicbot.i18n.L;

import com.jagrosh.jmusicbot.Bot;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class SCSearchCmd extends SearchCmd {
    public SCSearchCmd(Bot bot) {
        super(bot);
        this.searchPrefix = "scsearch:";
        this.name = "scsearch";
        this.help = L.t("scsearch.searches_soundcloud_given_terms");
        this.aliases = bot.getConfig().getAliases(this.name);
    }
}