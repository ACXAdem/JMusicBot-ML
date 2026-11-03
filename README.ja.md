# JMusicBot-ML — 多言語版 JMusicBot

[English](README.md) · [Türkçe](README.tr.md) · **日本語** · [Deutsch](README.de.md) · [Español](README.es.md) · [Русский](README.ru.md)

**JMusicBot-ML**（*ML = Multi-Language、多言語*）は、よく知られた [JMusicBot](https://github.com/jagrosh/MusicBot) の非公式・多言語版です。[JMusicBot-JP](https://github.com/Cosgy-Dev/JMusicBot-JP) 0.11.0 をベースにした、自分のサーバーで動かす Discord 音楽ボットで、**6 言語**（English, Türkçe, 日本語, Deutsch, Español, Русский）に対応しています。

YouTube、YouTube Music、**Spotify**、SoundCloud、インターネットラジオの音楽を再生できます。インストールは `.jar` ファイル 1 つだけです。

> **ベースにしたプロジェクト:** [jagrosh/MusicBot](https://github.com/jagrosh/MusicBot)（JMusicBot、John Grosh 氏）→ [Cosgy-Dev/JMusicBot-JP](https://github.com/Cosgy-Dev/JMusicBot-JP)（日本語版、Cosgy Dev）→ JMusicBot-ML
> 元のボットの功績はすべてこれらのプロジェクトにあります。JMusicBot-ML は独立した改変版であり、これらのプロジェクトとは関係がなく、承認も受けていません。詳しくは[ライセンスと謝辞](#s-license)をご覧ください。

## 機能

- **6 言語:** ボットのメッセージ、`config.txt` のコメント、コンソールのログを英語・トルコ語・日本語・ドイツ語・スペイン語・ロシア語で表示（config.txt の `language`）。スラッシュコマンドの説明は、各ユーザーの **Discord の言語** で表示されます。
- **Spotify:** 曲・アルバム・プレイリスト・アーティストのリンクに対応。Spotify は音声を提供しないため、同じ曲を YouTube Music / YouTube で自動的に探して再生します。**API キーは不要です。**
- **ボタン付きの「再生中」カード:** カバー画像、進行バー、⏮ ⏸ ⏭ ⏹ 🔁 🔉 🔊 🔀 📜 🎤 ボタン。
- **歌詞:** [LRCLIB](https://lrclib.net) から取得（キー不要）。
- **YouTube のラジオ / ミックスのリンク:** 確認なしで最大 75 曲を追加します。
- **yt-dlp によるフォールバック:** YouTube が曲を提供しない場合、[yt-dlp](https://github.com/yt-dlp/yt-dlp) でダウンロードして再生します。yt-dlp は初回起動時に自動でダウンロードされ、最新の状態に保たれます。
- **メンテナンス不要:** ダウンロードした曲のキャッシュとログファイルにはサイズの上限があり、古いものは自動で削除されます。

---

## 目次

1. [必要なもの](#s1)
2. [Discord でボットを作成する](#s2)
3. [ボットをダウンロードする](#s3)
4. [初回起動と config.txt](#s4)
5. [ボットをサーバーに招待する](#s5)
6. [ボットを起動する](#s6) — Windows · Linux · Pterodactyl · Docker
7. [任意の設定](#s7)
8. [コマンド](#s8)
9. [トラブルシューティング](#s9)
10. [ソースからビルドする](#s10) · [翻訳](#s-translations)

---

<a id="s1"></a>
## 1. 必要なもの

- **Java 25**（64 ビット）。古いバージョンの Java では動きません。
- Discord アカウントと、ボットを追加するサーバーでの **サーバー管理** 権限。
- 約 150 MB の空き容量（jar + yt-dlp）と、キャッシュ用の容量（初期設定では最大 1 GB）。
- メモリは 512 MB あれば十分です。

<a id="s2"></a>
## 2. Discord でボットを作成する

1. https://discord.com/developers/applications を開き → **New Application** → 名前を付けます。
2. 左のメニューで **Installation** を開き → *Install Link* を **None** にして保存します。
   （これをしないと、次の手順で「Private application cannot have a default authorization link」というエラーになります。）
3. 左のメニューで **Bot** を開き:
   - **Public Bot** を **オフ** にします（ボットを追加できるのはあなただけになります）。
   - **Privileged Gateway Intents** の **MESSAGE CONTENT INTENT** を **オン** にします（`!play` のようなプレフィックスコマンドに必要）。
   - **Reset Token** を押して **トークン** をコピーします。トークンは絶対に他人に教えないでください。トークンを知っている人は誰でもボットを操作できます。
4. 自分の **Discord ユーザー ID** を確認します: Discord → ユーザー設定 → 詳細設定 → **開発者モード** をオンにし、自分の名前を右クリック → **ユーザー ID をコピー**。

<a id="s3"></a>
## 3. ボットをダウンロードする

このページ右側の **Releases** から最新の `JMusicBot-ML.jar` をダウンロードし、**空のフォルダー** に置きます。ボットは設定・キャッシュ・ログをこのフォルダーに書き込みます。

<a id="s4"></a>
## 4. 初回起動と config.txt

ボットを一度起動します（コマンドは[6 章](#s6)を参照）。フォルダーに `config.txt` が作成されます。テキストエディターで開き、少なくとも次の項目を設定してください:

```
language = "ja"                // en, tr, ja, de, es, ru のいずれか
token = "ボットのトークン"        // 手順 2 で取得したトークン
owner = オーナーのID             // あなたの Discord ユーザー ID（数字、引用符なし）
prefix = "!"                   // プレフィックスコマンド用（例: !play）
```

保存してボットを再起動します。コンソールに `Finished Loading!` と `… 個のスラッシュコマンドをDiscordに登録しました` が表示されれば準備完了です。

> 初回起動時、ファイルは英語で作成されます。`language = "ja"` にして再起動すると、`config.txt` のコメントも日本語で書き直されます。設定はそのまま保持され、バックアップ（`config.txt.vX.bak`）が作成されます。新しいバージョンで設定が追加されたときも、同じことが自動で行われます。

<a id="s5"></a>
## 5. ボットをサーバーに招待する

下のリンクの `APPLICATION_ID` を、あなたのアプリの **Application ID**（Developer Portal → *General Information*）に置き換えて、ブラウザーで開きます:

```
https://discord.com/oauth2/authorize?client_id=APPLICATION_ID&scope=bot%20applications.commands&permissions=281475047352384
```

このリンクは、ボットに必要な権限（メッセージ送信、埋め込みリンク、ボイスチャンネルへの接続と発言、ニックネームの変更、ボイスチャンネルのステータス設定など）を要求します。

<a id="s6"></a>
## 6. ボットを起動する

コマンドはどの環境でも同じです:

```
java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
```

### Windows

コマンドプロンプト（cmd）を開き、Java 25 をインストールします:

```cmd
winget install EclipseAdoptium.Temurin.25.JRE
```

jar ファイルのあるフォルダーに移動し、ボットを起動します:

```cmd
cd C:\Users\ユーザー名\Desktop\jmusicbot
java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
```

この行を同じフォルダーの `start.bat` に書いておけば、ダブルクリックで起動できます。`-Dnogui=true` を外すと小さなウィンドウが開きます。

### Linux（Ubuntu / Debian）

```bash
sudo apt update
sudo apt install openjdk-25-jre-headless
mkdir ~/jmusicbot && cd ~/jmusicbot     # ここに JMusicBot-ML.jar を置く
java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
```

パッケージリポジトリに Java 25 がない場合は、[Adoptium Temurin 25](https://adoptium.net/) を使ってください。

起動時に自動でボットを立ち上げるには、`/etc/systemd/system/jmusicbot.service` を作成します（`ユーザー名` を置き換えてください）:

```ini
[Unit]
Description=JMusicBot-ML music bot
After=network-online.target

[Service]
User=ユーザー名
WorkingDirectory=/home/ユーザー名/jmusicbot
ExecStart=/usr/bin/java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable --now jmusicbot
journalctl -u jmusicbot -f     # コンソールを表示
```

### Pterodactyl

1. パネルで **Admin → Nests → Import Egg** を開き、このリポジトリの [`pterodactyl/egg-jmusicbot-ml.json`](pterodactyl/egg-jmusicbot-ml.json) をインポートします。
2. この egg で新しいサーバーを作成します（RAM 512 MB、ディスク 2 GB で十分）。Docker イメージ: `ghcr.io/pterodactyl/yolks:java_25`。
3. サーバーの **File Manager** で `JMusicBot-ML.jar` をアップロードします。
4. **Start** を押します。初回起動時に `config.txt` が作成されます。File Manager で `token`、`owner`、`language` を設定して再起動します。

更新するときは、jar ファイルを置き換えて再起動するだけです。

### Docker

`JMusicBot-ML.jar` を、このリポジトリの `Dockerfile` と同じ場所に置きます:

```bash
docker build -t jmusicbot-ml .
docker run -d --name jmusicbot-ml --restart unless-stopped -v "$(pwd)/data:/opt/jmusicbot-ml/data" jmusicbot-ml
docker logs -f jmusicbot-ml
```

`config.txt` などのファイルは `data` フォルダーに保存されます。

<a id="s7"></a>
## 7. 任意の設定

すべて `config.txt` にあり、各設定の説明もファイル内に書かれています。主なもの:

| 設定 | 初期値 | 内容 |
|---|---|---|
| `language` | `"en"` | ボットの言語: `en`、`tr`、`ja`、`de`、`es`、`ru`。 |
| `spclient`, `spsecret` | 空 | Spotify の API キー。**必須ではありません**。空の場合は Spotify の公開ページを使います。設定すると曲の照合が少し正確になります。 |
| `youtubeoauth2` | `false` | Google アカウントで YouTube にログインします（年齢制限のある動画用）。下記参照。 |
| `failnotice` | `"none"` | 曲がまったく再生できなかったときの通知先: `"none"`（通知なし、コンソールのみ）、`"dm"`（リクエストした人に DM）、`"channel"`（チャンネルに投稿）。 |
| `cachelimitmb` | `1024` | ダウンロードした曲を保存する `cache` フォルダーの最大サイズ（MB）。超えると、最も長く再生されていない曲から削除されます。 |
| `altprefix` | `"NONE"` | 2 つ目のプレフィックス（例: `"?"`）。 |

### Spotify のキー（任意）

1. https://developer.spotify.com/dashboard → **Create app**。
2. Redirect URI に `https://example.com` と入力し、**Web API** を選びます。
3. *Settings* ページの **Client ID** と **Client secret** を `spclient` と `spsecret` にコピーします。

2026 年から、Spotify は開発者アプリのオーナーに **Premium** を求めています。Premium がない場合はこれらを空のままにしてください。ボットはそれでも動作します。他の人のプレイリストは常に公開ページから読み込まれます（1 リストにつき最大 100 曲）。

### YouTube アカウント（年齢制限のある動画のみ）

通常の動画はアカウントなしで再生できます。YouTube の **18 歳以上向け** の動画は、ログインしたアカウントでないと開けません。

1. `config.txt` で `youtubeoauth2 = true` にしてボットを再起動します。
2. コンソールに `go to https://www.google.com/device and enter code XXX-XXX-XXX` という行が表示されます。
3. そのページを開いてコードを入力し、アカウントを選びます。ボットがログインを自動で検出し、キーを `youtube-oauth.txt` に保存します。再ログインは不要です。

> ⚠️ **メインのアカウントは使わないでください。** ボット専用の Google アカウントを別に作成してください（生年月日は 18 歳以上）。YouTube の規約は自動アクセスを認めておらず、アカウントが制限される可能性があります。`youtube-oauth.txt` は絶対に共有しないでください。接続は *Google アカウント → セキュリティ → サードパーティとの接続* からいつでも解除できます。

別の方法: Netscape 形式の `cookies.txt` をボットのフォルダーに置くと、yt-dlp のフォールバックがそのアカウントでダウンロードします。Cookie ファイルはアカウントの Google セッション全体へのアクセスを与えるため、より危険です。

<a id="s8"></a>
## 8. コマンド

すべてのコマンドは `/` でもプレフィックスでも使えます。全一覧は `/help` で表示されます。コマンドの説明はあなたの Discord の言語で表示されます。

| コマンド | 説明 |
|---|---|
| `/play` | 曲名、または YouTube / Spotify / SoundCloud のリンクを再生 |
| `/spotify` | Spotify のリンクを情報カード付きで再生 |
| `/search` | YouTube で検索して選択 |
| `/nowplaying` | ボタン付きの「再生中」カード |
| `/queue` | キューを表示 |
| `/skip` | スキップに投票 |
| `/lyrics` | 歌詞（`/lyrics Coldplay - Yellow` のようにも使えます） |
| `/volume` | 音量（0-150） |
| `/shuffle` | 自分の曲をシャッフル |
| `/seek` | 早送り / 巻き戻し |
| `/remove` | キューから曲を削除 |
| `/pause` | 一時停止（DJ） |
| `/stop` | 停止してキューを空にする（DJ） |
| `/forceskip` | 投票なしでスキップ（DJ） |
| `/playnext` | キューの先頭に追加（DJ） |
| `/repeat` | リピートモード（DJ） |
| `/skipto` | キュー内の曲にスキップ（DJ） |
| `/movetrack` | キュー内の曲を移動（DJ） |
| `/playlists` | サーバーのプレイリスト |
| `/mylist` | 個人のプレイリスト |
| `/settings` | サーバーの設定を表示 |
| `/setdj` | DJ ロールを設定（管理者） |
| `/settc` | コマンドを使えるチャンネルを制限（管理者） |
| `/help` | すべてのコマンドを表示 |

<a id="s9"></a>
## 9. トラブルシューティング

| 症状 | 対処 |
|---|---|
| ボットが起動しない、`UnsupportedClassVersionError` | Java が古すぎます。`java -version` で **25** と表示される必要があります。 |
| コンソールに `トークンが入力されていません` / `Invalid token` | `config.txt` の `token` を確認してください（引用符の中、空白なし）。 |
| Developer Portal で Public Bot をオフにできない | 先に **Installation → Install Link = None** にしてください。 |
| スラッシュコマンドが表示されない | Discord を再読み込みします（Ctrl+R）。招待リンクに `applications.commands` が含まれている必要があります。 |
| `!play` は動かないが `/play` は動く | Developer Portal で **MESSAGE CONTENT INTENT** がオンになっている必要があります。 |
| ボットがチャンネルに入ってすぐ抜ける | コンソールの `YouTubeのストリームを開けませんでした` と `yt-dlp error:` の行を確認してください。`Sign in to confirm your age` とあれば年齢制限のある動画です（[7 章](#s7)）。 |
| `yt-dlp exit code=255` | yt-dlp が起動できませんでした（ディスクがいっぱいの可能性）。ボットが一時ファイルを片付けて一度だけ再試行します。ディスクの空きを増やしてください。 |
| Spotify のリンクが再生されない | リンクが公開されている必要があります。ポッドキャストのリンクには対応していません。 |

不具合を報告するときは、関係するコンソールの行とボットのバージョン（起動時に表示される `JMusicBot-ML vX.Y.Z`）を添えてください。**トークンや `config.txt` は絶対に共有しないでください。**

<a id="s10"></a>
## 10. ソースからビルドする

JDK 25 が必要です:

```bash
./mvnw -B -DskipTests package
# 出力: target/JMusicBot-ML-All.jar
```

Windows では `mvnw.cmd -B -DskipTests package`。`v` で始まるタグ（例: `v1.0.1`）をプッシュすると、GitHub Actions が jar をビルドして **Release** に自動で添付します。

<a id="s-translations"></a>
### 翻訳

すべてのテキストは [`src/main/resources/lang/`](src/main/resources/lang/) にあります: `en.json`、`tr.json`、`ja.json`、`de.json`、`es.json`、`ru.json`、そして `lang/config/` に `config.txt` のテンプレートがあります。キーが足りない場合は英語が使われます。言語を追加するには、`en.json` と `config/en.conf` をコピーして値を翻訳し、[`L.java`](src/main/java/dev/cosgy/jmusicbot/i18n/L.java) の `LANGUAGES` に言語コードを追加します。翻訳の改善は大歓迎です。

変更履歴: [CHANGELOG.md](CHANGELOG.md)

<a id="s-license"></a>
## ライセンスと謝辞

JMusicBot-ML は、ベースにしたプロジェクトと同じ [Apache License 2.0](LICENSE) で公開されています。

### 元のプロジェクト

| プロジェクト | 作者 | 内容 |
|---|---|---|
| [jagrosh/MusicBot](https://github.com/jagrosh/MusicBot) | John Grosh（jagrosh） | オリジナルの JMusicBot |
| [Cosgy-Dev/JMusicBot-JP](https://github.com/Cosgy-Dev/JMusicBot-JP) | Cosgy Dev | 日本語版（バージョン 0.11.0 が JMusicBot-ML のベース） |

元の著作権表示はソースファイル内にそのまま残しています。JMusicBot-ML で追加したファイルと変更には「JMusicBot-ML contributors」と記載しています。変更内容は [CHANGELOG.md](CHANGELOG.md) に、その要約は [NOTICE](NOTICE) にあります。

### ライブラリとサービス

[JDA](https://github.com/discord-jda/JDA) · [Lavaplayer](https://github.com/lavalink-devs/lavaplayer) · [youtube-source](https://github.com/lavalink-devs/youtube-source) · [yt-dlp](https://github.com/yt-dlp/yt-dlp)（実行時にダウンロード） · [JDAVE](https://github.com/MinnDevelopment/jdave) · [Logback](https://logback.qos.ch) · [Typesafe Config](https://github.com/lightbend/config) · [jsoup](https://jsoup.org) · [Unirest](https://github.com/Kong/unirest-java) · [JFiglet](https://github.com/lalyos/jfiglet) · [JLyrics](https://github.com/Cosgy-Dev/JLyrics) · 歌詞: [LRCLIB](https://lrclib.net)。それぞれ各自のライセンスのもとで使用しています。[NOTICE](NOTICE) を参照してください。

### Claude による開発

JMusicBot-ML は、Anthropic が開発した AI アシスタント **[Claude](https://claude.ai)** の助けを借りて開発されました。このバージョンの変更の大部分（6 言語への翻訳、新しい Spotify 対応、「再生中」カード、yt-dlp によるフォールバック、不具合の修正）は Claude が書き、プロジェクトのオーナーが各バージョンを実際のサーバーでテストし、何を作るかを決めました。Claude と一緒に作成したコミットには `Co-Authored-By: Claude` と記載されています。

このボットは YouTube、Spotify、Discord とは関係がなく、それらの承認も受けていません。自分のサーバーで、各サービスの規約を守ってご利用ください。
