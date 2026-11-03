/*
 *  Copyright 2026 JMusicBot-ML contributors
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
package dev.cosgy.jmusicbot.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Turkish names of the commands. Every command works with both its English and Turkish name.
 *
 * <ul>
 *     <li>The <b>first name</b> in the array is registered with Discord as a separate slash command in Turkish mode,
 *     or as a Turkish name localization otherwise (e.g. <code>/play</code> and <code>/oynat</code>). Slash names contain no Turkish
 *     characters so they are easy to type on any keyboard.</li>
 *     <li><b>All names</b> in the array can be used as prefix commands (e.g. <code>!çal</code>, <code>!cal</code>).</li>
 *     <li>Commands whose slash name is <code>null</code> (bot owner commands) only get a prefix alias;
 *     so Discord's 100-command limit is not exceeded.</li>
 * </ul>
 */
public final class TurkishCommandNames {
    private TurkishCommandNames() {
    }

    private static final Map<String, String[]> NAMES = new HashMap<>();
    /** Commands that are not registered as slash commands (prefix only). */
    private static final Map<String, Boolean> PREFIX_ONLY = new HashMap<>();

    private static void slash(String english, String... turkish) {
        NAMES.put(english, turkish);
    }

    private static void prefixOnly(String english, String... turkish) {
        NAMES.put(english, turkish);
        PREFIX_ONLY.put(english, Boolean.TRUE);
    }

    static {
        // Genel
        slash("help", "yardim", "yardım");
        slash("about", "hakkinda", "hakkında", "bilgi");
        slash("invite", "davet");
        slash("ping", "gecikme");
        slash("settings", "ayarlar");
        slash("serverinfo", "sunucubilgi", "sunucu");
        slash("cache", "onbellek", "önbellek");
        // Music
        slash("play", "oynat", "çal", "cal");
        slash("lyrics", "sozler", "sözler", "sarkisozu", "şarkısözü");
        slash("nowplaying", "calan", "çalan", "simdi", "şimdi");
        slash("playlists", "listeler");
        slash("mylist", "listem");
        slash("queue", "kuyruk", "sira", "sıra");
        slash("remove", "kaldir", "kaldır", "sil");
        slash("search", "ara");
        slash("scsearch", "scara");
        slash("ncsearch", "ncara");
        slash("seek", "sar", "konum");
        slash("shuffle", "karistir", "karıştır");
        slash("skip", "gec", "geç");
        slash("volume", "ses");
        // DJ
        slash("forceremove", "zorlasil");
        slash("forceskip", "zorlagec", "zorlageç");
        slash("next", "sonraki");
        slash("movetrack", "tasi", "taşı");
        slash("pause", "duraklat");
        slash("playnext", "siradaki", "sıradaki");
        slash("repeat", "tekrar", "tekrarla");
        slash("skipto", "atla");
        slash("forcetoend", "sonaekle");
        slash("stop", "durdur", "dur");
        // Admin
        slash("prefix", "onek", "önek");
        slash("setdj", "djayarla");
        slash("setskip", "gecmeorani", "geçmeoranı");
        slash("settc", "metinkanali", "metinkanalı");
        slash("setvc", "seskanali", "seskanalı");
        slash("setvcstatus", "kanaldurumu");
        slash("autoplaylist", "otoliste");
        // Bot owner (prefix alias only)
        prefixOnly("slist", "sunucular");
        prefixOnly("debug", "hataayikla", "hataayıkla");
        prefixOnly("setavatar", "avatar");
        prefixOnly("setgame", "oyunayarla");
        prefixOnly("setname", "adayarla");
        prefixOnly("setstatus", "durumayarla");
        prefixOnly("publist", "herkeseacik", "herkeseaçık");
        prefixOnly("shutdown", "kapat");
        prefixOnly("eval", "calistir", "çalıştır");
        prefixOnly("leave", "ayril", "ayrıl");
    }

    /** Turkish name that is also registered as a slash command (null if none). */
    public static String slashName(String englishName) {
        if (englishName == null || PREFIX_ONLY.containsKey(englishName)) return null;
        String[] tr = NAMES.get(englishName);
        return tr == null || tr.length == 0 ? null : tr[0];
    }

    /** All Turkish names accepted for prefix commands. */
    public static String[] aliases(String englishName) {
        String[] tr = englishName == null ? null : NAMES.get(englishName);
        return tr == null ? new String[0] : tr;
    }

    /** Is the given spelling one of this command's Turkish names? */
    public static boolean matches(String englishName, String input) {
        if (input == null) return false;
        String in = input.toLowerCase(Locale.ROOT);
        for (String a : aliases(englishName)) {
            if (a.toLowerCase(Locale.ROOT).equals(in)) return true;
        }
        return false;
    }

    /** Translations of option names for Turkish Discord users. */
    public static final Map<String, String> OPTION_NAMES;

    static {
        Map<String, String> m = new HashMap<>();
        m.put("input", "arama");
        m.put("name", "ad");
        m.put("link", "link");
        m.put("user", "kullanici");
        m.put("role", "rol");
        m.put("channel", "kanal");
        m.put("title", "baslik");
        m.put("url", "url");
        m.put("game", "oyun");
        m.put("position", "sira");
        m.put("from", "nereden");
        m.put("to", "nereye");
        m.put("vol", "seviye");
        m.put("time", "zaman");
        m.put("option", "secenek");
        m.put("status", "durum");
        m.put("percent", "yuzde");
        m.put("prefix", "onek");
        m.put("code", "kod");
        m.put("image", "resim");
        m.put("value", "deger");
        m.put("serverid", "sunucu_id");
        m.put("page", "sayfa");
        OPTION_NAMES = Collections.unmodifiableMap(m);
    }

    /** Translations of subcommand names for Turkish Discord users. */
    public static final Map<String, String> SUBCOMMAND_NAMES;

    static {
        Map<String, String> m = new HashMap<>();
        m.put("play", "oynat");
        m.put("show", "goster");
        m.put("make", "olustur");
        m.put("delete", "sil");
        m.put("append", "ekle");
        m.put("all", "hepsi");
        m.put("set", "ayarla");
        m.put("none", "kaldir");
        m.put("single", "tek");
        m.put("off", "kapat");
        m.put("stream", "yayin");
        m.put("listen", "dinle");
        m.put("watch", "izle");
        m.put("competing", "yaris");
        m.put("playing", "oyna");
        m.put("setdefault", "varsayilan");
        SUBCOMMAND_NAMES = Collections.unmodifiableMap(m);
    }
}
