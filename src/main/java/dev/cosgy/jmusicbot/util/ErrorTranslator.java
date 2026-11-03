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

import dev.cosgy.jmusicbot.i18n.L;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Translates English error messages of the audio library (Lavaplayer / YouTube source)
 * into the bot language before showing them to the user. Unknown messages are left as they are.
 */
public final class ErrorTranslator {
    private ErrorTranslator() {
    }

    private static final Map<String, String> MAP = new LinkedHashMap<>();

    static {
        MAP.put("connecting to the url failed", L.t("error_translator.could_not_reach_url"));
        MAP.put("sign in to confirm your age", L.t("error_translator.video_requires_age_verification_sign"));
        MAP.put("sign in to confirm you", L.t("error_translator.youtube_asked_bot_verification_try"));
        MAP.put("requires login", L.t("error_translator.video_requires_sign"));
        MAP.put("login required", L.t("error_translator.video_requires_sign"));
        MAP.put("this video is unavailable", L.t("error_translator.video_unavailable"));
        MAP.put("video unavailable", L.t("error_translator.video_unavailable"));
        MAP.put("this video is private", L.t("error_translator.video_private"));
        MAP.put("private video", L.t("error_translator.video_private"));
        MAP.put("not available in your country", L.t("error_translator.video_not_available_country"));
        MAP.put("blocked it in your country", L.t("error_translator.video_blocked_country"));
        MAP.put("copyright", L.t("error_translator.video_was_removed_due_copyright"));
        MAP.put("premieres in", L.t("error_translator.video_has_not_been_released"));
        MAP.put("live event will begin", L.t("error_translator.live_stream_has_not_started"));
        MAP.put("unknown file format", L.t("error_translator.unknown_file_format"));
        MAP.put("not a valid url", L.t("error_translator.not_valid_url"));
        MAP.put("playlist is private", L.t("error_translator.playlist_private"));
        MAP.put("the playlist does not exist", L.t("error_translator.playlist_not_found"));
        MAP.put("something went wrong when looking up the track", L.t("error_translator.something_went_wrong_while_looking"));
        MAP.put("something went wrong when decoding the track", L.t("error_translator.something_went_wrong_while_decoding"));
        MAP.put("something broke when playing the track", L.t("error_translator.something_went_wrong_while_playing"));
        MAP.put("all clients failed to load the item", L.t("error_translator.youtube_did_not_provide_content"));
        MAP.put("this track is not playable", L.t("error_translator.track_cannot_played"));
        MAP.put("track is not playable", L.t("error_translator.track_cannot_played"));
        MAP.put("not found", L.t("error_translator.content_not_found"));
    }

    /** Translates the message; returns it unchanged if it is not recognized. */
    public static String tr(String message) {
        if (message == null || message.isBlank()) return L.t("error_translator.unknown_error");
        String lower = message.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> e : MAP.entrySet()) {
            if (lower.contains(e.getKey())) return e.getValue();
        }
        return message;
    }
}
