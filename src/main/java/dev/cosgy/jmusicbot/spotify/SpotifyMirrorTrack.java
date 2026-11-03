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

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Track that appears in the queue with Spotify info and, when its turn comes, finds the matching
 * YouTube video and plays it. This way even a 100-track playlist is added to the queue instantly.
 */
public class SpotifyMirrorTrack extends DelegatedAudioTrack {
    private static final Logger log = LoggerFactory.getLogger("Spotify");

    private final SpotifyTrackData data;
    private final SpotifySourceManager sourceManager;

    public SpotifyMirrorTrack(AudioTrackInfo trackInfo, SpotifyTrackData data, SpotifySourceManager sourceManager) {
        super(trackInfo);
        this.data = data;
        this.sourceManager = sourceManager;
    }

    public SpotifyTrackData getSpotifyData() {
        return data;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        AudioTrack match = sourceManager.findPlayable(data);
        if (!(match instanceof InternalAudioTrack)) {
            throw new FriendlyException(L.t("spotify_mirror_track.no_youtube_match_found_track") + data,
                    FriendlyException.Severity.COMMON, null);
        }
        String youtubeUrl = SpotifySourceManager.playableUrl(match);
        try {
            processDelegate((InternalAudioTrack) match, executor);
        } catch (Exception e) {
            // Retry with yt-dlp at the start of the track (when the YouTube stream can't be opened).
            if (Thread.currentThread().isInterrupted() || getPosition() > 5_000) throw e;
            log.warn(L.t("spotify_mirror_track.could_not_open_youtube_stream"), data, e.getMessage());
            AudioTrack local = sourceManager.loadViaYtDlp(youtubeUrl);
            if (!(local instanceof InternalAudioTrack)) throw e;
            processDelegate((InternalAudioTrack) local, executor);
        }
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new SpotifyMirrorTrack(trackInfo, data, sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }
}
