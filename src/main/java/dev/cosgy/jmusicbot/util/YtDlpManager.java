package dev.cosgy.jmusicbot.util;

import dev.cosgy.jmusicbot.i18n.L;

import java.io.*;
import java.net.http.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class YtDlpManager {
    private static final Logger log = LoggerFactory.getLogger(YtDlpManager.class);

    private static final String GITHUB_LATEST_BASE =
            "https://github.com/yt-dlp/yt-dlp/releases/latest/download/";
    private static final String SHA256_FILE = "SHA2-256SUMS";
    private static final int PROC_TIMEOUT_SEC = 120;

    // Auto-update defaults (24 hours; first run delayed randomly by 5–30 min so startups don't collide)
    private static final Duration DEFAULT_UPDATE_INTERVAL = Duration.ofDays(1);
    private static final boolean AUTO_UPDATE_ENABLED =
            !"false".equalsIgnoreCase(System.getProperty("jmusicbot.ytdlp.autoUpdate", "true"));
    private static final String UPDATE_TO = System.getProperty("jmusicbot.ytdlp.updateTo", "").trim(); // "", "stable", "nightly", "2025.XX"

    // Ready flag and path (prevents running more than once)
    private static final AtomicBoolean prepared = new AtomicBoolean(false);
    private static volatile Path preparedPath = null;

    // Auto-update scheduler (only one per JVM)
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "yt-dlp-auto-update");
                t.setDaemon(true);
                return t;
            });
    private static volatile ScheduledFuture<?> updateFuture = null;
    private static final AtomicBoolean updatingNow = new AtomicBoolean(false);

    private final Path binDir;
    private final Path exePath;
    private final String assetName;

    public YtDlpManager(Path botDir) {
        log.debug(L.t("yt_dlp_manager.starting_ytdlpmanager_botdir"), botDir);
        this.binDir = botDir.resolve("bin");
        this.assetName = pickAssetForCurrentPlatform();
        this.exePath = binDir.resolve(assetNameForLocal(assetName));
        log.debug(L.t("yt_dlp_manager.executable_path"), exePath);
    }

    /** Prepares yt-dlp and returns its executable path (download/verify/permissions/update once) */
    public Path prepare() throws Exception {
        if (prepared.get() && preparedPath != null) {
            log.debug(L.t("yt_dlp_manager.yt_dlp_already_prepared"), preparedPath);
            // Start auto-update if it hasn't started
            if (AUTO_UPDATE_ENABLED) startAutoUpdateIfNeeded(DEFAULT_UPDATE_INTERVAL);
            return preparedPath;
        }

        synchronized (YtDlpManager.class) {
            if (prepared.get() && preparedPath != null) {
                if (AUTO_UPDATE_ENABLED) startAutoUpdateIfNeeded(DEFAULT_UPDATE_INTERVAL);
                return preparedPath;
            }

            log.info(L.t("yt_dlp_manager.preparing_yt_dlp"));
            Files.createDirectories(binDir);
            log.debug(L.t("yt_dlp_manager.bin_folder_created_checked"), binDir);

            boolean needDownload = !Files.isRegularFile(exePath);
            if (!needDownload) {
                log.debug(L.t("yt_dlp_manager.checking_existing_yt_dlp_file"));
                if (!isExecutableOk(exePath)) {
                    log.warn(L.t("yt_dlp_manager.existing_yt_dlp_broken_cannot"));
                    needDownload = true;
                }
            }

            if (needDownload) {
                log.info(L.t("yt_dlp_manager.downloading_yt_dlp"));
                downloadAndVerify();
                grantExecuteIfNeeded(exePath);
                log.info(L.t("yt_dlp_manager.yt_dlp_downloaded_verified"));
            } else {
                log.info(L.t("yt_dlp_manager.using_existing_yt_dlp"));
            }

            // Run check
            log.debug(L.t("yt_dlp_manager.checking_yt_dlp_version"));
            String version = runAndCapture(exePath.toString(), "--version").trim();
            if (version.isEmpty()) throw new IllegalStateException("yt-dlp launch failed");
            log.info(L.t("yt_dlp_manager.yt_dlp_version"), version);

            // Update manually once (continue even if it fails)
            try {
                log.info(L.t("yt_dlp_manager.yt_dlp_updating_itself"));
                runUpdateCommandWithTimeout(300, exePath);
                log.info(L.t("yt_dlp_manager.yt_dlp_update_finished"));
            } catch (Exception e) {
                log.warn(L.t("yt_dlp_manager.could_not_update_yt_dlp"), e.getMessage());
            }

            preparedPath = exePath;
            prepared.set(true);

            // Start auto-update
            if (AUTO_UPDATE_ENABLED) startAutoUpdateIfNeeded(DEFAULT_UPDATE_INTERVAL);
            else log.info(L.t("yt_dlp_manager.yt_dlp_auto_update_disabled"));

            log.info(L.t("yt_dlp_manager.yt_dlp_ready"), exePath);
            return exePath;
        }
    }

    // ————— Auto-update control —————

    /** Starts auto-update with the default interval (24 hours). Does nothing if already started */
    public synchronized void startAutoUpdate() {
        startAutoUpdateIfNeeded(DEFAULT_UPDATE_INTERVAL);
    }

    /** Starts auto-update with the given interval (e.g. Duration.ofHours(6)). Does nothing if already started */
    public synchronized void startAutoUpdate(Duration interval) {
        startAutoUpdateIfNeeded(interval != null ? interval : DEFAULT_UPDATE_INTERVAL);
    }

    /** Stops auto-update */
    public synchronized void stopAutoUpdate() {
        if (updateFuture != null) {
            updateFuture.cancel(false);
            updateFuture = null;
            log.info(L.t("yt_dlp_manager.yt_dlp_auto_update_stopped"));
        }
    }

    private void startAutoUpdateIfNeeded(Duration interval) {
        if (updateFuture != null && !updateFuture.isCancelled()) return;

        long periodSec = Math.max(60, interval.getSeconds()); // At least 60 seconds
        long initialDelaySec = ThreadLocalRandom.current().nextLong(300, 1800); // Spread between 5–30 minutes

        // Clean up on shutdown
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(this::stopAutoUpdate, "yt-dlp-auto-update-shutdown"));
        } catch (IllegalStateException ignored) { /* already shutting down */ }

        updateFuture = SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                if (!prepared.get()) {
                    // Prepare if not ready yet (does not collide with other threads)
                    try { prepare(); } catch (Exception e) {
                        log.warn(L.t("yt_dlp_manager.preparation_before_auto_update_failed"), e.toString());
                        return;
                    }
                }
                performSelfUpdate(); // Main work
            } catch (Throwable t) {
                log.warn(L.t("yt_dlp_manager.error_yt_dlp_auto_update"), t.toString());
            }
        }, initialDelaySec, periodSec, TimeUnit.SECONDS);

        log.info(L.t("yt_dlp_manager.yt_dlp_auto_update_started"), initialDelaySec, periodSec);
    }

    /** The actual update (prevents running concurrently) */
    private void performSelfUpdate() {
        if (!updatingNow.compareAndSet(false, true)) {
            log.debug(L.t("yt_dlp_manager.skipped_because_another_update_running"));
            return;
        }
        try {
            Path target = (preparedPath != null) ? preparedPath : exePath;
            if (!Files.isRegularFile(target)) {
                log.warn(L.t("yt_dlp_manager.no_yt_dlp_found_update"), target);
                return;
            }
            log.info(L.t("yt_dlp_manager.yt_dlp_checking_automatic_update"));
            runUpdateCommandWithTimeout(600, target); // At most 600 seconds for the auto-update (plenty of margin)
        } catch (Exception e) {
            log.warn(L.t("yt_dlp_manager.yt_dlp_auto_update_failed"), e.toString());
        } finally {
            updatingNow.set(false);
        }
    }

    private void runUpdateCommandWithTimeout(int timeoutSec, Path exe) throws Exception {
        if (UPDATE_TO.isEmpty()) {
            runAndCaptureWithProgressTimeout(timeoutSec, exe.toString(), "-U");
        } else {
            runAndCaptureWithProgressTimeout(timeoutSec, exe.toString(), "--update-to", UPDATE_TO);
        }
    }

    // ————— helpers —————

    /** Pick the file name for the current platform */
    private static String pickAssetForCurrentPlatform() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);

        log.debug(L.t("yt_dlp_manager.platform_detected_os_arch"), os, arch);
        if (os.contains("win")) {
            if (arch.contains("aarch64") || arch.contains("arm64")) {
                log.debug(L.t("yt_dlp_manager.windows_arm64_build_selected"));
                return "yt-dlp_arm64.exe";
            }
            log.debug(L.t("yt_dlp_manager.windows_build_selected"));
            return "yt-dlp.exe";
        } else if (os.contains("mac") || os.contains("darwin")) {
            log.debug(L.t("yt_dlp_manager.macos_build_selected"));
            return "yt-dlp_macos";
        } else {
            log.debug(L.t("yt_dlp_manager.linux_build_selected"));
            return "yt-dlp_linux";
        }
    }

    /** Local file name (fixed) */
    private static String assetNameForLocal(String asset) {
        return asset.endsWith(".exe") ? "yt-dlp.exe" : "yt-dlp";
    }

    /** Download, verify SHA256 and install */
    private void downloadAndVerify() throws Exception {
        log.info(L.t("yt_dlp_manager.downloading_yt_dlp_2"), assetName);
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

        // 1) Download the file
        URI binUri = URI.create(GITHUB_LATEST_BASE + assetName);
        log.debug(L.t("yt_dlp_manager.download_url"), binUri);
        // Create the temp file on the same drive/folder to avoid cross-drive move errors (e.g. AppData→D:\ etc.)
        Path tmp = Files.createTempFile(binDir, "yt-dlp-", ".dl");
        log.debug(L.t("yt_dlp_manager.temporary_file"), tmp);

        HttpResponse<InputStream> response = client.send(
                HttpRequest.newBuilder(binUri).GET().build(),
                HttpResponse.BodyHandlers.ofInputStream()
        );
        long totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(-1);

        try (InputStream in = response.body();
             OutputStream out = Files.newOutputStream(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            downloadWithProgress(in, out, totalBytes);
            log.info(L.t("yt_dlp_manager.download_finished"));
        }

        // 2) SHA256 verification
        log.info(L.t("yt_dlp_manager.verifying_sha256_checksum"));
        URI sumsUri = URI.create(GITHUB_LATEST_BASE + SHA256_FILE);
        String sums = client.send(HttpRequest.newBuilder(sumsUri).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
        String expected = parseSha256ForAsset(sums, assetName);

        if (expected != null) {
            log.debug("Beklenen SHA256: {}", expected);
            String actual = sha256Hex(tmp);
            log.debug(L.t("yt_dlp_manager.actual_sha256"), actual);
            if (!expected.equalsIgnoreCase(actual)) {
                Files.deleteIfExists(tmp);
                log.error(L.t("yt_dlp_manager.sha256_mismatch_expected_actual"), expected, actual);
                throw new SecurityException("SHA-256 mismatch for " + assetName);
            }
            log.info(L.t("yt_dlp_manager.sha256_verified"));
        } else {
            log.warn(L.t("yt_dlp_manager.sha256_checksum_not_found_skipping"));
        }

        // 3) Install
        log.debug(L.t("yt_dlp_manager.moving_yt_dlp_its_final"), tmp, exePath);
        try {
            // Replace atomically if possible (assumes the same file system).
            Files.move(tmp, exePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // If ATOMIC_MOVE fails because of a different drive/file system, retry with a normal MOVE
            log.debug(L.t("yt_dlp_manager.atomic_move_not_supported_retrying"), e.getMessage());
            Files.move(tmp, exePath, StandardCopyOption.REPLACE_EXISTING);
        }
        grantExecuteIfNeeded(exePath);
        log.info(L.t("yt_dlp_manager.yt_dlp_installed"));
    }

    /** Download while showing progress */
    private void downloadWithProgress(InputStream in, OutputStream out, long totalBytes) throws IOException {
        byte[] buffer = new byte[8192];
        long downloadedBytes = 0;
        int bytesRead;
        long lastProgressTime = System.currentTimeMillis();
        int lastProgress = -1;

        log.info(L.t("yt_dlp_manager.download_started_size"), "yt-dlp", formatBytes(totalBytes));

        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            downloadedBytes += bytesRead;

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastProgressTime >= 500) {
                if (totalBytes > 0) {
                    int progress = (int) ((downloadedBytes * 100) / totalBytes);
                    if (progress != lastProgress) {
                        String progressBar = createProgressBar(downloadedBytes, totalBytes);
                        log.info(L.t("yt_dlp_manager.download_progress"),
                                progressBar, progress, formatBytes(downloadedBytes), formatBytes(totalBytes));
                        lastProgress = progress;
                    }
                } else {
                    log.info(L.t("yt_dlp_manager.download_progress_size_unknown"), formatBytes(downloadedBytes));
                }
                lastProgressTime = currentTime;
            }
        }

        if (totalBytes > 0) {
            log.info(L.t("yt_dlp_manager.download_finished_100"), formatBytes(downloadedBytes), formatBytes(totalBytes));
        } else {
            log.info(L.t("yt_dlp_manager.download_finished_2"), formatBytes(downloadedBytes));
        }
    }

    private String createProgressBar(long current, long total) {
        if (total <= 0) return "[??????????]";
        int barLength = 20;
        int filled = (int) ((current * barLength) / total);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filled) bar.append("=");
            else if (i == filled) bar.append(">");
            else bar.append(" ");
        }
        bar.append("]");
        return bar.toString();
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return L.t("yt_dlp_manager.unknown");
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private static String parseSha256ForAsset(String sumsText, String asset) {
        log.debug(L.t("yt_dlp_manager.looking_up_checksum_sha256sums"), asset);
        for (String line : sumsText.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            int sp = t.indexOf(' ');
            if (sp > 0) {
                String hash = t.substring(0, sp).trim();
                String file = t.substring(t.lastIndexOf(' ') + 1).trim();
                if (file.equals(asset)) {
                    log.debug(L.t("yt_dlp_manager.matching_checksum_found"), hash);
                    return hash;
                }
            }
        }
        log.debug(L.t("yt_dlp_manager.no_checksum_found"), asset);
        return null;
    }

    private static String sha256Hex(Path p) throws Exception {
        log.debug(L.t("yt_dlp_manager.computing_sha256"), p);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(p)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) md.update(buf, 0, r);
        }
        byte[] b = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static void grantExecuteIfNeeded(Path p) throws IOException {
        try {
            log.debug(L.t("yt_dlp_manager.granting_execute_permission"), p);
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(p);
            if (!perms.contains(PosixFilePermission.OWNER_EXECUTE)) {
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                perms.add(PosixFilePermission.GROUP_EXECUTE);
                perms.add(PosixFilePermission.OTHERS_EXECUTE);
                Files.setPosixFilePermissions(p, perms);
                log.debug(L.t("yt_dlp_manager.execute_permission_granted"));
            } else {
                log.debug(L.t("yt_dlp_manager.execute_permission_already_present"));
            }
        } catch (UnsupportedOperationException ignored) {
            log.debug(L.t("yt_dlp_manager.posix_permissions_not_supported_windows"));
        }
    }

    // ==== Temp folder ====
    // yt-dlp_linux is a single-file (PyInstaller) program: on every run it unpacks itself (~40-80 MB)
    // into the temp folder (_MEIxxxx). On Pterodactyl /tmp is only a ~100 MB memory disk; when two yt-dlp
    // run at the same time or leftovers of interrupted processes pile up, /tmp fills up and yt-dlp
    // fails instantly with "exit code=255". So the temp folder is moved into the bot folder (on disk).
    private static volatile long lastCleanup = 0;

    /** yt-dlp's temp folder: &lt;bot folder&gt;/tmp */
    public static Path tempDir() {
        Path t = Paths.get("tmp").toAbsolutePath();
        try {
            Files.createDirectories(t);
        } catch (IOException ignored) {
        }
        return t;
    }

    /** Sets the environment variables for a yt-dlp process (temp folder and UTF-8). */
    public static ProcessBuilder applyEnv(ProcessBuilder pb) {
        String t = tempDir().toString();
        Map<String, String> env = pb.environment();
        env.put("TMPDIR", t);
        env.put("TMP", t);
        env.put("TEMP", t);
        env.put("PYTHONIOENCODING", "utf-8");
        cleanStaleTemp(false);
        return pb;
    }

    /** Deletes leftovers of interrupted yt-dlp runs (_MEI* folders). */
    public static void cleanStaleTemp(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastCleanup < 10 * 60_000L) return;
        lastCleanup = now;
        long freed = 0;
        for (Path dir : new Path[]{tempDir(), Paths.get(System.getProperty("java.io.tmpdir", "/tmp")), Paths.get("/tmp")}) {
            try (var st = Files.list(dir)) {
                for (Path p : (Iterable<Path>) st::iterator) {
                    String n = p.getFileName().toString();
                    if (!n.startsWith("_MEI") || !Files.isDirectory(p)) continue;
                    // Don't touch the folder of a running yt-dlp (15 min on forced cleanup, 30 min otherwise)
                    long age = now - Files.getLastModifiedTime(p).toMillis();
                    if (age < (force ? 15 * 60_000L : 30 * 60_000L)) continue;
                    freed += deleteTree(p);
                }
            } catch (Exception ignored) {
            }
        }
        if (freed > 0) log.info(L.t("yt_dlp_manager.cleaned_up_leftover_yt_dlp"), freed / (1024 * 1024));
    }

    private static long deleteTree(Path root) {
        long[] size = {0};
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    if (Files.isRegularFile(p)) size[0] += Files.size(p);
                    Files.deleteIfExists(p);
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
        return size[0];
    }

    private static boolean isExecutableOk(Path exe) {
        log.debug(L.t("yt_dlp_manager.checking_executable"), exe);
        try {
            String out = runAndCapture(exe.toString(), "--version");
            boolean ok = !out.isBlank();
            log.debug("Denetim sonucu: {}", ok ? "OK" : "NG");
            return ok;
        } catch (Exception e) {
            log.debug(L.t("yt_dlp_manager.error_during_check"), e.getMessage());
            return false;
        }
    }

    private static String runAndCapture(String... cmd) throws Exception {
        log.debug(L.t("yt_dlp_manager.running_command"), Arrays.toString(cmd));
        Process proc = applyEnv(new ProcessBuilder(cmd).redirectErrorStream(true)).start();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (InputStream in = proc.getInputStream()) {
            in.transferTo(bos);
        }
        if (!proc.waitFor(PROC_TIMEOUT_SEC, TimeUnit.SECONDS)) {
            proc.destroyForcibly();
            log.error(L.t("yt_dlp_manager.process_timed_out"), Arrays.toString(cmd));
            throw new RuntimeException("Process timeout: " + Arrays.toString(cmd));
        }
        String output = bos.toString(StandardCharsets.UTF_8);
        log.debug(L.t("yt_dlp_manager.command_finished_output_size_bytes"), output.length());
        return output;
    }

    /** Old API (default 120 seconds) */
    private static String runAndCaptureWithProgress(String... cmd) throws Exception {
        return runAndCaptureWithProgressTimeout(PROC_TIMEOUT_SEC, cmd);
    }

    /** Version with a configurable timeout */
    private static String runAndCaptureWithProgressTimeout(int timeoutSec, String... cmd) throws Exception {
        log.debug(L.t("yt_dlp_manager.running_command"), Arrays.toString(cmd));
        Process proc = applyEnv(new ProcessBuilder(cmd).redirectErrorStream(true)).start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if (!line.trim().isEmpty()) log.info("  {}", line.trim());
            }
        }

        if (!proc.waitFor(timeoutSec, TimeUnit.SECONDS)) {
            proc.destroyForcibly();
            log.error(L.t("yt_dlp_manager.process_timed_out"), Arrays.toString(cmd));
            throw new RuntimeException("Process timeout: " + Arrays.toString(cmd));
        }

        int exitCode = proc.exitValue();
        if (exitCode != 0) {
            log.warn(L.t("yt_dlp_manager.command_exited_with_code"), exitCode);
        }

        log.debug(L.t("yt_dlp_manager.command_finished_output_size_bytes"), output.length());
        return output.toString();
    }
}
