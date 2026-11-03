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

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;

import java.util.List;

/**
 * Spotify album / playlist / artist. Carries extra info (cover, owner).
 */
public class SpotifyAudioPlaylist extends BasicAudioPlaylist {
    private final SpotifyCollection collection;

    public SpotifyAudioPlaylist(SpotifyCollection collection, List<AudioTrack> tracks, boolean isSearchResult) {
        super(collection.name, tracks, null, isSearchResult);
        this.collection = collection;
    }

    public SpotifyCollection getCollection() {
        return collection;
    }
}
