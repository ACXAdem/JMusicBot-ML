# JMusicBot-ML — Çok Dilli JMusicBot

[English](README.md) · **Türkçe** · [日本語](README.ja.md) · [Deutsch](README.de.md) · [Español](README.es.md) · [Русский](README.ru.md)

**JMusicBot-ML** (*ML = Multi-Language, yani çok dilli*), tanınmış [JMusicBot](https://github.com/jagrosh/MusicBot)'un resmi olmayan, çok dilli bir sürümüdür: [JMusicBot-JP](https://github.com/Cosgy-Dev/JMusicBot-JP) 0.11.0 tabanlı, **6 dilde** (English, Türkçe, 日本語, Deutsch, Español, Русский) kullanılabilen ve kendi sunucunuzda çalıştırabileceğiniz bir Discord müzik botu.

YouTube, YouTube Music, **Spotify**, SoundCloud ve internet radyolarından müzik çalar. Kurulumu tek bir `.jar` dosyasından ibarettir.

> **Dayandığı projeler:** [jagrosh/MusicBot](https://github.com/jagrosh/MusicBot) (JMusicBot, John Grosh) → [Cosgy-Dev/JMusicBot-JP](https://github.com/Cosgy-Dev/JMusicBot-JP) (Japonca sürüm, Cosgy Dev) → JMusicBot-ML.
> Botun asıl emeği bu projelere aittir. JMusicBot-ML bağımsız, değiştirilmiş bir sürümdür; onlarla bağlantılı değildir ve onlar tarafından onaylanmamıştır. Ayrıntılar: [Lisans ve teşekkür](#lisans-ve-teşekkür).

## Özellikler

- **6 dil:** bot mesajları, `config.txt` açıklamaları ve konsol kayıtları seçtiğiniz dilde (`config.txt` → `language`). Slash komut açıklamaları her kullanıcıya **kendi Discord dilinde** gösterilir.
- **Türkçe komut adları:** `/play` = `/oynat`, `!play` = `!çal`, `/skip` = `/gec`… (`language = "tr"` iken iki ad da ayrı komut olarak görünür; diğer dillerde Discord'u Türkçe olanlar Türkçe adı görür.)
- **Spotify:** şarkı, albüm, çalma listesi ve sanatçı linkleri. Spotify ses vermediği için aynı şarkı YouTube Music / YouTube'da otomatik bulunup oradan çalınır. API anahtarı **gerekmez**.
- **Düğmeli "Şimdi çalıyor" kartı:** kapak resmi, ilerleme çubuğu ve ⏮ ⏸ ⏭ ⏹ 🔁 🔉 🔊 🔀 📜 🎤 düğmeleri.
- **Şarkı sözleri:** [LRCLIB](https://lrclib.net) üzerinden (anahtar gerekmez, Türkçe şarkılar dahil).
- **YouTube radyo / mix linkleri:** onay sormadan en fazla 75 şarkı eklenir.
- **yt-dlp yedeği:** YouTube bir şarkıyı vermezse bot onu [yt-dlp](https://github.com/yt-dlp/yt-dlp) ile indirip çalar. yt-dlp ilk açılışta kendiliğinden indirilir ve güncel tutulur.
- **Bakım gerektirmez:** indirilen şarkı önbelleği ve günlük dosyaları sınırlıdır, eskiler kendiliğinden silinir.

---

## İçindekiler

1. [Gereksinimler](#1-gereksinimler)
2. [Discord'da bot oluşturma](#2-discordda-bot-oluşturma)
3. [Botu indirme](#3-botu-indirme)
4. [İlk çalıştırma ve config.txt](#4-ilk-çalıştırma-ve-configtxt)
5. [Botu sunucunuza davet etme](#5-botu-sunucunuza-davet-etme)
6. [Çalıştırma yöntemleri](#6-çalıştırma-yöntemleri) — Windows · Linux · Pterodactyl · Docker
7. [İsteğe bağlı ayarlar](#7-isteğe-bağlı-ayarlar)
8. [Komutlar](#8-komutlar)
9. [Sorun giderme](#9-sorun-giderme)
10. [Kaynaktan derleme](#10-kaynaktan-derleme)

---

## 1. Gereksinimler

- **Java 25** (64 bit). Daha eski Java sürümleriyle çalışmaz.
- Bir Discord hesabı ve botu ekleyeceğiniz sunucuda **Sunucuyu Yönet** yetkisi.
- ~150 MB boş disk (jar + yt-dlp) ve önbellek için ek alan (varsayılan en fazla 1 GB).
- 512 MB RAM yeterlidir.

## 2. Discord'da bot oluşturma

1. https://discord.com/developers/applications adresine gidin → **New Application** → bir ad verin.
2. Sol menüden **Installation** → *Install Link* değerini **None** yapın ve kaydedin.
   (Bunu yapmadan bir sonraki adımda "Özel uygulama varsayılan yetkilendirme bağlantısına sahip olamaz" hatası alırsınız.)
3. Sol menüden **Bot**:
   - **Public Bot** seçeneğini **kapatın** (botu yalnızca siz ekleyebilirsiniz).
   - **Privileged Gateway Intents** altında **MESSAGE CONTENT INTENT** seçeneğini **açın** (`!çal` gibi önekli komutlar için gerekir).
   - **Reset Token** → çıkan **token**'ı kopyalayın. Bunu kimseyle paylaşmayın; token'ı bilen herkes botunuzu kontrol edebilir.
4. Kendi **Discord kullanıcı kimliğinizi (ID)** alın: Discord → Ayarlar → Gelişmiş → **Geliştirici Modu**'nu açın, sonra kendi adınıza sağ tıklayın → **Kullanıcı ID'sini Kopyala**.

## 3. Botu indirme

Bu sayfanın sağındaki **Releases** bölümünden en son `JMusicBot-ML.jar` dosyasını indirin ve **boş bir klasöre** koyun. Bot ayar dosyalarını, önbelleği ve günlükleri bu klasöre yazar.

## 4. İlk çalıştırma ve config.txt

Botu bir kez çalıştırın (komutlar için [6. bölüme](#6-çalıştırma-yöntemleri) bakın). Klasörde `config.txt` oluşur. Dosyayı bir metin düzenleyiciyle açıp en azından şunları doldurun:

```
language = "tr"               // en, tr, ja, de, es veya ru
token = "BOT_TOKEN_BURAYA"     // 2. adımda kopyaladığınız token
owner = SAHIP_ID_BURAYA        // kendi Discord kullanıcı kimliğiniz (tırnaksız sayı)
prefix = "!"                   // önekli komutlar için (ör. !çal)
```

Kaydedip botu yeniden başlatın. Konsolda `Finished Loading!` ve `… slash komutu Discord'a kaydedildi` satırlarını görüyorsanız bot hazırdır.

> `language` değerini değiştirip botu yeniden başlatırsanız `config.txt` açıklamaları da o dile çevrilir. Ayarlarınız korunur ve eski dosyanın yedeği (`config.txt.vX.bak`) alınır. Yeni sürümlerde eklenen ayarlar için de aynısı kendiliğinden yapılır.
>
> İlk açılışta dosya İngilizce oluşturulur; Türkçe için `language = "tr"` yazıp yeniden başlatın.

## 5. Botu sunucunuza davet etme

Aşağıdaki linkte `UYGULAMA_ID` yerine uygulamanızın **Application ID**'sini (Developer Portal → *General Information*) yazıp tarayıcıda açın:

```
https://discord.com/oauth2/authorize?client_id=UYGULAMA_ID&scope=bot%20applications.commands&permissions=281475047352384
```

Bu link, botun ihtiyaç duyduğu yetkileri (mesaj gönderme, bağlantı ekleme, ses kanalına bağlanma ve konuşma, takma ad değiştirme, ses kanalı durumu…) ister.

## 6. Çalıştırma yöntemleri

Tüm yöntemlerde kullanılan komut aynıdır:

```
java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
```

### Windows

Komut İstemi'ni (cmd) açıp Java 25'i kurun:

```cmd
winget install EclipseAdoptium.Temurin.25.JRE
```

Sonra jar'ın olduğu klasöre gidip botu başlatın:

```cmd
cd C:\Users\KULLANICI\Desktop\jmusicbot
java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
```

İsterseniz bu satırı aynı klasörde `baslat.bat` adlı bir dosyaya yazıp çift tıklayarak açabilirsiniz. `-Dnogui=true` kısmını silerseniz bot küçük bir pencereyle açılır.

### Linux (Ubuntu / Debian)

```bash
sudo apt update
sudo apt install openjdk-25-jre-headless
mkdir ~/jmusicbot && cd ~/jmusicbot     # JMusicBot-ML.jar'ı buraya koyun
java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
```

Paket deposunda Java 25 yoksa [Adoptium Temurin 25](https://adoptium.net/) kullanın.

Bilgisayar açılınca kendiliğinden başlasın isterseniz `/etc/systemd/system/jmusicbot.service` dosyası oluşturun (`KULLANICI` yerine kendi kullanıcı adınızı yazın):

```ini
[Unit]
Description=JMusicBot-ML muzik botu
After=network-online.target

[Service]
User=KULLANICI
WorkingDirectory=/home/KULLANICI/jmusicbot
ExecStart=/usr/bin/java --enable-native-access=ALL-UNNAMED -Dnogui=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar JMusicBot-ML.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable --now jmusicbot
journalctl -u jmusicbot -f     # konsolu izlemek için
```

### Pterodactyl

1. Panelde **Admin → Nests → Import Egg** ile depodaki [`pterodactyl/egg-jmusicbot-ml.json`](pterodactyl/egg-jmusicbot-ml.json) dosyasını içe aktarın.
2. Bu egg ile yeni bir sunucu oluşturun (512 MB RAM, 2 GB disk yeterli). Docker imajı: `ghcr.io/pterodactyl/yolks:java_25`.
3. Sunucunun **Dosya Yöneticisi**'nden `JMusicBot-ML.jar` dosyasını yükleyin.
4. **Start** deyin. İlk açılışta `config.txt` oluşur. Dosya Yöneticisi'nden `token`, `owner` ve `language` değerlerini girip yeniden başlatın.

Güncelleme için yalnızca jar dosyasını değiştirip yeniden başlatmanız yeterlidir.

### Docker

`JMusicBot-ML.jar` dosyasını depodaki `Dockerfile` ile aynı klasöre koyun:

```bash
docker build -t jmusicbot-ml .
docker run -d --name jmusicbot-ml --restart unless-stopped -v "$(pwd)/data:/opt/jmusicbot-ml/data" jmusicbot-ml
docker logs -f jmusicbot-ml
```

`config.txt` ve diğer dosyalar `data` klasöründe tutulur.

## 7. İsteğe bağlı ayarlar

Hepsi `config.txt` içindedir ve her birinin açıklaması dosyanın kendisinde yazar. En önemlileri:

| Ayar | Varsayılan | Ne işe yarar |
|---|---|---|
| `language` | `"en"` | Botun dili: `en`, `tr`, `ja`, `de`, `es`, `ru`. |
| `spclient`, `spsecret` | boş | Spotify API anahtarları. **Zorunlu değil**; boşken Spotify'ın herkese açık sayfası kullanılır. Anahtar girilirse eşleşme biraz daha isabetli olur. |
| `youtubeoauth2` | `false` | YouTube'a Google hesabıyla giriş (yaş sınırlı videolar için). Aşağıya bakın. |
| `failnotice` | `"none"` | Bir şarkı hiç çalınamazsa kime haber verilsin: `"none"` (kimseye, yalnızca konsol), `"dm"` (şarkıyı isteyene özel mesaj), `"channel"` (kanala). |
| `cachelimitmb` | `1024` | İndirilen şarkıların tutulduğu `cache` klasörünün en fazla boyutu (MB). Aşılınca en uzun süredir çalınmayanlar silinir. |
| `altprefix` | `"NONE"` | İkinci bir önek (ör. `"?"`). |

### Spotify anahtarı (isteğe bağlı)

1. https://developer.spotify.com/dashboard → **Create app**.
2. Redirect URI olarak `https://example.com` yazın, **Web API**'yi seçin.
3. *Settings* sayfasındaki **Client ID** ve **Client secret** değerlerini `spclient` ve `spsecret` alanlarına yazın.

Spotify 2026'dan beri geliştirici uygulamaları için uygulama sahibinin **Premium** olmasını istiyor. Premium'unuz yoksa bu alanları boş bırakın; bot yine çalışır. Başkalarına ait çalma listeleri her durumda herkese açık sayfadan okunur (liste başına en fazla 100 şarkı).

### YouTube hesabı (yalnızca yaş sınırlı videolar için)

Normal videolar hesapsız çalar. YouTube'un **+18 işaretli** videoları ise yalnızca giriş yapılmış bir hesapla açılır.

1. `config.txt`'de `youtubeoauth2 = true` yapıp botu yeniden başlatın.
2. Konsolda `go to https://www.google.com/device and enter code XXX-XXX-XXX` satırı çıkar.
3. Bu adresi açın, kodu girin ve hesabı seçin. Bot girişi kendisi algılar, anahtarı `youtube-oauth.txt` dosyasına kaydeder; bir daha giriş gerekmez.

> ⚠️ **Ana hesabınızı kullanmayın.** Yalnızca bot için ayrı bir Google hesabı açın (doğum tarihi 18 yaş üstü olmalı). YouTube kuralları otomatik erişime izin vermez; hesap kısıtlanabilir. `youtube-oauth.txt` dosyasını kimseyle paylaşmayın. Bağlantıyı istediğiniz an *Google Hesabı → Güvenlik → Üçüncü taraf bağlantıları*'ndan kaldırabilirsiniz.

Alternatif: bot klasörüne Netscape biçiminde bir `cookies.txt` koyarsanız yt-dlp yedeği o hesapla indirir. Çerez dosyası hesabın tüm Google oturumuna erişim verdiği için daha risklidir.

## 8. Komutlar

Her komut `/` ile ya da öneki ile kullanılabilir. Önekli kullanımda Türkçe karakterli yazım da çalışır (`!çal`, `!geç`, `!sıra`). Tam liste için `/yardim` yazın.

| İngilizce | Türkçe | Açıklama |
|---|---|---|
| `/play` | `/oynat` (`!çal`) | Şarkı adı, YouTube / Spotify / SoundCloud linki çalar |
| `/spotify` | — | Spotify linkini bilgi kartıyla çalar |
| `/search` | `/ara` | YouTube'da arayıp seçtirir |
| `/nowplaying` | `/calan` | Düğmeli "Şimdi çalıyor" kartı |
| `/queue` | `/kuyruk` | Kuyruğu gösterir |
| `/skip` | `/gec` | Geçmek için oy verir |
| `/lyrics` | `/sozler` | Şarkı sözleri (`/sozler Tarkan - Şımarık` da olur) |
| `/volume` | `/ses` | Ses seviyesi (0-150) |
| `/shuffle` | `/karistir` | Sıradaki şarkılarınızı karıştırır |
| `/seek` | `/sar` | İleri / geri sarar |
| `/remove` | `/kaldir` | Kuyruktan şarkı kaldırır |
| `/pause` | `/duraklat` | Duraklatır (DJ) |
| `/stop` | `/durdur` | Durdurur ve kuyruğu temizler (DJ) |
| `/forceskip` | `/zorlagec` | Oylamasız geçer (DJ) |
| `/playnext` | `/siradaki` | Kuyruğun en önüne ekler (DJ) |
| `/repeat` | `/tekrar` | Tekrar modu (DJ) |
| `/skipto` | `/atla` | Kuyruktaki bir şarkıya atlar (DJ) |
| `/movetrack` | `/tasi` | Kuyrukta şarkı taşır (DJ) |
| `/playlists` | `/listeler` | Sunucu çalma listeleri |
| `/mylist` | `/listem` | Kişisel çalma listeleri |
| `/settings` | `/ayarlar` | Sunucu ayarlarını gösterir |
| `/setdj` | `/djayarla` | DJ rolünü ayarlar (Yönetici) |
| `/settc` | `/metinkanali` | Komutların kullanılacağı kanalı sınırlar (Yönetici) |
| `/help` | `/yardim` | Tüm komutları listeler |

## 9. Sorun giderme

| Belirti | Çözüm |
|---|---|
| Bot açılmıyor, `UnsupportedClassVersionError` | Java sürümünüz eski. `java -version` **25** göstermeli. |
| Konsolda `Token girilmedi` / `Invalid token` | `config.txt`'deki `token` değerini kontrol edin (tırnak içinde, boşluksuz). |
| Developer Portal'da Public Bot kapanmıyor | Önce **Installation → Install Link = None** yapın. |
| Slash komutları görünmüyor | Discord'u yeniden başlatın (Ctrl+R). Davet linkinde `applications.commands` olmalı. |
| `!çal` çalışmıyor ama `/oynat` çalışıyor | Developer Portal'da **MESSAGE CONTENT INTENT** açık olmalı. |
| Bot kanala girip hemen çıkıyor | Konsolda `YouTube akışı açılamadı` ve `yt-dlp hatası:` satırlarına bakın. `Sign in to confirm your age` yazıyorsa video yaş sınırlıdır ([7. bölüm](#youtube-hesabı-yalnızca-yaş-sınırlı-videolar-için)). |
| `yt-dlp çıkış kodu=255` | yt-dlp başlayamadı (disk dolu olabilir). Bot temizleyip bir kez daha dener; diskte yer açın. |
| Spotify linki çalmıyor | Link herkese açık olmalı. Podcast linkleri desteklenmez. |

Bir hata bildirirken konsoldaki ilgili satırları ve botun sürümünü (açılıştaki `JMusicBot-ML vX.Y.Z` yazısı) ekleyin. **Token'ınızı ve `config.txt`'yi asla paylaşmayın.**

## 10. Kaynaktan derleme

JDK 25 gerekir:

```bash
./mvnw -B -DskipTests package
# çıktı: target/JMusicBot-ML-All.jar
```

Windows'ta `mvnw.cmd -B -DskipTests package`. Depoya `v` ile başlayan bir etiket (ör. `v1.0.1`) gönderildiğinde GitHub Actions jar'ı derleyip **Releases**'a kendisi ekler.

Çeviriler `src/main/resources/lang/` klasöründedir (eksik anahtarlarda İngilizce kullanılır).

Değişiklik geçmişi: [CHANGELOG.md](CHANGELOG.md) · [DEGISIKLIKLER.md](DEGISIKLIKLER.md)

## Lisans ve teşekkür

JMusicBot-ML, dayandığı projelerle aynı lisans olan [Apache License 2.0](LICENSE) ile yayınlanır.

### Dayandığı projeler

| Proje | Geliştiren | Nedir |
|---|---|---|
| [jagrosh/MusicBot](https://github.com/jagrosh/MusicBot) | John Grosh (jagrosh) | Orijinal JMusicBot |
| [Cosgy-Dev/JMusicBot-JP](https://github.com/Cosgy-Dev/JMusicBot-JP) | Cosgy Dev | Japonca sürüm (JMusicBot-ML'un temeli 0.11.0 sürümüdür) |

Kaynak dosyalardaki orijinal telif bildirimleri korunmuştur. JMusicBot-ML'un eklediği dosya ve değişiklikler "JMusicBot-ML contributors" olarak işaretlidir. Neyin değiştiği [DEGISIKLIKLER.md](DEGISIKLIKLER.md) dosyasında ayrıntılı, [NOTICE](NOTICE) dosyasında özet olarak yazar: 6 dile çeviri, yeniden yazılan Spotify desteği, düğmeli "Şimdi çalıyor" kartı, yeni şarkı sözü kaynağı, yt-dlp yedeği, önbellek ve günlük sınırları ve pek çok hata düzeltmesi.

### Kütüphaneler ve hizmetler

[JDA](https://github.com/discord-jda/JDA) · [Lavaplayer](https://github.com/lavalink-devs/lavaplayer) · [youtube-source](https://github.com/lavalink-devs/youtube-source) · [yt-dlp](https://github.com/yt-dlp/yt-dlp) (çalışırken indirilir) · [JDAVE](https://github.com/MinnDevelopment/jdave) · [Logback](https://logback.qos.ch) · [Typesafe Config](https://github.com/lightbend/config) · [jsoup](https://jsoup.org) · [Unirest](https://github.com/Kong/unirest-java) · [JFiglet](https://github.com/lalyos/jfiglet) · [JLyrics](https://github.com/Cosgy-Dev/JLyrics) · şarkı sözleri: [LRCLIB](https://lrclib.net). Her biri kendi lisansıyla kullanılır; bkz. [NOTICE](NOTICE).

### Claude ile geliştirildi

JMusicBot-ML, Anthropic'in geliştirdiği yapay zekâ asistanı **[Claude](https://claude.ai)** yardımıyla geliştirildi. Bu sürümdeki değişikliklerin çoğunu (altı dile çeviri, yeni Spotify desteği, "Şimdi çalıyor" kartı, yt-dlp yedeği ve hata düzeltmeleri) Claude yazdı; proje sahibi her sürümü gerçek bir sunucuda test etti ve neyin yapılacağına karar verdi. Claude ile yapılan commit'ler `Co-Authored-By: Claude` ile işaretlidir.

Bu bot YouTube, Spotify veya Discord ile bağlantılı değildir ve onlar tarafından onaylanmamıştır. Kendi sunucunuzda, ilgili hizmetlerin kullanım koşullarına uyarak kullanın.
