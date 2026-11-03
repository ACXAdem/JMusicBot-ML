# JMusicBot-ML — многоязычный JMusicBot

[English](README.md) · [Türkçe](README.tr.md) · [日本語](README.ja.md) · [Deutsch](README.de.md) · [Español](README.es.md) · **Русский**

**JMusicBot-ML** (*ML = Multi-Language, многоязычный*) — неофициальная многоязычная версия известного [JMusicBot](https://github.com/jagrosh/MusicBot): музыкальный бот для Discord, который вы запускаете на своём сервере. Основан на [JMusicBot-JP](https://github.com/Cosgy-Dev/JMusicBot-JP) 0.11.0 и доступен на **6 языках** (English, Türkçe, 日本語, Deutsch, Español, Русский).

Играет музыку с YouTube, YouTube Music, **Spotify**, SoundCloud и интернет-радио. Установка — один файл `.jar`.

> **Основан на:** [jagrosh/MusicBot](https://github.com/jagrosh/MusicBot) (JMusicBot, автор John Grosh) → [Cosgy-Dev/JMusicBot-JP](https://github.com/Cosgy-Dev/JMusicBot-JP) (японская версия, Cosgy Dev) → JMusicBot-ML.
> Вся заслуга за исходного бота принадлежит этим проектам. JMusicBot-ML — независимая изменённая версия, не связанная с ними и не одобренная ими. См. [Лицензия и благодарности](#s-license).

## Возможности

- **6 языков:** сообщения бота, комментарии в `config.txt` и записи в консоли на английском, турецком, японском, немецком, испанском или русском (`language` в config.txt). Описания слэш-команд каждый пользователь видит на **своём языке Discord**.
- **Spotify:** ссылки на треки, альбомы, плейлисты и исполнителей. Spotify не отдаёт звук, поэтому тот же трек автоматически находится на YouTube Music / YouTube и воспроизводится оттуда. **API-ключ не нужен.**
- **Карточка «Сейчас играет» с кнопками:** обложка, полоса прогресса и кнопки ⏮ ⏸ ⏭ ⏹ 🔁 🔉 🔊 🔀 📜 🎤.
- **Тексты песен** из [LRCLIB](https://lrclib.net) (ключ не нужен).
- **Ссылки на радио / миксы YouTube:** до 75 треков добавляются без подтверждения.
- **Запасной вариант через yt-dlp:** если YouTube не отдаёт трек, бот скачивает его с помощью [yt-dlp](https://github.com/yt-dlp/yt-dlp) и воспроизводит. yt-dlp скачивается при первом запуске и обновляется автоматически.
- **Не требует обслуживания:** кэш скачанных треков и файлы журналов ограничены по размеру, старые файлы удаляются сами.

---

## Содержание

1. [Требования](#s1)
2. [Создание бота в Discord](#s2)
3. [Скачивание бота](#s3)
4. [Первый запуск и config.txt](#s4)
5. [Приглашение бота на сервер](#s5)
6. [Запуск бота](#s6) — Windows · Linux · Pterodactyl · Docker
7. [Дополнительные настройки](#s7)
8. [Команды](#s8)
9. [Решение проблем](#s9)
10. [Сборка из исходников](#s10) · [Переводы](#s-translations)

---

<a id="s1"></a>
## 1. Требования

- **Java 25** (64-бит). Более старые версии Java не подойдут.
- Аккаунт Discord и право **Управлять сервером** на сервере, куда вы добавляете бота.
- ~150 МБ свободного места (jar + yt-dlp) и место для кэша (по умолчанию не более 1 ГБ).
- Достаточно 512 МБ оперативной памяти.

<a id="s2"></a>
## 2. Создание бота в Discord

1. Откройте https://discord.com/developers/applications → **New Application** → задайте имя.
2. В меню слева откройте **Installation** → установите *Install Link* в **None** и сохраните.
   (Без этого следующий шаг завершится ошибкой «Private application cannot have a default authorization link».)
3. В меню слева откройте **Bot**:
   - **Выключите** **Public Bot** (добавлять бота сможете только вы).
   - В разделе **Privileged Gateway Intents** **включите** **MESSAGE CONTENT INTENT** (нужно для команд с префиксом, например `!play`).
   - Нажмите **Reset Token** и скопируйте **токен**. Никому его не передавайте — владелец токена может управлять вашим ботом.
4. Узнайте свой **ID пользователя Discord**: Discord → Настройки → Расширенные → включите **Режим разработчика**, затем щёлкните правой кнопкой по своему имени → **Копировать ID пользователя**.

<a id="s3"></a>
## 3. Скачивание бота

Скачайте последний `JMusicBot-ML.jar` из раздела **Releases** справа на этой странице и положите его в **пустую папку**. Бот хранит в ней настройки, кэш и журналы.

<a id="s4"></a>
## 4. Первый запуск и config.txt

Запустите бота один раз (команда — в [разделе 6](#s6)). В папке появится файл `config.txt`. Откройте его в текстовом редакторе и заполните как минимум:

```
language = "ru"                // en, tr, ja, de, es или ru
token = "ТОКЕН_БОТА"           // токен из шага 2
owner = ID_ВЛАДЕЛЬЦА           // ваш ID пользователя Discord (число, без кавычек)
prefix = "!"                   // для команд с префиксом (например, !play)
```

Сохраните и перезапустите бота. Когда в консоли появятся `Finished Loading!` и `Зарегистрировано слэш-команд в Discord: …`, бот готов.

> При первом запуске файл создаётся на английском. Если указать `language = "ru"` и перезапустить бота, комментарии в `config.txt` тоже будут переписаны на русском. Ваши настройки сохранятся, а также будет создана резервная копия (`config.txt.vX.bak`). То же происходит автоматически, когда новая версия добавляет настройки.

<a id="s5"></a>
## 5. Приглашение бота на сервер

Замените `APPLICATION_ID` в ссылке ниже на **Application ID** вашего приложения (Developer Portal → *General Information*) и откройте её в браузере:

```
https://discord.com/oauth2/authorize?client_id=APPLICATION_ID&scope=bot%20applications.commands&permissions=281475047352384
```

Ссылка запрашивает права, нужные боту (отправка сообщений, встраивание ссылок, подключение и речь в голосовом канале, смена ника, статус голосового канала…).

<a id="s6"></a>
## 6. Запуск бота

Команда везде одинаковая:

```
java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
```

### Windows

Откройте командную строку (cmd) и установите Java 25:

```cmd
winget install EclipseAdoptium.Temurin.25.JRE
```

Затем перейдите в папку с jar-файлом и запустите бота:

```cmd
cd C:\Users\ИМЯ\Desktop\jmusicbot
java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
```

Эту строку можно сохранить в файл `start.bat` в той же папке и запускать двойным щелчком. Без `-Dnogui=true` бот открывает небольшое окно.

### Linux (Ubuntu / Debian)

```bash
sudo apt update
sudo apt install openjdk-25-jre-headless
mkdir ~/jmusicbot && cd ~/jmusicbot     # положите сюда JMusicBot-ML.jar
java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
```

Если в вашем репозитории пакетов нет Java 25, используйте [Adoptium Temurin 25](https://adoptium.net/).

Чтобы бот запускался автоматически при загрузке, создайте `/etc/systemd/system/jmusicbot.service` (замените `ПОЛЬЗОВАТЕЛЬ`):

```ini
[Unit]
Description=JMusicBot-ML music bot
After=network-online.target

[Service]
User=ПОЛЬЗОВАТЕЛЬ
WorkingDirectory=/home/ПОЛЬЗОВАТЕЛЬ/jmusicbot
ExecStart=/usr/bin/java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable --now jmusicbot
journalctl -u jmusicbot -f     # следить за консолью
```

### Pterodactyl

1. В панели откройте **Admin → Nests → Import Egg** и импортируйте [`pterodactyl/egg-jmusicbot-ml.json`](pterodactyl/egg-jmusicbot-ml.json) из этого репозитория.
2. Создайте новый сервер с этим egg (достаточно 512 МБ RAM и 2 ГБ диска). Docker-образ: `ghcr.io/pterodactyl/yolks:java_25`.
3. Загрузите `JMusicBot-ML.jar` через **File Manager** сервера.
4. Нажмите **Start**. При первом запуске создаётся `config.txt`. Заполните в File Manager `token`, `owner` и `language` и перезапустите.

Для обновления просто замените jar-файл и перезапустите.

### Docker

Положите `JMusicBot-ML.jar` рядом с `Dockerfile` из этого репозитория:

```bash
docker build -t jmusicbot-ml .
docker run -d --name jmusicbot-ml --restart unless-stopped -v "$(pwd)/data:/opt/jmusicbot-ml/data" jmusicbot-ml
docker logs -f jmusicbot-ml
```

`config.txt` и остальные файлы хранятся в папке `data`.

<a id="s7"></a>
## 7. Дополнительные настройки

Всё находится в `config.txt`, и каждая настройка описана прямо в файле. Самые важные:

| Настройка | По умолчанию | Что делает |
|---|---|---|
| `language` | `"en"` | Язык бота: `en`, `tr`, `ja`, `de`, `es`, `ru`. |
| `spclient`, `spsecret` | пусто | API-ключи Spotify. **Не обязательны**; без них используется публичная страница Spotify. С ними поиск соответствий чуть точнее. |
| `youtubeoauth2` | `false` | Вход в YouTube через аккаунт Google (для видео с возрастными ограничениями). См. ниже. |
| `failnotice` | `"none"` | Кого уведомлять, если трек не удаётся воспроизвести: `"none"` (никого, только консоль), `"dm"` (личное сообщение тому, кто заказал трек), `"channel"` (в канал). |
| `cachelimitmb` | `1024` | Максимальный размер (МБ) папки `cache` со скачанными треками. При превышении удаляются треки, которые дольше всего не воспроизводились. |
| `altprefix` | `"NONE"` | Второй префикс (например, `"?"`). |

### Ключи Spotify (необязательно)

1. https://developer.spotify.com/dashboard → **Create app**.
2. Укажите `https://example.com` как Redirect URI и выберите **Web API**.
3. Скопируйте **Client ID** и **Client secret** со страницы *Settings* в `spclient` и `spsecret`.

С 2026 года Spotify требует, чтобы у владельца приложения разработчика был **Premium**. Если Premium нет, оставьте поля пустыми — бот всё равно работает. Чужие плейлисты всегда читаются с публичной страницы (не более 100 треков на список).

### Аккаунт YouTube (только для видео с возрастными ограничениями)

Обычные видео играют без аккаунта. Видео YouTube с отметкой **18+** открываются только при входе в аккаунт.

1. Установите `youtubeoauth2 = true` в `config.txt` и перезапустите бота.
2. В консоли появится строка `go to https://www.google.com/device and enter code XXX-XXX-XXX`.
3. Откройте эту страницу, введите код и выберите аккаунт. Бот сам обнаружит вход и сохранит ключ в `youtube-oauth.txt`; повторный вход не понадобится.

> ⚠️ **Не используйте основной аккаунт.** Создайте отдельный аккаунт Google только для бота (дата рождения — 18+). Условия YouTube не разрешают автоматизированный доступ, аккаунт могут ограничить. Никому не передавайте `youtube-oauth.txt`. Подключение можно удалить в любой момент: *Аккаунт Google → Безопасность → Сторонние подключения*.

Альтернатива: положите `cookies.txt` в формате Netscape в папку бота, и запасной вариант через yt-dlp будет скачивать с этого аккаунта. Файл cookies даёт доступ ко всему сеансу Google этого аккаунта, поэтому это рискованнее.

<a id="s8"></a>
## 8. Команды

Все команды работают через `/` или с префиксом. Введите `/help`, чтобы увидеть полный список. Описания команд показываются на языке вашего Discord.

| Команда | Описание |
|---|---|
| `/play` | Воспроизводит название трека или ссылку YouTube / Spotify / SoundCloud |
| `/spotify` | Воспроизводит ссылку Spotify с информационной карточкой |
| `/search` | Ищет на YouTube и даёт выбрать |
| `/nowplaying` | Карточка «Сейчас играет» с кнопками |
| `/queue` | Показывает очередь |
| `/skip` | Голосование за пропуск |
| `/lyrics` | Текст песни (`/lyrics Coldplay - Yellow` тоже работает) |
| `/volume` | Громкость (0-150) |
| `/shuffle` | Перемешивает ваши треки |
| `/seek` | Перемотка вперёд / назад |
| `/remove` | Удаляет трек из очереди |
| `/pause` | Пауза (DJ) |
| `/stop` | Останавливает и очищает очередь (DJ) |
| `/forceskip` | Пропуск без голосования (DJ) |
| `/playnext` | Добавляет в начало очереди (DJ) |
| `/repeat` | Режим повтора (DJ) |
| `/skipto` | Переход к треку в очереди (DJ) |
| `/movetrack` | Перемещает трек в очереди (DJ) |
| `/playlists` | Плейлисты сервера |
| `/mylist` | Личные плейлисты |
| `/settings` | Показывает настройки сервера |
| `/setdj` | Задаёт роль DJ (Администратор) |
| `/settc` | Ограничивает команды одним каналом (Администратор) |
| `/help` | Список всех команд |

<a id="s9"></a>
## 9. Решение проблем

| Проблема | Решение |
|---|---|
| Бот не запускается, `UnsupportedClassVersionError` | Слишком старая Java. `java -version` должна показывать **25**. |
| В консоли `Токен не введён!` / `Invalid token` | Проверьте `token` в `config.txt` (в кавычках, без пробелов). |
| В Developer Portal не выключается Public Bot | Сначала установите **Installation → Install Link = None**. |
| Слэш-команды не появляются | Перезапустите Discord (Ctrl+R). В ссылке приглашения должно быть `applications.commands`. |
| `!play` не работает, а `/play` работает | В Developer Portal должен быть включён **MESSAGE CONTENT INTENT**. |
| Бот заходит в канал и сразу выходит | Найдите в консоли строки `Не удалось открыть поток YouTube` и `yt-dlp error:`. Если там `Sign in to confirm your age`, у видео возрастное ограничение ([раздел 7](#s7)). |
| `yt-dlp exit code=255` | yt-dlp не смог запуститься (возможно, диск заполнен). Бот очищает временные файлы и пробует ещё раз; освободите место на диске. |
| Ссылка Spotify не воспроизводится | Ссылка должна быть публичной. Ссылки на подкасты не поддерживаются. |

Сообщая об ошибке, приложите нужные строки из консоли и версию бота (баннер `JMusicBot-ML vX.Y.Z` при запуске). **Никогда не публикуйте свой токен и `config.txt`.**

<a id="s10"></a>
## 10. Сборка из исходников

Нужен JDK 25:

```bash
./mvnw -B -DskipTests package
# результат: target/JMusicBot-ML-All.jar
```

В Windows: `mvnw.cmd -B -DskipTests package`. Когда в репозиторий отправляется тег, начинающийся с `v` (например, `v1.0.1`), GitHub Actions собирает jar и автоматически прикрепляет его к **Release**.

<a id="s-translations"></a>
### Переводы

Все тексты лежат в [`src/main/resources/lang/`](src/main/resources/lang/): `en.json`, `tr.json`, `ja.json`, `de.json`, `es.json`, `ru.json`, а шаблоны `config.txt` — в `lang/config/`. Для отсутствующих ключей используется английский. Чтобы добавить язык, скопируйте `en.json` и `config/en.conf`, переведите значения и добавьте код языка в `LANGUAGES` в [`L.java`](src/main/java/dev/cosgy/jmusicbot/i18n/L.java). Исправления переводов очень приветствуются.

Список изменений: [CHANGELOG.md](CHANGELOG.md)

<a id="s-license"></a>
## Лицензия и благодарности

JMusicBot-ML распространяется по [Apache License 2.0](LICENSE) — той же лицензии, что и проекты, на которых он основан.

### Исходные проекты

| Проект | Автор | Что это |
|---|---|---|
| [jagrosh/MusicBot](https://github.com/jagrosh/MusicBot) | John Grosh (jagrosh) | Оригинальный JMusicBot |
| [Cosgy-Dev/JMusicBot-JP](https://github.com/Cosgy-Dev/JMusicBot-JP) | Cosgy Dev | Японская версия (версия 0.11.0 — основа JMusicBot-ML) |

Оригинальные уведомления об авторских правах сохранены в исходных файлах. Файлы и изменения, добавленные JMusicBot-ML, помечены «JMusicBot-ML contributors». Что именно изменено, описано в [CHANGELOG.md](CHANGELOG.md) и кратко — в [NOTICE](NOTICE).

### Библиотеки и сервисы

[JDA](https://github.com/discord-jda/JDA) · [Lavaplayer](https://github.com/lavalink-devs/lavaplayer) · [youtube-source](https://github.com/lavalink-devs/youtube-source) · [yt-dlp](https://github.com/yt-dlp/yt-dlp) (скачивается при запуске) · [JDAVE](https://github.com/MinnDevelopment/jdave) · [Logback](https://logback.qos.ch) · [Typesafe Config](https://github.com/lightbend/config) · [jsoup](https://jsoup.org) · [Unirest](https://github.com/Kong/unirest-java) · [JFiglet](https://github.com/lalyos/jfiglet) · [JLyrics](https://github.com/Cosgy-Dev/JLyrics) · тексты песен от [LRCLIB](https://lrclib.net). Каждая используется на условиях своей лицензии; см. [NOTICE](NOTICE).

### Создано с помощью Claude

JMusicBot-ML разработан с помощью **[Claude](https://claude.ai)** — ИИ-ассистента компании Anthropic. Claude написал большую часть изменений этой версии — переводы на шесть языков, новую поддержку Spotify, карточку «Сейчас играет», запасной вариант через yt-dlp и исправления ошибок, — а владелец проекта проверял каждую версию на реальном сервере и решал, что делать. Коммиты, сделанные с Claude, помечены `Co-Authored-By: Claude`.

Этот бот не связан с YouTube, Spotify или Discord и не одобрен ими. Запускайте его на своём сервере и соблюдайте условия используемых сервисов.
