# JMusicBot-ML — Multi-Language JMusicBot

**English** · [Türkçe](README.tr.md) · [日本語](README.ja.md) · [Deutsch](README.de.md) · [Español](README.es.md) · [Русский](README.ru.md)

**JMusicBot-ML** (*ML = Multi-Language*) is an unofficial, multi-language edition of the well-known [JMusicBot](https://github.com/jagrosh/MusicBot): a self-hosted Discord music bot based on [JMusicBot-JP](https://github.com/Cosgy-Dev/JMusicBot-JP) 0.11.0, available in **6 languages** (English, Türkçe, 日本語, Deutsch, Español, Русский).

It plays music from YouTube, YouTube Music, **Spotify**, SoundCloud and internet radio. Installation is a single `.jar` file.

> **Based on:** [jagrosh/MusicBot](https://github.com/jagrosh/MusicBot) (JMusicBot, by John Grosh) → [Cosgy-Dev/JMusicBot-JP](https://github.com/Cosgy-Dev/JMusicBot-JP) (Japanese edition, by Cosgy Dev) → JMusicBot-ML.
> All credit for the original bot goes to those projects. JMusicBot-ML is an independent, modified version and is not affiliated with or endorsed by them. See [License and credits](#license-and-credits).

## Features

- **6 languages:** bot messages, `config.txt` comments and console logs in English, Turkish, Japanese, German, Spanish or Russian (`language` in config.txt). Slash command descriptions are shown to every user in **their own Discord language**.
- **Spotify:** track, album, playlist and artist links. Spotify doesn't provide audio, so the same track is found on YouTube Music / YouTube automatically and played from there. **No API key needed.**
- **"Now playing" card with buttons:** cover art, progress bar and ⏮ ⏸ ⏭ ⏹ 🔁 🔉 🔊 🔀 📜 🎤 buttons.
- **Lyrics** from [LRCLIB](https://lrclib.net) (no key needed).
- **YouTube radio / mix links:** up to 75 tracks are added without asking.
- **yt-dlp fallback:** if YouTube refuses a track, the bot downloads it with [yt-dlp](https://github.com/yt-dlp/yt-dlp) and plays it. yt-dlp is downloaded on first start and kept up to date.
- **Maintenance-free:** the downloaded-track cache and log files are size-limited; old ones are deleted automatically.

---

## Contents

1. [Requirements](#1-requirements)
2. [Creating the bot on Discord](#2-creating-the-bot-on-discord)
3. [Downloading the bot](#3-downloading-the-bot)
4. [First start and config.txt](#4-first-start-and-configtxt)
5. [Inviting the bot to your server](#5-inviting-the-bot-to-your-server)
6. [Running the bot](#6-running-the-bot) — Windows · Linux · Pterodactyl · Docker
7. [Optional settings](#7-optional-settings)
8. [Commands](#8-commands)
9. [Troubleshooting](#9-troubleshooting)
10. [Building from source](#10-building-from-source) · [Translations](#translations)

---

## 1. Requirements

- **Java 25** (64-bit). Older Java versions don't work.
- A Discord account and the **Manage Server** permission on the server you want to add the bot to.
- ~150 MB of free disk space (jar + yt-dlp) plus room for the cache (1 GB at most by default).
- 512 MB of RAM is enough.

## 2. Creating the bot on Discord

1. Go to https://discord.com/developers/applications → **New Application** → give it a name.
2. In the left menu open **Installation** → set *Install Link* to **None** and save.
   (Without this, the next step fails with "Private application cannot have a default authorization link".)
3. In the left menu open **Bot**:
   - Turn **Public Bot** **off** (only you can add the bot).
   - Under **Privileged Gateway Intents**, turn **MESSAGE CONTENT INTENT** **on** (needed for prefix commands like `!play`).
   - Click **Reset Token** and copy the **token**. Never share it — anyone with the token can control your bot.
4. Get your **Discord user ID**: Discord → Settings → Advanced → turn on **Developer Mode**, then right-click your name → **Copy User ID**.

## 3. Downloading the bot

Download the latest `JMusicBot-ML.jar` from the **Releases** section on the right of this page and put it into an **empty folder**. The bot writes its settings, cache and logs into this folder.

## 4. First start and config.txt

Start the bot once (see [section 6](#6-running-the-bot) for the command). A `config.txt` file is created in the folder. Open it with a text editor and fill in at least:

```
language = "en"                // en, tr, ja, de, es or ru
token = "BOT_TOKEN_HERE"       // the token from step 2
owner = OWNER_ID_HERE          // your Discord user ID (a number, no quotes)
prefix = "!"                   // for prefix commands (e.g. !play)
```

Save and restart the bot. When the console shows `Finished Loading!` and `Registered … slash commands with Discord`, the bot is ready.

> When you change `language` and restart, the comments in `config.txt` are rewritten in that language too. Your settings are kept and a backup (`config.txt.vX.bak`) is made. The same happens automatically when a new version adds settings.

## 5. Inviting the bot to your server

Replace `APPLICATION_ID` in the link below with your application's **Application ID** (Developer Portal → *General Information*) and open it in your browser:

```
https://discord.com/oauth2/authorize?client_id=APPLICATION_ID&scope=bot%20applications.commands&permissions=281475047352384
```

The link requests the permissions the bot needs (send messages, embed links, connect and speak in voice, change nickname, set voice channel status…).

## 6. Running the bot

The command is the same everywhere:

```
java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
```

### Windows

Open Command Prompt (cmd) and install Java 25:

```cmd
winget install EclipseAdoptium.Temurin.25.JRE
```

Then go to the folder with the jar and start the bot:

```cmd
cd C:\Users\YOURNAME\Desktop\jmusicbot
java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
```

You can put this line into a `start.bat` file in the same folder and double-click it. Without `-Dnogui=true` the bot opens a small window.

### Linux (Ubuntu / Debian)

```bash
sudo apt update
sudo apt install openjdk-25-jre-headless
mkdir ~/jmusicbot && cd ~/jmusicbot     # put JMusicBot-ML.jar here
java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
```

If your package repository has no Java 25, use [Adoptium Temurin 25](https://adoptium.net/).

To start the bot automatically at boot, create `/etc/systemd/system/jmusicbot.service` (replace `YOURUSER`):

```ini
[Unit]
Description=JMusicBot-ML music bot
After=network-online.target

[Service]
User=YOURUSER
WorkingDirectory=/home/YOURUSER/jmusicbot
ExecStart=/usr/bin/java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable --now jmusicbot
journalctl -u jmusicbot -f     # follow the console
```

### Pterodactyl

1. In the panel go to **Admin → Nests → Import Egg** and import [`pterodactyl/egg-jmusicbot-ml.json`](pterodactyl/egg-jmusicbot-ml.json) from this repository.
2. Create a new server with this egg (512 MB RAM and 2 GB disk are enough). Docker image: `ghcr.io/pterodactyl/yolks:java_25`.
3. Upload `JMusicBot-ML.jar` with the server's **File Manager**.
4. Click **Start**. `config.txt` is created on first start. Fill in `token`, `owner` and `language` in the File Manager and restart.

To update, just replace the jar file and restart.

### Docker

Put `JMusicBot-ML.jar` next to the `Dockerfile` in this repository:

```bash
docker build -t jmusicbot-ml .
docker run -d --name jmusicbot-ml --restart unless-stopped -v "$(pwd)/data:/opt/jmusicbot-ml/data" jmusicbot-ml
docker logs -f jmusicbot-ml
```

`config.txt` and the other files are kept in the `data` folder.

## 7. Optional settings

Everything is in `config.txt`, and every setting is explained in the file itself. The most important ones:

| Setting | Default | What it does |
|---|---|---|
| `language` | `"en"` | Bot language: `en`, `tr`, `ja`, `de`, `es`, `ru`. |
| `spclient`, `spsecret` | empty | Spotify API keys. **Not required**; without them Spotify's public page is used. With them, matching is a bit more accurate. |
| `youtubeoauth2` | `false` | Sign in to YouTube with a Google account (for age-restricted videos). See below. |
| `failnotice` | `"none"` | Who is told when a track can't be played at all: `"none"` (nobody, console only), `"dm"` (DM to the requester), `"channel"` (in the channel). |
| `cachelimitmb` | `1024` | Maximum size (MB) of the `cache` folder where downloaded tracks are kept. When exceeded, the least recently played tracks are deleted. |
| `altprefix` | `"NONE"` | A second prefix (e.g. `"?"`). |

### Spotify keys (optional)

1. https://developer.spotify.com/dashboard → **Create app**.
2. Enter `https://example.com` as the Redirect URI and select **Web API**.
3. Copy **Client ID** and **Client secret** from the *Settings* page into `spclient` and `spsecret`.

Since 2026 Spotify requires the app owner to have **Premium** for developer apps. If you don't have Premium, leave these empty; the bot still works. Other people's playlists are always read from the public page (at most 100 tracks per list).

### YouTube account (only for age-restricted videos)

Normal videos play without an account. YouTube's **18+** videos only open with a signed-in account.

1. Set `youtubeoauth2 = true` in `config.txt` and restart the bot.
2. A line `go to https://www.google.com/device and enter code XXX-XXX-XXX` appears in the console.
3. Open that page, enter the code and choose the account. The bot detects the sign-in by itself and saves the token to `youtube-oauth.txt`; no sign-in is needed again.

> ⚠️ **Don't use your main account.** Create a separate Google account just for the bot (its birth date must be 18+). YouTube's terms don't allow automated access; the account may be restricted. Never share `youtube-oauth.txt`. You can remove the connection at any time in *Google Account → Security → Third-party connections*.

Alternative: put a Netscape-format `cookies.txt` into the bot folder and the yt-dlp fallback downloads with that account. A cookie file gives access to the account's whole Google session, so it is riskier.

## 8. Commands

Every command works with `/` or with the prefix. Type `/help` for the full list. Turkish command names (e.g. `/oynat`) are also available for users whose Discord is in Turkish.

| Command | Description |
|---|---|
| `/play` | Plays a track name or a YouTube / Spotify / SoundCloud link |
| `/spotify` | Plays a Spotify link with an info card |
| `/search` | Searches YouTube and lets you pick |
| `/nowplaying` | "Now playing" card with buttons |
| `/queue` | Shows the queue |
| `/skip` | Votes to skip |
| `/lyrics` | Lyrics (`/lyrics Coldplay - Yellow` works too) |
| `/volume` | Volume (0-150) |
| `/shuffle` | Shuffles your tracks |
| `/seek` | Seeks forward / back |
| `/remove` | Removes a track from the queue |
| `/pause` | Pauses (DJ) |
| `/stop` | Stops and clears the queue (DJ) |
| `/forceskip` | Skips without a vote (DJ) |
| `/playnext` | Adds to the front of the queue (DJ) |
| `/repeat` | Repeat mode (DJ) |
| `/skipto` | Skips to a track in the queue (DJ) |
| `/movetrack` | Moves a track in the queue (DJ) |
| `/playlists` | Server playlists |
| `/mylist` | Personal playlists |
| `/settings` | Shows the server settings |
| `/setdj` | Sets the DJ role (Admin) |
| `/settc` | Restricts commands to a channel (Admin) |
| `/help` | Lists all commands |

## 9. Troubleshooting

| Symptom | Fix |
|---|---|
| Bot doesn't start, `UnsupportedClassVersionError` | Your Java is too old. `java -version` must show **25**. |
| Console shows `No token entered` / `Invalid token` | Check `token` in `config.txt` (inside quotes, no spaces). |
| Public Bot can't be turned off in the Developer Portal | First set **Installation → Install Link = None**. |
| Slash commands don't show up | Restart Discord (Ctrl+R). The invite link must include `applications.commands`. |
| `!play` doesn't work but `/play` does | **MESSAGE CONTENT INTENT** must be on in the Developer Portal. |
| Bot joins and leaves immediately | Check the `Could not open YouTube stream` and `yt-dlp error:` lines in the console. If it says `Sign in to confirm your age`, the video is age-restricted ([section 7](#youtube-account-only-for-age-restricted-videos)). |
| `yt-dlp exit code=255` | yt-dlp couldn't start (disk may be full). The bot cleans up and retries once; free up disk space. |
| Spotify link doesn't play | The link must be public. Podcast links are not supported. |

When reporting a bug, include the relevant console lines and the bot version (the `JMusicBot-ML vX.Y.Z` banner at startup). **Never share your token or `config.txt`.**

## 10. Building from source

Requires JDK 25:

```bash
./mvnw -B -DskipTests package
# output: target/JMusicBot-ML-All.jar
```

On Windows use `mvnw.cmd -B -DskipTests package`. When a tag starting with `v` (e.g. `v1.0.1`) is pushed, GitHub Actions builds the jar and attaches it to a **Release** automatically.

### Translations

All texts are in [`src/main/resources/lang/`](src/main/resources/lang/): `en.json`, `tr.json`, `ja.json`, `de.json`, `es.json`, `ru.json`, and the `config.txt` templates in `lang/config/`. English is the fallback for missing keys. To add a language, copy `en.json` and `config/en.conf`, translate the values, and add the language code to `LANGUAGES` in [`L.java`](src/main/java/dev/cosgy/jmusicbot/i18n/L.java). Fixes to the translations are very welcome.

Changelog: [CHANGELOG.md](CHANGELOG.md)

## License and credits

JMusicBot-ML is released under the [Apache License 2.0](LICENSE), the same license as the projects it is based on.

### Upstream projects

| Project | Author | What it is |
|---|---|---|
| [jagrosh/MusicBot](https://github.com/jagrosh/MusicBot) | John Grosh (jagrosh) | The original JMusicBot |
| [Cosgy-Dev/JMusicBot-JP](https://github.com/Cosgy-Dev/JMusicBot-JP) | Cosgy Dev | Japanese edition (version 0.11.0 is the base of JMusicBot-ML) |

The original copyright notices are kept in the source files. Files and changes added by JMusicBot-ML are marked "JMusicBot-ML contributors". What was changed is listed in [CHANGELOG.md](CHANGELOG.md) and summarised in [NOTICE](NOTICE): translations into 6 languages, the rewritten Spotify support, the "now playing" card with buttons, the new lyrics source, the yt-dlp fallback, cache and log limits, and many bug fixes.

### Libraries and services

[JDA](https://github.com/discord-jda/JDA) · [Lavaplayer](https://github.com/lavalink-devs/lavaplayer) · [youtube-source](https://github.com/lavalink-devs/youtube-source) · [yt-dlp](https://github.com/yt-dlp/yt-dlp) (downloaded at runtime) · [JDAVE](https://github.com/MinnDevelopment/jdave) · [Logback](https://logback.qos.ch) · [Typesafe Config](https://github.com/lightbend/config) · [jsoup](https://jsoup.org) · [Unirest](https://github.com/Kong/unirest-java) · [JFiglet](https://github.com/lalyos/jfiglet) · [JLyrics](https://github.com/Cosgy-Dev/JLyrics) · lyrics by [LRCLIB](https://lrclib.net). Each is used under its own license; see [NOTICE](NOTICE).

### Made with Claude

JMusicBot-ML was developed with the help of **[Claude](https://claude.ai)**, an AI assistant made by Anthropic. Claude wrote most of the changes in this edition — the translations into six languages, the new Spotify support, the "now playing" card, the yt-dlp fallback and the bug fixes — while the project owner tested every version on a real server and decided what to build. Commits made with Claude are marked with `Co-Authored-By: Claude`.

This bot is not affiliated with or endorsed by YouTube, Spotify or Discord. Run it on your own server and follow the terms of the services involved.
