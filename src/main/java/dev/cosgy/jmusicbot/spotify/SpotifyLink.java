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
package dev.cosgy.jmusicbot.spotify;

import dev.cosgy.jmusicbot.i18n.L;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a Spotify link (URL / URI).
 * Supported formats:
 * <ul>
 *     <li>https://open.spotify.com/track/ID</li>
 *     <li>https://open.spotify.com/intl-tr/album/ID?si=...</li>
 *     <li>https://open.spotify.com/user/xxx/playlist/ID</li>
 *     <li>https://open.spotify.com/embed/artist/ID</li>
 *     <li>spotify:track:ID</li>
 *     <li>https://spotify.link/xxxx (short link, resolved separately)</li>
 * </ul>
 */
public final class SpotifyLink {

    public enum Type {
        TRACK, ALBUM, PLAYLIST, ARTIST;

        public String path() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** Name shown to the user (in the bot language). */
        public String turkish() {
            switch (this) {
                case TRACK:
                    return L.t("spotify_link.track");
                case ALBUM:
                    return L.t("spotify_link.album");
                case PLAYLIST:
                    return L.t("spotify_link.playlist");
                case ARTIST:
                    return L.t("spotify_link.artist");
                default:
                    return name();
            }
        }
    }

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(?:https?://)?(?:open|play)\\.spotify\\.com/(?:intl-[a-z]{2}(?:[-_][a-z]{2,4})?/)?(?:embed/|embed-podcast/)?(?:user/[^/]+/)?"
                    + "(track|album|playlist|artist)/([A-Za-z0-9]{10,40})(?:[/?#].*)?$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern URI_PATTERN = Pattern.compile(
            "^spotify:(?:user:[^:]+:)?(track|album|playlist|artist):([A-Za-z0-9]{10,40})$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SHORT_PATTERN = Pattern.compile(
            "^(?:https?://)?(?:spotify\\.link|spotify\\.app\\.link)/\\S+$",
            Pattern.CASE_INSENSITIVE);

    /** For finding an open.spotify.com link inside any text (short link resolution). */
    static final Pattern URL_IN_TEXT = Pattern.compile(
            "https?://open\\.spotify\\.com/(?:intl-[a-z]{2}(?:[-_][a-z]{2,4})?/)?(?:user/[^/\"']+/)?(track|album|playlist|artist)/([A-Za-z0-9]{10,40})",
            Pattern.CASE_INSENSITIVE);

    public final Type type;
    public final String id;

    public SpotifyLink(Type type, String id) {
        this.type = type;
        this.id = id;
    }

    /** Cleans up the text typed by the user (<link> format, spaces). */
    public static String clean(String input) {
        if (input == null) return "";
        String s = input.trim();
        if (s.startsWith("<") && s.endsWith(">")) s = s.substring(1, s.length() - 1).trim();
        return s;
    }

    /** Parses a normal Spotify link/URI; returns null otherwise. Short links are not resolved here. */
    public static SpotifyLink parse(String input) {
        String s = clean(input);
        if (s.isEmpty()) return null;
        Matcher m = URL_PATTERN.matcher(s);
        if (!m.matches()) m = URI_PATTERN.matcher(s);
        if (!m.matches()) return null;
        return new SpotifyLink(Type.valueOf(m.group(1).toUpperCase(Locale.ROOT)), m.group(2));
    }

    public static boolean isShortLink(String input) {
        return SHORT_PATTERN.matcher(clean(input)).matches();
    }

    /** Is the text a Spotify link (including short links)? */
    public static boolean isSpotify(String input) {
        return parse(input) != null || isShortLink(input);
    }

    public String openUrl() {
        return "https://open.spotify.com/" + type.path() + "/" + id;
    }

    public String embedUrl() {
        return "https://open.spotify.com/embed/" + type.path() + "/" + id;
    }

    @Override
    public String toString() {
        return openUrl();
    }
}
