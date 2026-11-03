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
package com.jagrosh.jmusicbot.audio;

import dev.cosgy.jmusicbot.i18n.L;

import dev.lavalink.youtube.YoutubeAudioSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Sign in to YouTube with a Google account (OAuth, device code flow).
 * <p>
 * On first start a code is written to the console; the user enters it at google.com/device and
 * chooses an account. The refresh token is saved to {@code youtube-oauth.txt},
 * so no sign-in is needed on later starts.
 */
final class YoutubeOauthStore {
    private static final Logger log = LoggerFactory.getLogger("YouTubeGiris");
    static final Path FILE = Paths.get("youtube-oauth.txt").toAbsolutePath();

    private YoutubeOauthStore() {
    }

    static void setup(YoutubeAudioSourceManager yt, String configToken) {
        String token = configToken == null || configToken.isBlank() ? readFile() : configToken.trim();
        if (token == null) {
            log.info("==================================================");
            log.info(L.t("youtube_oauth_store.starting_youtube_sign_with_google"));
            log.info(L.t("youtube_oauth_store.line_saying_go_https_www"));
            log.info(L.t("youtube_oauth_store.1_open_https_www_google"));
            log.info(L.t("youtube_oauth_store.2_enter_code_from_line"));
            log.info(L.t("youtube_oauth_store.3_allow_access_bot_detects"));
            log.info("==================================================");
        } else {
            log.info(L.t("youtube_oauth_store.youtube_google_account_sign_enabled"));
        }
        try {
            yt.useOauth2(token, false);
        } catch (Exception e) {
            // Don't let startup crash if there is no internet or the token is invalid; the token is refreshed when first needed
            log.warn(L.t("youtube_oauth_store.could_not_connect_youtube_account"), e.toString());
            try {
                yt.useOauth2(token, true);
            } catch (Exception ignored) {
            }
        }

        // Save to the file when sign-in completes (or the token is refreshed)
        final String[] saved = {token};
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "youtube-oauth-store");
            t.setDaemon(true);
            return t;
        });
        ses.scheduleWithFixedDelay(() -> {
            try {
                String now = yt.getOauth2RefreshToken();
                if (now != null && !now.isBlank() && !now.equals(saved[0])) {
                    Files.writeString(FILE, now + System.lineSeparator(), StandardCharsets.UTF_8);
                    saved[0] = now;
                    log.info(L.t("youtube_oauth_store.google_account_connected_token_saved"), FILE);
                }
            } catch (Exception e) {
                log.debug(L.t("youtube_oauth_store.could_not_save_youtube_token"), e.toString());
            }
        }, 5, 10, TimeUnit.SECONDS);
    }

    private static String readFile() {
        try {
            if (!Files.isRegularFile(FILE)) return null;
            String s = Files.readString(FILE, StandardCharsets.UTF_8).trim();
            return s.isEmpty() ? null : s;
        } catch (Exception e) {
            return null;
        }
    }
}
