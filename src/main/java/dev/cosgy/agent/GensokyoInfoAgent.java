/*
 *  Copyright 2022 Cosgy Dev (info@cosgy.dev).
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

package dev.cosgy.agent;

import dev.cosgy.jmusicbot.i18n.L;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.cosgy.agent.objects.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GensokyoInfoAgent extends Thread {
    private static final Logger log = LoggerFactory.getLogger(GensokyoInfoAgent.class);
    private static long INTERVAL_MILLIS = 1000; // 5 secs
    private static ResultSet info = null;
    private static String lastSong = "";

    public GensokyoInfoAgent() {
        setDaemon(true);
        setName("GensokyoInfoAgent");
    }

    @SuppressWarnings("UnusedReturnValue")
    private static ResultSet fetch() throws Exception {
        HttpURLConnection connection = null;
        try {

            if (info != null) {
                if (info.getSongtimes().getPlayed() < info.getSongtimes().getDuration()) {
                    return info;
                }
            }
            // URL to fetch the XML from
            //URL url = new URL("https://gensokyoradio.net/xml");

            System.setProperty("http.agent", "Chrome");

            HttpRequest req = HttpRequest.newBuilder(new URI("https://gensokyoradio.net/json"))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .setHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36")
                    .setHeader("accept-language", "ja,en-US;q=0.9,en;q=0.8")
                    .build();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = res.body();

            switch (res.statusCode()) {
                case 200:
                    // Map the JSON in the HTTP response to the ResultSet class
                    info = new ObjectMapper().readValue(body, ResultSet.class);
                    return info;
                case 403:
                    log.info(L.t("gensokyo_info_agent.could_not_fetch_gensokyo_radio"));
                    log.info("Body:{}", res.body());
                    return null;
                default:
                    log.info(L.t("gensokyo_info_agent.could_not_fetch_gensokyo_radio_2"));
                    return null;
            }

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static ResultSet getInfo() throws Exception {
        return fetch();
    }

    @Override
    public void run() {
        log.info(L.t("gensokyo_info_agent.gensokyoinfoagent_started"));

        //noinspection InfiniteLoopStatement
        while (true) {

            try {
                sleep(1000);
                // Don't request track info from Gensokyo Radio again until the current track ends.
                // Otherwise it would look like a DDoS attack...
                if (info != null) {
                    info.getSongtimes().setPlayed(info.getSongtimes().getPlayed() + 1);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
