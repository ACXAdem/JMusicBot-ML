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
package dev.cosgy.jmusicbot.i18n;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Translations. Every user-facing and log text of the bot is looked up here by key.
 * <p>
 * Language files live in {@code src/main/resources/lang/<code>.json}. The active language is
 * read from {@code language} in config.txt (or the {@code -Djmusicbot.lang=xx} system property);
 * the default is English. A key missing from a language falls back to English.
 * <p>
 * To add a language: copy {@code en.json} to {@code <code>.json}, translate the values and
 * add the code to {@link #LANGUAGES}.
 */
public final class L {
    /** Supported languages: code → Discord locales the language is shown for. */
    public static final Map<String, List<DiscordLocale>> LANGUAGES = new LinkedHashMap<>();

    static {
        LANGUAGES.put("en", List.of(DiscordLocale.ENGLISH_US, DiscordLocale.ENGLISH_UK));
        LANGUAGES.put("tr", List.of(DiscordLocale.TURKISH));
        LANGUAGES.put("ja", List.of(DiscordLocale.JAPANESE));
        LANGUAGES.put("de", List.of(DiscordLocale.GERMAN));
        LANGUAGES.put("es", List.of(DiscordLocale.SPANISH, DiscordLocale.SPANISH_LATAM));
        LANGUAGES.put("ru", List.of(DiscordLocale.RUSSIAN));
    }

    public static final String DEFAULT = "en";

    private static final Map<String, Map<String, String>> BUNDLES = new HashMap<>();
    private static volatile String lang;
    /** Reverse lookup (text in the active language → key), used for Discord localizations. */
    private static volatile Map<String, String> reverse;

    private L() {
    }

    /** Text of {@code key} in the active language. */
    public static String t(String key) {
        String s = bundle(lang()).get(key);
        if (s == null) s = bundle(DEFAULT).get(key);
        return s == null ? key : s;
    }

    /** Text of {@code key} in the active language, formatted with {@link String#format}. */
    public static String f(String key, Object... args) {
        return String.format(t(key), args);
    }

    /** Text of {@code key} in a specific language (null if that language has no such key). */
    public static String in(String language, String key) {
        return bundle(language).get(key);
    }

    /** Key of a text in the active language (null if the text did not come from a language file). */
    public static String keyOf(String text) {
        if (text == null) return null;
        Map<String, String> r = reverse;
        if (r == null) {
            Map<String, String> m = new HashMap<>();
            for (Map.Entry<String, String> e : bundle(lang()).entrySet()) m.putIfAbsent(e.getValue(), e.getKey());
            reverse = r = m;
        }
        return r.get(text);
    }

    /**
     * Translations of a text (that came from a language file) into every other supported language,
     * as Discord locales. Texts longer than {@code max} characters are skipped.
     */
    public static Map<DiscordLocale, String> localizations(String text, int max) {
        Map<DiscordLocale, String> out = new LinkedHashMap<>();
        String key = keyOf(text);
        if (key == null) return out;
        for (Map.Entry<String, List<DiscordLocale>> e : LANGUAGES.entrySet()) {
            if (e.getKey().equals(lang())) continue;
            String v = in(e.getKey(), key);
            if (v == null || v.isBlank() || v.length() > max) continue;
            for (DiscordLocale loc : e.getValue()) out.put(loc, v);
        }
        return out;
    }

    /** Active language code (en, tr, ja, de, es, ru). */
    public static String lang() {
        String l = lang;
        if (l == null) {
            synchronized (L.class) {
                if (lang == null) lang = detect();
                l = lang;
            }
        }
        return l;
    }

    /** Sets the active language (for tests or when config.txt changes). */
    public static synchronized void setLang(String code) {
        lang = normalize(code);
        reverse = null;
    }

    public static boolean isTurkish() {
        return "tr".equals(lang());
    }

    /** Java locale of the active language (for lower/upper-casing). */
    public static Locale locale() {
        return Locale.forLanguageTag(lang());
    }

    static String normalize(String code) {
        if (code == null) return DEFAULT;
        String c = code.trim().toLowerCase(Locale.ROOT);
        int i = c.indexOf('-');
        if (i < 0) i = c.indexOf('_');
        if (i > 0) c = c.substring(0, i);
        return LANGUAGES.containsKey(c) ? c : DEFAULT;
    }

    private static String detect() {
        String p = System.getProperty("jmusicbot.lang", System.getProperty("acxbot.lang"));
        if (p != null && !p.isBlank()) return normalize(p);
        try {
            Path cfg = Paths.get(System.getProperty("config.file", System.getProperty("config", "config.txt")));
            if (Files.isRegularFile(cfg)) {
                Config c = ConfigFactory.parseFile(cfg.toFile());
                if (c.hasPath("language")) return normalize(c.getString("language"));
                // config.txt from an older version (no language key): keep the language it was written in
                String text = Files.readString(cfg, StandardCharsets.UTF_8);
                if (text.contains("(Türkçe) Ayarları") || text.contains("Ayar dosyasının sürümü")) return "tr";
                if (text.matches("(?s).*[\\u3040-\\u30ff].*")) return "ja";
            }
        } catch (Exception ignored) {
            // A broken config.txt is reported by BotConfig; fall back to the default here
        }
        return DEFAULT;
    }

    private static Map<String, String> bundle(String code) {
        Map<String, String> b = BUNDLES.get(code);
        if (b != null) return b;
        synchronized (BUNDLES) {
            b = BUNDLES.get(code);
            if (b != null) return b;
            Map<String, String> m = new HashMap<>();
            try (InputStream in = L.class.getResourceAsStream("/lang/" + code + ".json")) {
                if (in != null) {
                    JSONObject o = new JSONObject(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    for (String k : o.keySet()) m.put(k, o.getString(k));
                }
            } catch (Exception e) {
                System.err.println("[JMusicBot-ML] Could not load language file " + code + ".json: " + e);
            }
            b = Collections.unmodifiableMap(m);
            BUNDLES.put(code, b);
            return b;
        }
    }
}
