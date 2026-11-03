# JMusicBot-ML — Mehrsprachiger JMusicBot

[English](README.md) · [Türkçe](README.tr.md) · [日本語](README.ja.md) · **Deutsch** · [Español](README.es.md) · [Русский](README.ru.md)

**JMusicBot-ML** (*ML = Multi-Language, mehrsprachig*) ist eine inoffizielle, mehrsprachige Ausgabe des bekannten [JMusicBot](https://github.com/jagrosh/MusicBot): ein selbst gehosteter Discord-Musikbot auf Basis von [JMusicBot-JP](https://github.com/Cosgy-Dev/JMusicBot-JP) 0.11.0, verfügbar in **6 Sprachen** (English, Türkçe, 日本語, Deutsch, Español, Русский).

Er spielt Musik von YouTube, YouTube Music, **Spotify**, SoundCloud und Internetradio. Die Installation besteht aus einer einzigen `.jar`-Datei.

> **Basiert auf:** [jagrosh/MusicBot](https://github.com/jagrosh/MusicBot) (JMusicBot, von John Grosh) → [Cosgy-Dev/JMusicBot-JP](https://github.com/Cosgy-Dev/JMusicBot-JP) (japanische Ausgabe, von Cosgy Dev) → JMusicBot-ML.
> Der Verdienst für den ursprünglichen Bot gebührt diesen Projekten. JMusicBot-ML ist eine unabhängige, veränderte Version und steht in keiner Verbindung zu ihnen. Siehe [Lizenz und Danksagung](#s-license).

## Funktionen

- **6 Sprachen:** Bot-Nachrichten, Kommentare in `config.txt` und Konsolenausgaben auf Englisch, Türkisch, Japanisch, Deutsch, Spanisch oder Russisch (`language` in config.txt). Die Beschreibungen der Slash-Befehle sieht jeder Nutzer in **seiner eigenen Discord-Sprache**.
- **Spotify:** Links zu Titeln, Alben, Playlists und Künstlern. Spotify liefert kein Audio, daher wird derselbe Titel automatisch auf YouTube Music / YouTube gefunden und von dort abgespielt. **Kein API-Schlüssel nötig.**
- **„Läuft gerade“-Karte mit Tasten:** Cover, Fortschrittsbalken und die Tasten ⏮ ⏸ ⏭ ⏹ 🔁 🔉 🔊 🔀 📜 🎤.
- **Songtexte** von [LRCLIB](https://lrclib.net) (kein Schlüssel nötig).
- **YouTube-Radio-/Mix-Links:** bis zu 75 Titel werden ohne Rückfrage hinzugefügt.
- **yt-dlp als Ausweichlösung:** Wenn YouTube einen Titel verweigert, lädt der Bot ihn mit [yt-dlp](https://github.com/yt-dlp/yt-dlp) herunter und spielt ihn ab. yt-dlp wird beim ersten Start heruntergeladen und aktuell gehalten.
- **Wartungsfrei:** Der Cache für heruntergeladene Titel und die Logdateien sind größenbegrenzt; alte Dateien werden automatisch gelöscht.

---

## Inhalt

1. [Voraussetzungen](#s1)
2. [Den Bot bei Discord erstellen](#s2)
3. [Den Bot herunterladen](#s3)
4. [Erster Start und config.txt](#s4)
5. [Den Bot auf deinen Server einladen](#s5)
6. [Den Bot ausführen](#s6) — Windows · Linux · Pterodactyl · Docker
7. [Optionale Einstellungen](#s7)
8. [Befehle](#s8)
9. [Fehlerbehebung](#s9)
10. [Aus dem Quellcode bauen](#s10) · [Übersetzungen](#s-translations)

---

<a id="s1"></a>
## 1. Voraussetzungen

- **Java 25** (64 Bit). Ältere Java-Versionen funktionieren nicht.
- Ein Discord-Konto und die Berechtigung **Server verwalten** auf dem Server, auf dem der Bot laufen soll.
- ~150 MB freier Speicher (jar + yt-dlp) plus Platz für den Cache (standardmäßig höchstens 1 GB).
- 512 MB RAM reichen aus.

<a id="s2"></a>
## 2. Den Bot bei Discord erstellen

1. Öffne https://discord.com/developers/applications → **New Application** → gib einen Namen ein.
2. Öffne im linken Menü **Installation** → setze *Install Link* auf **None** und speichere.
   (Ohne diesen Schritt schlägt der nächste mit „Private application cannot have a default authorization link“ fehl.)
3. Öffne im linken Menü **Bot**:
   - Schalte **Public Bot** **aus** (nur du kannst den Bot hinzufügen).
   - Schalte unter **Privileged Gateway Intents** den **MESSAGE CONTENT INTENT** **ein** (nötig für Präfix-Befehle wie `!play`).
   - Klicke auf **Reset Token** und kopiere den **Token**. Gib ihn niemals weiter — wer den Token hat, kann deinen Bot steuern.
4. Hole dir deine **Discord-Benutzer-ID**: Discord → Einstellungen → Erweitert → **Entwicklermodus** einschalten, dann Rechtsklick auf deinen Namen → **Benutzer-ID kopieren**.

<a id="s3"></a>
## 3. Den Bot herunterladen

Lade die neueste `JMusicBot-ML.jar` aus dem Bereich **Releases** rechts auf dieser Seite herunter und lege sie in einen **leeren Ordner**. Der Bot schreibt seine Einstellungen, den Cache und die Logs in diesen Ordner.

<a id="s4"></a>
## 4. Erster Start und config.txt

Starte den Bot einmal (den Befehl findest du in [Abschnitt 6](#s6)). Im Ordner entsteht eine `config.txt`. Öffne sie mit einem Texteditor und trage mindestens Folgendes ein:

```
language = "de"                // en, tr, ja, de, es oder ru
token = "BOT_TOKEN_HIER"       // der Token aus Schritt 2
owner = BESITZER_ID_HIER       // deine Discord-Benutzer-ID (eine Zahl, ohne Anführungszeichen)
prefix = "!"                   // für Präfix-Befehle (z. B. !play)
```

Speichere und starte den Bot neu. Sobald die Konsole `Finished Loading!` und `… Slash-Befehle bei Discord registriert` zeigt, ist der Bot bereit.

> Die Datei wird beim ersten Start auf Englisch erstellt. Wenn du `language = "de"` einträgst und neu startest, werden auch die Kommentare in `config.txt` auf Deutsch neu geschrieben. Deine Einstellungen bleiben erhalten, und es wird eine Sicherung (`config.txt.vX.bak`) angelegt. Dasselbe passiert automatisch, wenn eine neue Version Einstellungen hinzufügt.

<a id="s5"></a>
## 5. Den Bot auf deinen Server einladen

Ersetze im folgenden Link `APPLICATION_ID` durch die **Application ID** deiner Anwendung (Developer Portal → *General Information*) und öffne ihn im Browser:

```
https://discord.com/oauth2/authorize?client_id=APPLICATION_ID&scope=bot%20applications.commands&permissions=281475047352384
```

Der Link fordert die Berechtigungen an, die der Bot braucht (Nachrichten senden, Links einbetten, im Sprachkanal verbinden und sprechen, Spitznamen ändern, Sprachkanalstatus setzen …).

<a id="s6"></a>
## 6. Den Bot ausführen

Der Befehl ist überall gleich:

```
java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
```

### Windows

Öffne die Eingabeaufforderung (cmd) und installiere Java 25:

```cmd
winget install EclipseAdoptium.Temurin.25.JRE
```

Wechsle dann in den Ordner mit der jar-Datei und starte den Bot:

```cmd
cd C:\Users\DEINNAME\Desktop\jmusicbot
java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
```

Du kannst diese Zeile in eine `start.bat` im selben Ordner schreiben und sie per Doppelklick starten. Ohne `-Dnogui=true` öffnet der Bot ein kleines Fenster.

### Linux (Ubuntu / Debian)

```bash
sudo apt update
sudo apt install openjdk-25-jre-headless
mkdir ~/jmusicbot && cd ~/jmusicbot     # JMusicBot-ML.jar hierher legen
java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
```

Wenn deine Paketquelle kein Java 25 hat, nimm [Adoptium Temurin 25](https://adoptium.net/).

Damit der Bot beim Hochfahren automatisch startet, lege `/etc/systemd/system/jmusicbot.service` an (ersetze `DEINUSER`):

```ini
[Unit]
Description=JMusicBot-ML music bot
After=network-online.target

[Service]
User=DEINUSER
WorkingDirectory=/home/DEINUSER/jmusicbot
ExecStart=/usr/bin/java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable --now jmusicbot
journalctl -u jmusicbot -f     # Konsole verfolgen
```

### Pterodactyl

1. Gehe im Panel zu **Admin → Nests → Import Egg** und importiere [`pterodactyl/egg-jmusicbot-ml.json`](pterodactyl/egg-jmusicbot-ml.json) aus diesem Repository.
2. Erstelle mit diesem Egg einen neuen Server (512 MB RAM und 2 GB Speicher reichen). Docker-Image: `ghcr.io/pterodactyl/yolks:java_25`.
3. Lade `JMusicBot-ML.jar` über den **File Manager** des Servers hoch.
4. Klicke auf **Start**. Beim ersten Start entsteht `config.txt`. Trage im File Manager `token`, `owner` und `language` ein und starte neu.

Zum Aktualisieren ersetzt du einfach die jar-Datei und startest neu.

### Docker

Lege `JMusicBot-ML.jar` neben das `Dockerfile` aus diesem Repository:

```bash
docker build -t jmusicbot-ml .
docker run -d --name jmusicbot-ml --restart unless-stopped -v "$(pwd)/data:/opt/jmusicbot-ml/data" jmusicbot-ml
docker logs -f jmusicbot-ml
```

`config.txt` und die übrigen Dateien liegen im Ordner `data`.

<a id="s7"></a>
## 7. Optionale Einstellungen

Alles steht in `config.txt`, und jede Einstellung ist in der Datei selbst erklärt. Die wichtigsten:

| Einstellung | Standard | Wirkung |
|---|---|---|
| `language` | `"en"` | Sprache des Bots: `en`, `tr`, `ja`, `de`, `es`, `ru`. |
| `spclient`, `spsecret` | leer | Spotify-API-Schlüssel. **Nicht nötig**; ohne sie wird die öffentliche Spotify-Seite genutzt. Mit ihnen ist die Zuordnung etwas genauer. |
| `youtubeoauth2` | `false` | Anmeldung bei YouTube mit einem Google-Konto (für altersbeschränkte Videos). Siehe unten. |
| `failnotice` | `"none"` | Wer benachrichtigt wird, wenn ein Titel gar nicht abspielbar ist: `"none"` (niemand, nur Konsole), `"dm"` (DM an die anfragende Person), `"channel"` (im Kanal). |
| `cachelimitmb` | `1024` | Maximale Größe (MB) des Ordners `cache` mit heruntergeladenen Titeln. Wird sie überschritten, werden die am längsten nicht gespielten Titel gelöscht. |
| `altprefix` | `"NONE"` | Ein zweites Präfix (z. B. `"?"`). |

### Spotify-Schlüssel (optional)

1. https://developer.spotify.com/dashboard → **Create app**.
2. Trage `https://example.com` als Redirect URI ein und wähle **Web API**.
3. Kopiere **Client ID** und **Client secret** von der Seite *Settings* nach `spclient` und `spsecret`.

Seit 2026 verlangt Spotify für Entwickler-Apps, dass der App-Besitzer **Premium** hat. Ohne Premium lässt du die Felder leer; der Bot funktioniert trotzdem. Playlists anderer Leute werden immer über die öffentliche Seite gelesen (höchstens 100 Titel pro Liste).

### YouTube-Konto (nur für altersbeschränkte Videos)

Normale Videos laufen ohne Konto. YouTubes **18+**-Videos öffnen sich nur mit einem angemeldeten Konto.

1. Setze `youtubeoauth2 = true` in `config.txt` und starte den Bot neu.
2. In der Konsole erscheint die Zeile `go to https://www.google.com/device and enter code XXX-XXX-XXX`.
3. Öffne die Seite, gib den Code ein und wähle das Konto. Der Bot erkennt die Anmeldung selbst und speichert den Schlüssel in `youtube-oauth.txt`; eine erneute Anmeldung ist nicht nötig.

> ⚠️ **Verwende nicht dein Hauptkonto.** Lege ein eigenes Google-Konto nur für den Bot an (Geburtsdatum 18+). Die YouTube-Bedingungen erlauben keinen automatisierten Zugriff; das Konto kann eingeschränkt werden. Gib `youtube-oauth.txt` niemals weiter. Du kannst die Verbindung jederzeit unter *Google-Konto → Sicherheit → Verbindungen mit Drittanbietern* entfernen.

Alternative: Lege eine `cookies.txt` im Netscape-Format in den Bot-Ordner, dann lädt die yt-dlp-Ausweichlösung mit diesem Konto herunter. Eine Cookie-Datei gibt Zugriff auf die gesamte Google-Sitzung des Kontos und ist daher riskanter.

<a id="s8"></a>
## 8. Befehle

Jeder Befehl funktioniert mit `/` oder mit dem Präfix. Mit `/help` erhältst du die vollständige Liste. Die Befehlsbeschreibungen erscheinen in deiner Discord-Sprache.

| Befehl | Beschreibung |
|---|---|
| `/play` | Spielt einen Titelnamen oder einen YouTube- / Spotify- / SoundCloud-Link |
| `/spotify` | Spielt einen Spotify-Link mit Infokarte |
| `/search` | Sucht auf YouTube und lässt dich auswählen |
| `/nowplaying` | „Läuft gerade“-Karte mit Tasten |
| `/queue` | Zeigt die Warteschlange |
| `/skip` | Stimmt fürs Überspringen |
| `/lyrics` | Songtext (`/lyrics Coldplay - Yellow` geht auch) |
| `/volume` | Lautstärke (0-150) |
| `/shuffle` | Mischt deine Titel |
| `/seek` | Spult vor / zurück |
| `/remove` | Entfernt einen Titel aus der Warteschlange |
| `/pause` | Pausiert (DJ) |
| `/stop` | Stoppt und leert die Warteschlange (DJ) |
| `/forceskip` | Überspringt ohne Abstimmung (DJ) |
| `/playnext` | Fügt am Anfang der Warteschlange ein (DJ) |
| `/repeat` | Wiederholungsmodus (DJ) |
| `/skipto` | Springt zu einem Titel in der Warteschlange (DJ) |
| `/movetrack` | Verschiebt einen Titel in der Warteschlange (DJ) |
| `/playlists` | Server-Playlists |
| `/mylist` | Persönliche Playlists |
| `/settings` | Zeigt die Servereinstellungen |
| `/setdj` | Legt die DJ-Rolle fest (Admin) |
| `/settc` | Beschränkt Befehle auf einen Kanal (Admin) |
| `/help` | Listet alle Befehle |

<a id="s9"></a>
## 9. Fehlerbehebung

| Problem | Lösung |
|---|---|
| Bot startet nicht, `UnsupportedClassVersionError` | Dein Java ist zu alt. `java -version` muss **25** anzeigen. |
| Konsole zeigt `Kein Token eingegeben` / `Invalid token` | Prüfe `token` in `config.txt` (in Anführungszeichen, ohne Leerzeichen). |
| Public Bot lässt sich im Developer Portal nicht ausschalten | Setze zuerst **Installation → Install Link = None**. |
| Slash-Befehle erscheinen nicht | Discord neu laden (Strg+R). Der Einladungslink muss `applications.commands` enthalten. |
| `!play` geht nicht, `/play` schon | Im Developer Portal muss **MESSAGE CONTENT INTENT** eingeschaltet sein. |
| Bot betritt den Kanal und verlässt ihn sofort | Prüfe die Konsolenzeilen `YouTube-Stream konnte nicht geöffnet werden` und `yt-dlp error:`. Steht dort `Sign in to confirm your age`, ist das Video altersbeschränkt ([Abschnitt 7](#s7)). |
| `yt-dlp exit code=255` | yt-dlp konnte nicht starten (Speicher evtl. voll). Der Bot räumt auf und versucht es einmal erneut; schaffe Speicherplatz. |
| Spotify-Link wird nicht abgespielt | Der Link muss öffentlich sein. Podcast-Links werden nicht unterstützt. |

Gib bei einer Fehlermeldung die passenden Konsolenzeilen und die Bot-Version an (das Banner `JMusicBot-ML vX.Y.Z` beim Start). **Gib niemals deinen Token oder deine `config.txt` weiter.**

<a id="s10"></a>
## 10. Aus dem Quellcode bauen

Benötigt JDK 25:

```bash
./mvnw -B -DskipTests package
# Ausgabe: target/JMusicBot-ML-All.jar
```

Unter Windows: `mvnw.cmd -B -DskipTests package`. Wenn ein Tag mit `v` am Anfang (z. B. `v1.0.1`) gepusht wird, baut GitHub Actions die jar-Datei und hängt sie automatisch an ein **Release** an.

<a id="s-translations"></a>
### Übersetzungen

Alle Texte liegen in [`src/main/resources/lang/`](src/main/resources/lang/): `en.json`, `tr.json`, `ja.json`, `de.json`, `es.json`, `ru.json` sowie die `config.txt`-Vorlagen in `lang/config/`. Für fehlende Schlüssel wird Englisch verwendet. Um eine Sprache hinzuzufügen, kopiere `en.json` und `config/en.conf`, übersetze die Werte und trage den Sprachcode in `LANGUAGES` in [`L.java`](src/main/java/dev/cosgy/jmusicbot/i18n/L.java) ein. Verbesserungen der Übersetzungen sind sehr willkommen.

Änderungsprotokoll: [CHANGELOG.md](CHANGELOG.md)

<a id="s-license"></a>
## Lizenz und Danksagung

JMusicBot-ML steht unter der [Apache License 2.0](LICENSE), derselben Lizenz wie die Projekte, auf denen es basiert.

### Ursprungsprojekte

| Projekt | Autor | Was es ist |
|---|---|---|
| [jagrosh/MusicBot](https://github.com/jagrosh/MusicBot) | John Grosh (jagrosh) | Der ursprüngliche JMusicBot |
| [Cosgy-Dev/JMusicBot-JP](https://github.com/Cosgy-Dev/JMusicBot-JP) | Cosgy Dev | Japanische Ausgabe (Version 0.11.0 ist die Basis von JMusicBot-ML) |

Die ursprünglichen Urheberrechtshinweise bleiben in den Quelldateien erhalten. Von JMusicBot-ML hinzugefügte Dateien und Änderungen sind mit „JMusicBot-ML contributors“ gekennzeichnet. Was geändert wurde, steht in [CHANGELOG.md](CHANGELOG.md) und zusammengefasst in [NOTICE](NOTICE).

### Bibliotheken und Dienste

[JDA](https://github.com/discord-jda/JDA) · [Lavaplayer](https://github.com/lavalink-devs/lavaplayer) · [youtube-source](https://github.com/lavalink-devs/youtube-source) · [yt-dlp](https://github.com/yt-dlp/yt-dlp) (wird zur Laufzeit heruntergeladen) · [JDAVE](https://github.com/MinnDevelopment/jdave) · [Logback](https://logback.qos.ch) · [Typesafe Config](https://github.com/lightbend/config) · [jsoup](https://jsoup.org) · [Unirest](https://github.com/Kong/unirest-java) · [JFiglet](https://github.com/lalyos/jfiglet) · [JLyrics](https://github.com/Cosgy-Dev/JLyrics) · Songtexte von [LRCLIB](https://lrclib.net). Jede wird unter ihrer eigenen Lizenz verwendet; siehe [NOTICE](NOTICE).

### Mit Claude entwickelt

JMusicBot-ML wurde mit Hilfe von **[Claude](https://claude.ai)** entwickelt, einem KI-Assistenten von Anthropic. Claude hat die meisten Änderungen dieser Ausgabe geschrieben — die Übersetzungen in sechs Sprachen, die neue Spotify-Unterstützung, die „Läuft gerade“-Karte, die yt-dlp-Ausweichlösung und die Fehlerbehebungen —, während der Projektinhaber jede Version auf einem echten Server getestet und entschieden hat, was gebaut wird. Mit Claude erstellte Commits sind mit `Co-Authored-By: Claude` gekennzeichnet.

Dieser Bot steht in keiner Verbindung zu YouTube, Spotify oder Discord und wird von ihnen nicht unterstützt. Betreibe ihn auf deinem eigenen Server und halte dich an die Bedingungen der jeweiligen Dienste.
