package com.sedmelluq.discord.lavaplayer.source.nico;

import dev.cosgy.jmusicbot.i18n.L;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDescriptor;
import com.sedmelluq.discord.lavaplayer.container.playlists.ExtendedM3uParser;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalSeekableInputStream;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio track that handles processing NicoNico tracks.
 */
public class NicoAudioTrack extends DelegatedAudioTrack {
    private static final Logger log = LoggerFactory.getLogger(NicoAudioTrack.class);
    private final MediaContainerDescriptor containerTrackFactory;
    private final NicoAudioSourceManager sourceManager;

    /**
     * @param trackInfo     Track info
     * @param sourceManager Source manager which was used to find this track
     */
    public NicoAudioTrack(AudioTrackInfo trackInfo, NicoAudioSourceManager sourceManager, MediaContainerDescriptor containerTrackFactory) {
        super(trackInfo);

        this.containerTrackFactory = containerTrackFactory;
        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
        File playbackUrl = downloadAudio(NicoAudioSourceManager.ytDlpPath);

        log.debug("Starting NicoNico track from URL: {}", playbackUrl);
        try (LocalSeekableInputStream inputStream = new LocalSeekableInputStream(playbackUrl)) {
            processDelegate((InternalAudioTrack) containerTrackFactory.createTrack(trackInfo, inputStream), localExecutor);
        }
    }

    //
    private @NotNull File downloadAudio(@NotNull Path ytDlpPath) {
        try {
            // Root and cache folder
            Path botRoot = Paths.get("").toAbsolutePath().normalize();
            Path cacheDir = botRoot.resolve("cache");
            Files.createDirectories(cacheDir);

            // Output file (absolute path)
            String id = getIdentifier(); // Format expected by the existing method
            Path outFile = cacheDir.resolve(id + ".wav");

            // Return as is if it already exists and is not empty
            if (Files.isRegularFile(outFile) && Files.size(outFile) > 0) {
                return outFile.toFile();
            }

            log.info("Downloading NicoNico track via yt-dlp: id={} -> {}", id, outFile);

            // Build the command (the absolute path of the yt-dlp file we downloaded is used)
            List<String> cmd = new ArrayList<>();
            cmd.add(ytDlpPath.toString());

            // Options for stable operation
            Collections.addAll(cmd,
                    "--no-progress",
                    "--no-playlist",
                    "--ignore-config",
                    "--newline",
                    "--restrict-filenames",
                    "--no-overwrites" // Keep the existing file (skip if present)
            );

            // Login (optional)
            if (NicoAudioSourceManager.userName != null && NicoAudioSourceManager.password != null) {
                cmd.add("--username");
                cmd.add(NicoAudioSourceManager.userName);
                cmd.add("--password");
                cmd.add(NicoAudioSourceManager.password);
                log.info(L.t("nico_audio_track.niconico_login_credentials_used"));

                // TOTP (two-factor authentication)
                if (NicoAudioSourceManager.twofactor != null) {
                    String code = TOTPGenerator.getCode(NicoAudioSourceManager.twofactor);
                    if (code != null && code.matches("\\d{6}")) {
                        cmd.add("--twofactor");
                        cmd.add(code);
                        log.info(L.t("nico_audio_track.two_factor_authentication_done"), code);
                    } else {
                        log.warn(L.t("nico_audio_track.invalid_two_factor_authentication_code"), code);
                    }
                }
            }

            // Audio extraction (WAV / best quality)
            Collections.addAll(cmd,
                    "--extract-audio",
                    "--audio-format", "wav",
                    "--audio-quality", "0"
            );

            // The output path must be absolute (a relative path can shift depending on the environment)
            cmd.add("--output");
            cmd.add(outFile.toString());

            // Hedef URL
            cmd.add("https://www.nicovideo.jp/watch/" + id);

            // Run the process: merge stderr and stdout so it's readable
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(botRoot.toFile());
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            StringBuilder logBuf = new StringBuilder(4096);
            try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    logBuf.append(line).append('\n');
                    // At DEBUG level because yt-dlp progress output is long
                    log.debug(line);
                }
            }

            // Timeout (change if needed)
            boolean finished = proc.waitFor(300, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                throw new RuntimeException("yt-dlp timed out (300s).");
            }

            int code = proc.exitValue();
            if (code != 0) {
                // Clean up partial files on failure
                safeDeleteIfEmpty(outFile);
                String tail = tailOf(logBuf, 2000);
                throw new RuntimeException("yt-dlp failed with exit code " + code + "\n--- output ---\n" + tail);
            }

            // Even on success, 0-byte files are treated as errors
            if (!Files.isRegularFile(outFile) || Files.size(outFile) == 0) {
                safeDeleteIfEmpty(outFile);
                String tail = tailOf(logBuf, 2000);
                throw new RuntimeException("yt-dlp finished but output not found or empty.\n--- output ---\n" + tail);
            }

            return outFile.toFile();

        } catch (Exception e) {
            throw new RuntimeException("Failed to download audio via yt-dlp", e);
        }
    }

// --- helpers ---

    private static void safeDeleteIfEmpty(Path p) {
        try {
            if (Files.isRegularFile(p) && Files.size(p) == 0) {
                Files.deleteIfExists(p);
            }
        } catch (IOException ignored) {}
    }

    private static String tailOf(CharSequence sb, int max) {
        int len = sb.length();
        if (len <= max) return sb.toString();
        return sb.subSequence(len - max, len).toString();
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new NicoAudioTrack(trackInfo, sourceManager, containerTrackFactory);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }
}