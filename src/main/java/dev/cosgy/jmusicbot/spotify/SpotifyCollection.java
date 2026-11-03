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

import java.util.Collections;
import java.util.List;

/**
 * Content loaded from Spotify (single track, album, playlist or artist).
 */
public final class SpotifyCollection {
    public final SpotifyLink link;
    public final String name;
    /** Artist name for an album/artist, owner for a playlist. */
    public final String owner;
    public final String imageUrl;
    public final List<SpotifyTrackData> tracks;
    /** Total number of tracks on Spotify (-1 if unknown). The embed page can return at most 100 tracks. */
    public final int totalOnSpotify;
    /** Data source: "api" or "embed". */
    public final String source;

    public SpotifyCollection(SpotifyLink link, String name, String owner, String imageUrl,
                             List<SpotifyTrackData> tracks, int totalOnSpotify, String source) {
        this.link = link;
        this.name = name == null ? "" : name;
        this.owner = owner == null ? "" : owner;
        this.imageUrl = imageUrl;
        this.tracks = tracks == null ? Collections.emptyList() : Collections.unmodifiableList(tracks);
        this.totalOnSpotify = totalOnSpotify;
        this.source = source;
    }

    /** True if Spotify has more tracks but not all of them could be fetched. */
    public boolean isTruncated() {
        return totalOnSpotify > tracks.size();
    }
}
