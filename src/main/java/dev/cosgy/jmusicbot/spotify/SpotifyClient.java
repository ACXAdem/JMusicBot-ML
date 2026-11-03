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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads track / album / playlist / artist info from Spotify.
 *
 * <p>Two methods are used:</p>
 * <ol>
 *     <li><b>Official Web API</b> (needs {@code spclient}/{@code spsecret} in config.txt).
 *     Used for single tracks and albums; the ISRC code gives the most accurate match.</li>
 *     <li><b>Public "embed" page</b> (no key needed). Since February 2026 Spotify no longer lets
 *     Development Mode apps read other people's playlists through the API, so
 *     playlists and artists are read this way. The embed page returns at most 100 tracks.</li>
 * </ol>
 * If the API fails (no key, quota exceeded, etc.) the embed page is used automatically.
 */
public class SpotifyClient {
    private static final Logger log = LoggerFactory.getLogger("Spotify");

    private static final String AUTH_URL = "https://accounts.spotify.com/api/token";
    private static final String API_URL = "https://api.spotify.com/v1";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0";
    /** Maximum number of tracks taken from an album/playlist. */
    private static final int MAX_TRACKS = 500;

    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "<script[^>]*\\b(?:id|type)=\"(resource|initial-state|__NEXT_DATA__)\"[^>]*>(.*?)</script>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private final String clientId;
    private final String clientSecret;
    private final HttpClient http;
    private final HttpClient httpNoRedirect;

    private final Object tokenLock = new Object();
    private volatile String accessToken;
    private volatile long accessTokenExpiresAt;
    /** So we don't retry on every request when the credentials are wrong. */
    private volatile long authBlockedUntil;

    public SpotifyClient(String clientId, String clientSecret) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.httpNoRedirect = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /** Are spclient and spsecret set in config.txt? */
    public boolean hasCredentials() {
        return !clientId.isEmpty() && !clientSecret.isEmpty();
    }

    // =====================================================================
    //  General entry point
    // =====================================================================

    /**
     * Loads the content of a Spotify link. The official API is tried first (when possible), then the embed page.
     */
    public SpotifyCollection load(SpotifyLink link) throws SpotifyException {
        SpotifyException apiError = null;

        boolean apiFirst = hasCredentials() && (link.type == SpotifyLink.Type.TRACK || link.type == SpotifyLink.Type.ALBUM);
        if (apiFirst) {
            try {
                return link.type == SpotifyLink.Type.TRACK ? apiTrack(link) : apiAlbum(link);
            } catch (SpotifyException e) {
                if (e.getStatus() == 404 || e.getStatus() == 400) {
                    throw new SpotifyException(link.type.turkish() + L.t("spotify_client.not_found_spotify_link_may"), e.getStatus(), e);
                }
                apiError = e;
                log.warn(L.t("spotify_client.could_not_load_via_spotify"), e.getMessage());
            }
        }

        try {
            return embed(link);
        } catch (SpotifyException embedError) {
            // If the playlist couldn't be read via the embed page and keys are set, try the API as a last resort
            // (only works for the app owner's own playlists).
            if (!apiFirst && hasCredentials() && link.type == SpotifyLink.Type.PLAYLIST) {
                try {
                    return apiPlaylist(link);
                } catch (SpotifyException e) {
                    log.debug(L.t("spotify_client.could_not_read_playlist_via"), e.getMessage());
                }
            }
            if (apiError != null) {
                throw new SpotifyException(apiError.getMessage(), apiError.getStatus(), embedError);
            }
            throw embedError;
        }
    }

