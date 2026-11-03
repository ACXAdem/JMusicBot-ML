# JMusicBot-ML — JMusicBot multilingüe

[English](README.md) · [Türkçe](README.tr.md) · [日本語](README.ja.md) · [Deutsch](README.de.md) · **Español** · [Русский](README.ru.md)

**JMusicBot-ML** (*ML = Multi-Language, multilingüe*) es una edición no oficial y multilingüe del conocido [JMusicBot](https://github.com/jagrosh/MusicBot): un bot de música para Discord que alojas tú mismo, basado en [JMusicBot-JP](https://github.com/Cosgy-Dev/JMusicBot-JP) 0.11.0 y disponible en **6 idiomas** (English, Türkçe, 日本語, Deutsch, Español, Русский).

Reproduce música de YouTube, YouTube Music, **Spotify**, SoundCloud y radios por internet. La instalación es un único archivo `.jar`.

> **Basado en:** [jagrosh/MusicBot](https://github.com/jagrosh/MusicBot) (JMusicBot, de John Grosh) → [Cosgy-Dev/JMusicBot-JP](https://github.com/Cosgy-Dev/JMusicBot-JP) (edición japonesa, de Cosgy Dev) → JMusicBot-ML.
> Todo el mérito del bot original es de esos proyectos. JMusicBot-ML es una versión modificada e independiente, sin relación con ellos ni respaldada por ellos. Consulta [Licencia y créditos](#s-license).

## Características

- **6 idiomas:** mensajes del bot, comentarios de `config.txt` y registros de consola en inglés, turco, japonés, alemán, español o ruso (`language` en config.txt). Cada usuario ve las descripciones de los comandos de barra en **su propio idioma de Discord**.
- **Spotify:** enlaces de canciones, álbumes, listas y artistas. Spotify no ofrece audio, así que la misma canción se busca automáticamente en YouTube Music / YouTube y se reproduce desde allí. **No hace falta clave de API.**
- **Tarjeta «Reproduciendo ahora» con botones:** portada, barra de progreso y botones ⏮ ⏸ ⏭ ⏹ 🔁 🔉 🔊 🔀 📜 🎤.
- **Letras** de [LRCLIB](https://lrclib.net) (sin clave).
- **Enlaces de radio / mix de YouTube:** se añaden hasta 75 canciones sin preguntar.
- **Alternativa con yt-dlp:** si YouTube rechaza una canción, el bot la descarga con [yt-dlp](https://github.com/yt-dlp/yt-dlp) y la reproduce. yt-dlp se descarga en el primer arranque y se mantiene actualizado.
- **Sin mantenimiento:** la caché de canciones descargadas y los archivos de registro tienen un límite de tamaño; los antiguos se borran solos.

---

## Contenido

1. [Requisitos](#s1)
2. [Crear el bot en Discord](#s2)
3. [Descargar el bot](#s3)
4. [Primer arranque y config.txt](#s4)
5. [Invitar el bot a tu servidor](#s5)
6. [Ejecutar el bot](#s6) — Windows · Linux · Pterodactyl · Docker
7. [Ajustes opcionales](#s7)
8. [Comandos](#s8)
9. [Solución de problemas](#s9)
10. [Compilar desde el código fuente](#s10) · [Traducciones](#s-translations)

---

<a id="s1"></a>
## 1. Requisitos

- **Java 25** (64 bits). Las versiones anteriores de Java no funcionan.
- Una cuenta de Discord y el permiso **Gestionar servidor** en el servidor donde quieras añadir el bot.
- ~150 MB libres en disco (jar + yt-dlp) más espacio para la caché (1 GB como máximo por defecto).
- Con 512 MB de RAM es suficiente.

<a id="s2"></a>
## 2. Crear el bot en Discord

1. Ve a https://discord.com/developers/applications → **New Application** → ponle un nombre.
2. En el menú de la izquierda abre **Installation** → pon *Install Link* en **None** y guarda.
   (Sin esto, el siguiente paso falla con «Private application cannot have a default authorization link».)
3. En el menú de la izquierda abre **Bot**:
   - **Desactiva** **Public Bot** (solo tú podrás añadir el bot).
   - En **Privileged Gateway Intents**, **activa** **MESSAGE CONTENT INTENT** (necesario para comandos con prefijo como `!play`).
   - Pulsa **Reset Token** y copia el **token**. No lo compartas nunca: quien tenga el token puede controlar tu bot.
4. Obtén tu **ID de usuario de Discord**: Discord → Ajustes → Avanzado → activa el **Modo desarrollador**, luego clic derecho en tu nombre → **Copiar ID de usuario**.

<a id="s3"></a>
## 3. Descargar el bot

Descarga el último `JMusicBot-ML.jar` de la sección **Releases** a la derecha de esta página y colócalo en una **carpeta vacía**. El bot guarda en esa carpeta sus ajustes, la caché y los registros.

<a id="s4"></a>
## 4. Primer arranque y config.txt

Inicia el bot una vez (el comando está en la [sección 6](#s6)). Se crea un archivo `config.txt` en la carpeta. Ábrelo con un editor de texto y rellena al menos:

```
language = "es"                // en, tr, ja, de, es o ru
token = "TOKEN_DEL_BOT"        // el token del paso 2
owner = ID_DEL_PROPIETARIO     // tu ID de usuario de Discord (un número, sin comillas)
prefix = "!"                   // para comandos con prefijo (p. ej. !play)
```

Guarda y reinicia el bot. Cuando la consola muestre `Finished Loading!` y `Se registraron … comandos de barra en Discord`, el bot está listo.

> En el primer arranque el archivo se crea en inglés. Si pones `language = "es"` y reinicias, los comentarios de `config.txt` también se reescriben en español. Tus ajustes se conservan y se crea una copia de seguridad (`config.txt.vX.bak`). Lo mismo ocurre automáticamente cuando una versión nueva añade ajustes.

<a id="s5"></a>
## 5. Invitar el bot a tu servidor

Sustituye `APPLICATION_ID` en el siguiente enlace por el **Application ID** de tu aplicación (Developer Portal → *General Information*) y ábrelo en el navegador:

```
https://discord.com/oauth2/authorize?client_id=APPLICATION_ID&scope=bot%20applications.commands&permissions=281475047352384
```

El enlace pide los permisos que necesita el bot (enviar mensajes, insertar enlaces, conectarse y hablar en voz, cambiar el apodo, poner el estado del canal de voz…).

<a id="s6"></a>
## 6. Ejecutar el bot

El comando es el mismo en todas partes:

```
java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
```

### Windows

Abre el Símbolo del sistema (cmd) e instala Java 25:

```cmd
winget install EclipseAdoptium.Temurin.25.JRE
```

Luego ve a la carpeta del jar e inicia el bot:

```cmd
cd C:\Users\TUNOMBRE\Desktop\jmusicbot
java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
```

Puedes poner esa línea en un archivo `start.bat` en la misma carpeta y abrirlo con doble clic. Sin `-Dnogui=true` el bot abre una pequeña ventana.

### Linux (Ubuntu / Debian)

```bash
sudo apt update
sudo apt install openjdk-25-jre-headless
mkdir ~/jmusicbot && cd ~/jmusicbot     # coloca aquí JMusicBot-ML.jar
java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
```

Si tu repositorio de paquetes no tiene Java 25, usa [Adoptium Temurin 25](https://adoptium.net/).

Para que el bot arranque solo al encender el equipo, crea `/etc/systemd/system/jmusicbot.service` (sustituye `TUUSUARIO`):

```ini
[Unit]
Description=JMusicBot-ML music bot
After=network-online.target

[Service]
User=TUUSUARIO
WorkingDirectory=/home/TUUSUARIO/jmusicbot
ExecStart=/usr/bin/java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable --now jmusicbot
journalctl -u jmusicbot -f     # seguir la consola
```

### Pterodactyl

1. En el panel ve a **Admin → Nests → Import Egg** e importa [`pterodactyl/egg-jmusicbot-ml.json`](pterodactyl/egg-jmusicbot-ml.json) de este repositorio.
2. Crea un servidor nuevo con este egg (bastan 512 MB de RAM y 2 GB de disco). Imagen de Docker: `ghcr.io/pterodactyl/yolks:java_25`.
3. Sube `JMusicBot-ML.jar` con el **File Manager** del servidor.
4. Pulsa **Start**. En el primer arranque se crea `config.txt`. Rellena `token`, `owner` y `language` desde el File Manager y reinicia.

Para actualizar, solo tienes que sustituir el archivo jar y reiniciar.

### Docker

Coloca `JMusicBot-ML.jar` junto al `Dockerfile` de este repositorio:

```bash
docker build -t jmusicbot-ml .
docker run -d --name jmusicbot-ml --restart unless-stopped -v "$(pwd)/data:/opt/jmusicbot-ml/data" jmusicbot-ml
docker logs -f jmusicbot-ml
```

`config.txt` y los demás archivos se guardan en la carpeta `data`.

<a id="s7"></a>
## 7. Ajustes opcionales

Todo está en `config.txt`, y cada ajuste se explica en el propio archivo. Los más importantes:

| Ajuste | Predeterminado | Para qué sirve |
|---|---|---|
| `language` | `"en"` | Idioma del bot: `en`, `tr`, `ja`, `de`, `es`, `ru`. |
| `spclient`, `spsecret` | vacío | Claves de la API de Spotify. **No son necesarias**; sin ellas se usa la página pública de Spotify. Con ellas la búsqueda es algo más precisa. |
| `youtubeoauth2` | `false` | Inicia sesión en YouTube con una cuenta de Google (para vídeos con restricción de edad). Ver abajo. |
| `failnotice` | `"none"` | A quién se avisa cuando una canción no se puede reproducir: `"none"` (a nadie, solo consola), `"dm"` (MD a quien la pidió), `"channel"` (en el canal). |
| `cachelimitmb` | `1024` | Tamaño máximo (MB) de la carpeta `cache` con las canciones descargadas. Si se supera, se borran las que llevan más tiempo sin reproducirse. |
| `altprefix` | `"NONE"` | Un segundo prefijo (p. ej. `"?"`). |

### Claves de Spotify (opcional)

1. https://developer.spotify.com/dashboard → **Create app**.
2. Escribe `https://example.com` como Redirect URI y elige **Web API**.
3. Copia **Client ID** y **Client secret** de la página *Settings* en `spclient` y `spsecret`.

Desde 2026, Spotify exige que el propietario de una app de desarrollador tenga **Premium**. Si no lo tienes, deja estos campos vacíos; el bot sigue funcionando. Las listas de otras personas siempre se leen desde la página pública (100 canciones como máximo por lista).

### Cuenta de YouTube (solo para vídeos con restricción de edad)

Los vídeos normales se reproducen sin cuenta. Los vídeos **+18** de YouTube solo se abren con una cuenta con sesión iniciada.

1. Pon `youtubeoauth2 = true` en `config.txt` y reinicia el bot.
2. En la consola aparece la línea `go to https://www.google.com/device and enter code XXX-XXX-XXX`.
3. Abre esa página, escribe el código y elige la cuenta. El bot detecta el inicio de sesión por sí mismo y guarda la clave en `youtube-oauth.txt`; no hace falta volver a iniciar sesión.

> ⚠️ **No uses tu cuenta principal.** Crea una cuenta de Google aparte solo para el bot (con fecha de nacimiento de mayor de edad). Las condiciones de YouTube no permiten el acceso automatizado; la cuenta podría quedar restringida. No compartas nunca `youtube-oauth.txt`. Puedes quitar la conexión cuando quieras en *Cuenta de Google → Seguridad → Conexiones con terceros*.

Alternativa: coloca un `cookies.txt` en formato Netscape en la carpeta del bot y la alternativa con yt-dlp descargará con esa cuenta. Un archivo de cookies da acceso a toda la sesión de Google de la cuenta, así que es más arriesgado.

<a id="s8"></a>
## 8. Comandos

Todos los comandos funcionan con `/` o con el prefijo. Escribe `/help` para ver la lista completa. Las descripciones de los comandos aparecen en tu idioma de Discord.

| Comando | Descripción |
|---|---|
| `/play` | Reproduce un nombre de canción o un enlace de YouTube / Spotify / SoundCloud |
| `/spotify` | Reproduce un enlace de Spotify con una tarjeta de información |
| `/search` | Busca en YouTube y te deja elegir |
| `/nowplaying` | Tarjeta «Reproduciendo ahora» con botones |
| `/queue` | Muestra la cola |
| `/skip` | Vota para saltar |
| `/lyrics` | Letra (`/lyrics Coldplay - Yellow` también funciona) |
| `/volume` | Volumen (0-150) |
| `/shuffle` | Mezcla tus canciones |
| `/seek` | Avanza / retrocede |
| `/remove` | Quita una canción de la cola |
| `/pause` | Pausa (DJ) |
| `/stop` | Detiene y vacía la cola (DJ) |
| `/forceskip` | Salta sin votación (DJ) |
| `/playnext` | Añade al principio de la cola (DJ) |
| `/repeat` | Modo de repetición (DJ) |
| `/skipto` | Salta a una canción de la cola (DJ) |
| `/movetrack` | Mueve una canción en la cola (DJ) |
| `/playlists` | Listas del servidor |
| `/mylist` | Listas personales |
| `/settings` | Muestra los ajustes del servidor |
| `/setdj` | Define el rol de DJ (Admin) |
| `/settc` | Limita los comandos a un canal (Admin) |
| `/help` | Muestra todos los comandos |

<a id="s9"></a>
## 9. Solución de problemas

| Síntoma | Solución |
|---|---|
| El bot no arranca, `UnsupportedClassVersionError` | Tu Java es demasiado antiguo. `java -version` debe mostrar **25**. |
| La consola muestra `¡No se introdujo ningún token!` / `Invalid token` | Revisa `token` en `config.txt` (entre comillas, sin espacios). |
| No se puede desactivar Public Bot en el Developer Portal | Primero pon **Installation → Install Link = None**. |
| No aparecen los comandos de barra | Reinicia Discord (Ctrl+R). El enlace de invitación debe incluir `applications.commands`. |
| `!play` no funciona pero `/play` sí | **MESSAGE CONTENT INTENT** debe estar activado en el Developer Portal. |
| El bot entra en el canal y sale enseguida | Mira en la consola las líneas `No se pudo abrir la transmisión de YouTube` y `yt-dlp error:`. Si dice `Sign in to confirm your age`, el vídeo tiene restricción de edad ([sección 7](#s7)). |
| `yt-dlp exit code=255` | yt-dlp no pudo arrancar (puede que el disco esté lleno). El bot limpia y lo intenta una vez más; libera espacio. |
| Un enlace de Spotify no se reproduce | El enlace debe ser público. Los enlaces de pódcast no son compatibles. |

Al informar de un error, incluye las líneas de consola correspondientes y la versión del bot (el banner `JMusicBot-ML vX.Y.Z` al arrancar). **No compartas nunca tu token ni tu `config.txt`.**

<a id="s10"></a>
## 10. Compilar desde el código fuente

Requiere JDK 25:

```bash
./mvnw -B -DskipTests package
# salida: target/JMusicBot-ML-All.jar
```

En Windows usa `mvnw.cmd -B -DskipTests package`. Cuando se sube una etiqueta que empieza por `v` (p. ej. `v1.0.1`), GitHub Actions compila el jar y lo adjunta automáticamente a una **Release**.

<a id="s-translations"></a>
### Traducciones

Todos los textos están en [`src/main/resources/lang/`](src/main/resources/lang/): `en.json`, `tr.json`, `ja.json`, `de.json`, `es.json`, `ru.json`, y las plantillas de `config.txt` en `lang/config/`. El inglés se usa para las claves que falten. Para añadir un idioma, copia `en.json` y `config/en.conf`, traduce los valores y añade el código del idioma a `LANGUAGES` en [`L.java`](src/main/java/dev/cosgy/jmusicbot/i18n/L.java). Las mejoras de las traducciones son muy bienvenidas.

Registro de cambios: [CHANGELOG.md](CHANGELOG.md)

<a id="s-license"></a>
## Licencia y créditos

JMusicBot-ML se publica bajo la [Apache License 2.0](LICENSE), la misma licencia que los proyectos en los que se basa.

### Proyectos de origen

| Proyecto | Autor | Qué es |
|---|---|---|
| [jagrosh/MusicBot](https://github.com/jagrosh/MusicBot) | John Grosh (jagrosh) | El JMusicBot original |
| [Cosgy-Dev/JMusicBot-JP](https://github.com/Cosgy-Dev/JMusicBot-JP) | Cosgy Dev | Edición japonesa (la versión 0.11.0 es la base de JMusicBot-ML) |

Los avisos de copyright originales se conservan en los archivos fuente. Los archivos y cambios añadidos por JMusicBot-ML están marcados como «JMusicBot-ML contributors». Lo que se cambió figura en [CHANGELOG.md](CHANGELOG.md) y resumido en [NOTICE](NOTICE).

### Bibliotecas y servicios

[JDA](https://github.com/discord-jda/JDA) · [Lavaplayer](https://github.com/lavalink-devs/lavaplayer) · [youtube-source](https://github.com/lavalink-devs/youtube-source) · [yt-dlp](https://github.com/yt-dlp/yt-dlp) (se descarga al ejecutar) · [JDAVE](https://github.com/MinnDevelopment/jdave) · [Logback](https://logback.qos.ch) · [Typesafe Config](https://github.com/lightbend/config) · [jsoup](https://jsoup.org) · [Unirest](https://github.com/Kong/unirest-java) · [JFiglet](https://github.com/lalyos/jfiglet) · [JLyrics](https://github.com/Cosgy-Dev/JLyrics) · letras de [LRCLIB](https://lrclib.net). Cada una se usa bajo su propia licencia; consulta [NOTICE](NOTICE).

### Hecho con Claude

JMusicBot-ML se desarrolló con la ayuda de **[Claude](https://claude.ai)**, un asistente de IA creado por Anthropic. Claude escribió la mayoría de los cambios de esta edición —las traducciones a seis idiomas, el nuevo soporte de Spotify, la tarjeta «Reproduciendo ahora», la alternativa con yt-dlp y las correcciones de errores—, mientras que el propietario del proyecto probó cada versión en un servidor real y decidió qué construir. Los commits hechos con Claude están marcados con `Co-Authored-By: Claude`.

Este bot no está afiliado a YouTube, Spotify ni Discord, ni cuenta con su respaldo. Úsalo en tu propio servidor y respeta las condiciones de los servicios implicados.
