# Changelog

## JMusicBot-ML v1.0.0 — first public release
- **Multi-language:** the bot speaks English (default), Turkish, Japanese, German, Spanish and Russian. Select it with `language` in config.txt; all bot messages, console logs and the comments in config.txt follow it.
- Slash command descriptions are registered in all six languages, so every user sees them in their own Discord language. Turkish command names (`/oynat` for `/play`…) are shown to Turkish Discord users; with `language = "tr"` they are also registered as separate commands.
- Changing `language` rewrites config.txt in that language (settings are kept, a backup is made). Existing Turkish config.txt files from the development versions continue with `language = "tr"` automatically (config.txt v7).
- Spotify track / album / playlist / artist links without an API key, "now playing" card with buttons, lyrics from LRCLIB, YouTube radio/mix support (up to 75 tracks), yt-dlp fallback, optional YouTube sign-in, cache and log size limits — see the development versions below.
- Code comments, GitHub templates, Dockerfile and the Pterodactyl egg in English; README in six languages.
- README and NOTICE credit the upstream projects and mention that the project was developed with the help of Claude (Anthropic).

## Development versions before 1.0 (v0.1.x, private Turkish edition)
- v0.1.11 — GitHub release preparation (README, GitHub Actions build, Pterodactyl egg, .gitignore); disabled the upstream update check.
- v0.1.10 — `failnotice`: messages about unplayable tracks no longer go to the channel by default (none / dm / channel).
- v0.1.9 — quieter console (no JSON dumps or stack traces from the YouTube library); log files are rotated, compressed and deleted after 7 days / 100 MB.
- v0.1.8 — configurable cache limit (`cachelimitmb`).
- v0.1.7 — fixed `yt-dlp exit code=255` on Pterodactyl (temp folder moved to disk, leftover cleanup, retry).
- v0.1.6 — Google account sign-in for YouTube actually works (TV client), `cookies.txt` support, clear reasons for unplayable tracks.
- v0.1.5 — fixed lost titles and instability of the yt-dlp fallback; YouTube radio playlists capped at 75 tracks.
- v0.1.4 — `stop` really stops background jobs; the old list no longer mixes into new tracks.
- v0.1.3 — the yt-dlp fallback no longer gets stuck; downloaded tracks are cached.
- v0.1.2 — playlist/radio links are added without confirmation.
- v0.1.1 — new lyrics source (LRCLIB), "now playing" card with buttons, previous-track history.
- v0.1.0 — full Turkish translation, Turkish command names, rewritten Spotify support, many bug fixes, jar size reduced from ~910 MB to ~66 MB.

See [DEGISIKLIKLER.md](DEGISIKLIKLER.md) for the detailed (Turkish) history.