    /** Resolves spotify.link / spotify.app.link short links to the real link. */
    public SpotifyLink resolveShortLink(String shortUrl) throws SpotifyException {
        String url = SpotifyLink.clean(shortUrl);
        if (!url.startsWith("http")) url = "https://" + url;
        try {
            String current = url;
            for (int i = 0; i < 5; i++) {
                HttpRequest req = HttpRequest.newBuilder(URI.create(current))
                        .header("User-Agent", USER_AGENT)
                        .timeout(Duration.ofSeconds(15))
                        .GET().build();
                HttpResponse<String> res = httpNoRedirect.send(req, HttpResponse.BodyHandlers.ofString());
                Optional<String> location = res.headers().firstValue("location");
                if (res.statusCode() >= 300 && res.statusCode() < 400 && location.isPresent()) {
                    String next = URI.create(current).resolve(location.get()).toString();
                    SpotifyLink link = SpotifyLink.parse(next);
                    if (link != null) return link;
                    current = next;
                    continue;
                }
                Matcher m = SpotifyLink.URL_IN_TEXT.matcher(res.body() == null ? "" : res.body());
                if (m.find()) {
                    return new SpotifyLink(SpotifyLink.Type.valueOf(m.group(1).toUpperCase(Locale.ROOT)), m.group(2));
                }
                break;
            }
        } catch (IOException e) {
            throw new SpotifyException(L.t("spotify_client.could_not_open_spotify_short") + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SpotifyException(L.t("spotify_client.operation_cancelled"), e);
        }
        throw new SpotifyException(L.t("spotify_client.could_not_resolve_spotify_short"));
    }

    /** Searches tracks on Spotify (used by the spsearch: prefix; needs keys). */
    public List<SpotifyTrackData> search(String query, int limit) throws SpotifyException {
        if (!hasCredentials()) {
            throw new SpotifyException(L.t("spotify_client.search_spotify_spclient_spsecret_must"));
        }
        int l = Math.max(1, Math.min(10, limit)); // Development Mode'da en fazla 10
        JSONObject json = apiGet(API_URL + "/search?type=track&limit=" + l + "&q="
                + URLEncoder.encode(query, StandardCharsets.UTF_8));
        List<SpotifyTrackData> list = new ArrayList<>();
        JSONObject tracks = json.optJSONObject("tracks");
        JSONArray items = tracks == null ? null : tracks.optJSONArray("items");
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                JSONObject t = items.optJSONObject(i);
                if (t != null) list.add(parseApiTrack(t, null));
            }
        }
        return list;
    }

    // =====================================================================
    //  Resmi Web API
    // =====================================================================

    private SpotifyCollection apiTrack(SpotifyLink link) throws SpotifyException {
        JSONObject json = apiGet(API_URL + "/tracks/" + link.id);
        SpotifyTrackData t = parseApiTrack(json, null);
        List<SpotifyTrackData> list = new ArrayList<>();
        list.add(t);
        return new SpotifyCollection(link, t.title, t.artists, t.imageUrl, list, 1, "api");
    }

    private SpotifyCollection apiAlbum(SpotifyLink link) throws SpotifyException {
        JSONObject album = apiGet(API_URL + "/albums/" + link.id);
        String name = album.optString("name", "");
        String artists = joinArtists(album.optJSONArray("artists"));
        String image = largestImage(album.optJSONArray("images"));

        List<SpotifyTrackData> list = new ArrayList<>();
        JSONObject page = album.optJSONObject("tracks");
        int total = page == null ? -1 : page.optInt("total", -1);
        while (page != null && list.size() < MAX_TRACKS) {
            JSONArray items = page.optJSONArray("items");
            if (items != null) {
                for (int i = 0; i < items.length() && list.size() < MAX_TRACKS; i++) {
                    JSONObject t = items.optJSONObject(i);
                    if (t != null) list.add(parseApiTrack(t, image));
                }
            }
            String next = page.optString("next", null);
            if (next == null || next.isEmpty() || "null".equals(next)) break;
            page = apiGet(next);
        }
        return new SpotifyCollection(link, name, artists, image, list, total, "api");
    }

    /** Only works for the app owner's own playlists (Development Mode restriction). */
    private SpotifyCollection apiPlaylist(SpotifyLink link) throws SpotifyException {
        JSONObject pl = apiGet(API_URL + "/playlists/" + link.id);
        String name = pl.optString("name", "");
        JSONObject ownerObj = pl.optJSONObject("owner");
        String owner = ownerObj == null ? "" : ownerObj.optString("display_name", "");
        String image = largestImage(pl.optJSONArray("images"));

        // February 2026: the "tracks" field was renamed to "items". Support both.
        JSONObject page = pl.optJSONObject("items");
        if (page == null) page = pl.optJSONObject("tracks");
        if (page == null || page.optJSONArray("items") == null) {
            throw new SpotifyException(L.t("spotify_client.playlist_s_tracks_cannot_read"), 403, null);
        }
        int total = page.optInt("total", -1);
        List<SpotifyTrackData> list = new ArrayList<>();
        while (page != null && list.size() < MAX_TRACKS) {
            JSONArray items = page.optJSONArray("items");
            if (items != null) {
                for (int i = 0; i < items.length() && list.size() < MAX_TRACKS; i++) {
                    JSONObject entry = items.optJSONObject(i);
                    if (entry == null) continue;
                    JSONObject t = entry.optJSONObject("item");
                    if (t == null) t = entry.optJSONObject("track");
                    if (t == null || entry.optBoolean("is_local", false)) continue;
                    if (!"track".equals(t.optString("type", "track"))) continue; // skip podcast episodes
                    list.add(parseApiTrack(t, null));
                }
            }
            String next = page.optString("next", null);
            if (next == null || next.isEmpty() || "null".equals(next)) break;
            page = apiGet(next);
        }
        return new SpotifyCollection(link, name, owner, image, list, total, "api");
    }

    private SpotifyTrackData parseApiTrack(JSONObject t, String fallbackImage) {
        String image = fallbackImage;
        JSONObject album = t.optJSONObject("album");
        if (album != null) {
            String img = largestImage(album.optJSONArray("images"));
            if (img != null) image = img;
        }
        JSONArray artistsArr = t.optJSONArray("artists");
        String primary = "";
        if (artistsArr != null && artistsArr.length() > 0 && artistsArr.optJSONObject(0) != null) {
            primary = artistsArr.optJSONObject(0).optString("name", "");
        }
        JSONObject ext = t.optJSONObject("external_ids");
        String isrc = ext == null ? null : ext.optString("isrc", null);
        return new SpotifyTrackData(
                t.optString("id", null),
                t.optString("name", ""),
                joinArtists(artistsArr),
                primary,
                t.optLong("duration_ms", 0),
                isrc,
                image);
    }

    private JSONObject apiGet(String url) throws SpotifyException {
        return apiGet(url, true);
    }

    private JSONObject apiGet(String url, boolean allowRetry) throws SpotifyException {
        String token = getAccessToken(false);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .GET().build();
        HttpResponse<String> res = send(req);
        int code = res.statusCode();
        String body = res.body() == null ? "" : res.body();

        if (code == 200) {
            try {
                return new JSONObject(body);
            } catch (JSONException e) {
                throw new SpotifyException(L.t("spotify_client.received_unexpected_response_from_spotify"), code, e);
            }
        }
        if (code == 401 && allowRetry) {
            getAccessToken(true); // the token may have expired; refresh and try once more
            return apiGet(url, false);
        }
        if (code == 429) {
            if (body.contains("QUOTA_EXCEEDED")) {
                throw new SpotifyException(L.t("spotify_client.spotify_api_quota_exceeded_try"), code, null);
            }
            long wait = res.headers().firstValue("retry-after").map(SpotifyClient::parseLongSafe).orElse(5L);
            if (allowRetry && wait <= 5) {
                sleep(wait * 1000L);
                return apiGet(url, false);
            }
            throw new SpotifyException(L.t("spotify_client.spotify_rate_limit_reached") + wait + L.t("spotify_client.try_again_few_seconds"), code, null);
        }
        if (code == 403) {
            throw new SpotifyException(L.t("spotify_client.spotify_denied_access_content_possibly"), code, null);
        }
        if (code == 404) {
            throw new SpotifyException(L.t("spotify_client.not_found_spotify"), code, null);
        }
        throw new SpotifyException(L.t("spotify_client.spotify_api_error_http") + code + ").", code, null);
    }

    private String getAccessToken(boolean forceRefresh) throws SpotifyException {
        if (!hasCredentials()) throw new SpotifyException(L.t("spotify_client.spotify_api_keys_are_not"));
        long now = System.currentTimeMillis();
        if (!forceRefresh && accessToken != null && now < accessTokenExpiresAt - 60_000) return accessToken;
        synchronized (tokenLock) {
            now = System.currentTimeMillis();
            if (!forceRefresh && accessToken != null && now < accessTokenExpiresAt - 60_000) return accessToken;
            if (now < authBlockedUntil) {
                throw new SpotifyException(L.t("spotify_client.spotify_authentication_failed_check_spclient"));
            }
            String basic = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
            HttpRequest req = HttpRequest.newBuilder(URI.create(AUTH_URL))
                    .header("Authorization", "Basic " + basic)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                    .build();
            HttpResponse<String> res = send(req);
            String body = res.body() == null ? "" : res.body();
            if (res.statusCode() != 200) {
                if (res.statusCode() == 400 || res.statusCode() == 401) {
                    authBlockedUntil = System.currentTimeMillis() + 5 * 60_000; // don't retry for 5 minutes
                    log.error(L.t("spotify_client.spotify_authentication_failed_http"), res.statusCode(), body);
                    throw new SpotifyException(L.t("spotify_client.spotify_authentication_failed_check_spclient"), res.statusCode(), null);
                }
                throw new SpotifyException(L.t("spotify_client.could_not_get_spotify_access") + res.statusCode() + ").", res.statusCode(), null);
            }
            try {
                JSONObject json = new JSONObject(body);
                accessToken = json.getString("access_token");
                accessTokenExpiresAt = System.currentTimeMillis() + json.optLong("expires_in", 3600) * 1000L;
                log.debug(L.t("spotify_client.spotify_access_token_refreshed"));
                return accessToken;
            } catch (JSONException e) {
                throw new SpotifyException(L.t("spotify_client.could_not_read_spotify_access"), e);
            }
        }
    }

    // =====================================================================
    //  Embed page (no key needed)
    // =====================================================================

    SpotifyCollection embed(SpotifyLink link) throws SpotifyException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(link.embedUrl()))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "en-US,en;q=0.8")
                .timeout(Duration.ofSeconds(20))
                .GET().build();
        HttpResponse<String> res = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            res = send(req);
            if (res.statusCode() == 200 && res.body() != null && res.body().contains("<script")) break;
            if (res.statusCode() == 404) {
                throw new SpotifyException(link.type.turkish() + L.t("spotify_client.not_found_spotify_may_private"), 404, null);
            }
            sleep(500L * (attempt + 1));
        }
        if (res == null || res.statusCode() != 200) {
            throw new SpotifyException(L.t("spotify_client.could_not_read_spotify_page") + (res == null ? "?" : res.statusCode()) + ").");
        }
        JSONObject entity = extractEmbedEntity(res.body());
        if (entity == null) {
            throw new SpotifyException(L.t("spotify_client.spotify_page_format_may_have"));
        }
        return parseEmbedEntity(link, entity);
    }

    /** Extracts the content (entity) JSON from the embed HTML. Package-visible for tests. */
    static JSONObject extractEmbedEntity(String html) {
        if (html == null) return null;
        Matcher m = SCRIPT_PATTERN.matcher(html);
        while (m.find()) {
            String kind = m.group(1);
            String content = m.group(2).trim();
            if (content.isEmpty()) continue;
            try {
                if (!content.startsWith("{")) {
                    content = new String(Base64.getMimeDecoder().decode(content), StandardCharsets.UTF_8);
                }
                JSONObject root = new JSONObject(content);
                JSONObject entity = null;
                if ("__NEXT_DATA__".equalsIgnoreCase(kind)) {
                    entity = path(root, "props", "pageProps", "state", "data", "entity");
                } else if ("initial-state".equalsIgnoreCase(kind)) {
                    entity = path(root, "data", "entity");
                } else {
                    entity = root.has("trackList") || root.has("uri") ? root : path(root, "data", "entity");
                }
                if (entity == null) entity = findEntity(root, 0);
                if (entity != null) return entity;
            } catch (RuntimeException ignored) {
                // sonraki script'i dene
            }
        }
        return null;
    }

    static SpotifyCollection parseEmbedEntity(SpotifyLink link, JSONObject entity) throws SpotifyException {
        String name = firstNonEmpty(entity.optString("name", ""), entity.optString("title", ""));
        String subtitle = firstNonEmpty(entity.optString("subtitle", ""), joinArtists(entity.optJSONArray("artists")));
        String image = embedImage(entity);

        List<SpotifyTrackData> list = new ArrayList<>();
        JSONArray trackList = entity.optJSONArray("trackList");
        if (trackList != null) {
            for (int i = 0; i < trackList.length() && list.size() < MAX_TRACKS; i++) {
                JSONObject t = trackList.optJSONObject(i);
                if (t == null) continue;
                String uri = t.optString("uri", "");
                if (!uri.isEmpty() && !uri.startsWith("spotify:track:")) continue; // podcast episodes etc.
                String id = uri.startsWith("spotify:track:") ? uri.substring("spotify:track:".length()) : null;
                String title = firstNonEmpty(t.optString("title", ""), t.optString("name", ""));
                String artists = firstNonEmpty(joinArtists(t.optJSONArray("artists")), t.optString("subtitle", ""));
                artists = artists.replace(' ', ' ');
                long duration = readDuration(t);
                if (title.isEmpty()) continue;
                list.add(new SpotifyTrackData(id, title, artists, null, duration, null,
                        link.type == SpotifyLink.Type.ALBUM ? image : embedImage(t)));
            }
        } else if (link.type == SpotifyLink.Type.TRACK) {
            String artists = firstNonEmpty(joinArtists(entity.optJSONArray("artists")), entity.optString("subtitle", ""));
            list.add(new SpotifyTrackData(link.id, name, artists.replace(' ', ' '), null, readDuration(entity), null, image));
            subtitle = artists;
        }

        if (list.isEmpty()) {
            throw new SpotifyException(link.type.turkish() + L.t("spotify_client.contains_no_playable_tracks"));
        }
        int total = -1;
        JSONObject tracksObj = entity.optJSONObject("tracks");
        if (tracksObj != null) total = tracksObj.optInt("totalCount", tracksObj.optInt("total", -1));
        if (total < 0) total = entity.optInt("totalCount", entity.optInt("trackCount", -1));
        return new SpotifyCollection(link, name, subtitle, image, list, total, "embed");
    }

    // =====================================================================
    //  Helpers
    // =====================================================================

    private HttpResponse<String> send(HttpRequest req) throws SpotifyException {
        try {
            return http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new SpotifyException(L.t("spotify_client.could_not_connect_spotify") + e.getClass().getSimpleName()
                    + (e.getMessage() == null ? "" : " (" + e.getMessage() + ")"), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SpotifyException(L.t("spotify_client.operation_cancelled"), e);
        }
    }

    private static long readDuration(JSONObject t) {
        Object d = t.opt("duration");
        if (d instanceof Number) return ((Number) d).longValue();
        if (d instanceof JSONObject) {
            JSONObject o = (JSONObject) d;
            return o.optLong("totalMilliseconds", o.optLong("milliseconds", 0));
        }
        return t.optLong("duration_ms", t.optLong("durationMs", 0));
    }

    private static String embedImage(JSONObject o) {
        if (o == null) return null;
        JSONObject cover = o.optJSONObject("coverArt");
        String img = cover == null ? null : largestImage(cover.optJSONArray("sources"));
        if (img == null) img = largestImage(o.optJSONArray("images"));
        if (img == null) {
            JSONObject vi = o.optJSONObject("visualIdentity");
            if (vi != null) img = largestImage(vi.optJSONArray("image"));
        }
        return img;
    }

    private static String largestImage(JSONArray arr) {
        if (arr == null) return null;
        String best = null;
        int bestW = -1;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject img = arr.optJSONObject(i);
            if (img == null) continue;
            String url = img.optString("url", "");
            int w = img.optInt("width", img.optInt("maxWidth", 0));
            if (!url.isEmpty() && w > bestW) {
                best = url;
                bestW = w;
            }
        }
        return best;
    }

    private static String joinArtists(JSONArray arr) {
        if (arr == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject a = arr.optJSONObject(i);
            String n = a == null ? "" : a.optString("name", "");
            if (n.isEmpty()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(n);
        }
        return sb.toString();
    }

    private static JSONObject path(JSONObject root, String... keys) {
        JSONObject cur = root;
        for (String k : keys) {
            if (cur == null) return null;
            cur = cur.optJSONObject(k);
        }
        return cur;
    }

    /** If no known path is found, looks for an object in the JSON that has a "trackList" or a spotify: URI. */
    private static JSONObject findEntity(Object node, int depth) {
        if (depth > 8 || node == null) return null;
        if (node instanceof JSONObject) {
            JSONObject o = (JSONObject) node;
            if (o.has("trackList")) return o;
            String uri = o.optString("uri", "");
            if (uri.matches("spotify:(track|album|playlist|artist):[A-Za-z0-9]+") && (o.has("name") || o.has("title"))) return o;
            for (String k : o.keySet()) {
                JSONObject r = findEntity(o.opt(k), depth + 1);
                if (r != null) return r;
            }
        } else if (node instanceof JSONArray) {
            JSONArray a = (JSONArray) node;
            for (int i = 0; i < a.length(); i++) {
                JSONObject r = findEntity(a.opt(i), depth + 1);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static String firstNonEmpty(String a, String b) {
        return a != null && !a.isBlank() ? a.trim() : (b == null ? "" : b.trim());
    }

    private static long parseLongSafe(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 5L;
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
