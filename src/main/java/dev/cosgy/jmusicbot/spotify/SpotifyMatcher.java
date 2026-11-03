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

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Compares a Spotify track with a YouTube search result and gives it a score.
 * The higher the score, the better the match.
 */
public final class SpotifyMatcher {
    private SpotifyMatcher() {
    }

    /** Words that get a penalty in the YouTube result if they are not in the Spotify title. */
    private static final String[] UNWANTED = {
            "live", "canli", "cover", "karaoke", "remix", "nightcore", "sped up", "speed up", "slowed",
            "reverb", "8d", "instrumental", "enstrumantal", "acoustic", "akustik", "bass boosted", "tiktok",
            "1 hour", "10 hours", "loop", "reaction", "tutorial", "piano"
    };

    private static final Pattern FEAT = Pattern.compile("\\s*[(\\[](?:feat\\.?|ft\\.?|featuring|with)\\s[^)\\]]*[)\\]]", Pattern.CASE_INSENSITIVE);
    private static final Pattern REMASTER = Pattern.compile("\\s+-\\s+(?:[^-]*\\b(?:remaster(?:ed)?|version|edit|mono|stereo|mix)\\b.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    /** Strips suffixes such as "(feat. X)" and "- 2011 Remaster" from the title. */
    public static String cleanTitle(String title) {
        if (title == null) return "";
        String t = FEAT.matcher(title).replaceAll("");
        t = REMASTER.matcher(t).replaceAll("");
        return t.trim();
    }

    /** Lowercases and removes accents and punctuation (including Turkish characters). */
    public static String normalize(String s) {
        if (s == null) return "";
        String t = s.toLowerCase(Locale.ROOT)
                .replace('ı', 'i').replace('ğ', 'g').replace('ş', 's')
                .replace('ç', 'c').replace('ö', 'o').replace('ü', 'u');
        t = Normalizer.normalize(t, Normalizer.Form.NFD);
        t = MARKS.matcher(t).replaceAll("");
        t = NON_ALNUM.matcher(t).replaceAll(" ");
        return t.trim().replaceAll("\\s+", " ");
    }

    private static Set<String> tokens(String normalized) {
        Set<String> set = new HashSet<>(Arrays.asList(normalized.split(" ")));
        set.remove("");
        return set;
    }

    /**
     * @param candTitle   title of the YouTube result
     * @param candAuthor  channel name of the YouTube result
     * @param candLength  duration of the YouTube result (ms, <= 0 if unknown)
     * @param track       the Spotify track
     */
    public static double score(String candTitle, String candAuthor, long candLength, SpotifyTrackData track) {
        String ct = normalize(candTitle);
        String ca = normalize(candAuthor);
        String tt = normalize(cleanTitle(track.title));
        String ta = normalize(track.primaryArtist);

        double score = 0;

        // How many of the title words appear in the YouTube title?
        Set<String> titleTokens = tokens(tt);
        if (!titleTokens.isEmpty()) {
            Set<String> candTokens = tokens(ct + " " + ca);
            int hit = 0;
            for (String tok : titleTokens) if (candTokens.contains(tok)) hit++;
            score += 40.0 * hit / titleTokens.size();
        }
        if (!tt.isEmpty() && ct.contains(tt)) score += 15;

        // Artist
        if (!ta.isEmpty()) {
            if (ca.contains(ta) || (ca.length() > 3 && ta.contains(ca))) score += 25;
            else if (ct.contains(ta)) score += 20;
            else {
                // Small bonus if at least one word of the artist name appears
                Set<String> at = tokens(ta);
                Set<String> cand = tokens(ct + " " + ca);
                for (String tok : at) {
                    if (tok.length() > 2 && cand.contains(tok)) {
                        score += 8;
                        break;
                    }
                }
            }
        }
        if (ca.endsWith(" topic")) score += 5; // "Artist - Topic" = official audio recording

        // Duration match (the most important criterion)
        if (track.durationMs > 0 && candLength > 0) {
            long diff = Math.abs(candLength - track.durationMs);
            if (diff <= 2_000) score += 30;
            else if (diff <= 5_000) score += 22;
            else if (diff <= 10_000) score += 10;
            else if (diff <= 30_000) score -= 10;
            else score -= 40;
        }

        // Unwanted versions
        String rawTrackTitle = normalize(track.title);
        for (String w : UNWANTED) {
            if (containsWord(ct, w) && !containsWord(rawTrackTitle, w)) score -= 18;
        }
        return score;
    }

    /** Duration difference (ms). 0 if the durations are unknown. */
    public static long durationDiff(long candLength, SpotifyTrackData track) {
        if (track.durationMs <= 0 || candLength <= 0) return 0;
        return Math.abs(candLength - track.durationMs);
    }

    private static boolean containsWord(String haystack, String word) {
        return (" " + haystack + " ").contains(" " + word + " ");
    }
}
