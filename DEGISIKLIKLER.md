# Değişiklikler

## JMusicBot-ML v1.0.0 — ilk açık sürüm
- **Çok dilli:** bot İngilizce (varsayılan), Türkçe, Japonca, Almanca, İspanyolca ve Rusça konuşuyor. config.txt'deki `language` ile seçilir; tüm bot mesajları, konsol kayıtları ve config.txt açıklamaları seçilen dile geçer.
- Slash komut açıklamaları altı dilde kaydedilir; herkes kendi Discord dilinde görür. Discord'u Türkçe olanlar Türkçe komut adlarını (`/oynat` …) görür; `language = "tr"` iken bunlar ayrıca ayrı komut olarak da kaydedilir.
- `language` değişince config.txt o dilde yeniden yazılır (ayarlar korunur, yedek alınır). Geliştirme sürümlerinden kalan Türkçe config.txt dosyaları otomatik olarak `language = "tr"` ile devam eder (config.txt v7).
- API anahtarsız Spotify (şarkı / albüm / liste / sanatçı), düğmeli "Şimdi çalıyor" kartı, LRCLIB şarkı sözleri, YouTube radyo/mix desteği (en fazla 75 şarkı), yt-dlp yedeği, isteğe bağlı YouTube girişi, önbellek ve günlük sınırları — ayrıntılar aşağıdaki geliştirme sürümlerinde.
- Kod yorumları, GitHub şablonları, Dockerfile ve Pterodactyl egg'i İngilizce; README altı dilde.
- README ve NOTICE dayanılan projeleri belirtiyor ve projenin Claude (Anthropic) yardımıyla geliştirildiğini yazıyor.

# 1.0 öncesi geliştirme sürümleri (özel Türkçe sürüm)

## PLEBISTAN v0.1.11
- GitHub'da yayınlamaya hazırlık: yeni kurulum rehberi (README), GitHub Actions ile otomatik derleme ve sürüm yayınlama, Pterodactyl egg'i depoya eklendi, `.gitignore` gizli dosyaları (config.txt, youtube-oauth.txt, cookies.txt) dışarıda tutacak şekilde güncellendi.
- Orijinal projenin (Cosgy-Dev) GitHub sürümlerini denetleyip sahibe "yeni sürüm var" DM'i atan güncelleme kontrolü kapatıldı.
- Derleme eklentisi (maven-shade) güncellendi; çıktı adı `JMusicBot-TR-All.jar`.

## PLEBISTAN v0.1.10
- **Çalınamayan şarkı mesajı artık sohbete yazılmıyor:** config.txt'ye `failnotice` eklendi. Varsayılan `"none"`: bot yalnızca konsola yazar ve sessizce sıradaki şarkıya geçer. `"dm"`: yalnızca şarkıyı isteyen kişiye özel mesaj (yönetici ayar bilgisi olmadan). `"channel"`: eski davranış.
- config.txt v4 → v5 otomatik güncellenir.

## PLEBISTAN v0.1.9
- **Konsol sadeleşti:** YouTube kütüphanesinin her hatada döktüğü dev JSON satırları, yığın izleri (stack trace) ve 10 dakikada bir yazılan "visitor id" satırı kaldırıldı. Yerine tek satırlık Türkçe özet yazılıyor: `YouTube akışı açılamadı (id=…): ANDROID_VR: giriş gerekli, WEB: ses akışı yok, … → yt-dlp deneniyor`.
- Açılıştaki JDA "CacheFlag" uyarıları giderildi.
- **Günlük dosyaları (logs klasörü) kendiliğinden temizleniyor:** her gün yeni dosya (10 MB'ı geçerse bölünür), eski dosyalar sıkıştırılır; 7 günden eskiler ve toplam 100 MB'ı aşan kısım silinir. Eski sürümlerden kalan 7 günden eski dosyalar da açılışta silinir.

## PLEBISTAN v0.1.8
- **Önbellek sınırı ayarlanabilir:** config.txt'ye `cachelimitmb` eklendi (varsayılan 1024 MB, en az 100). `cache` klasörü bu boyutu geçince en uzun süredir çalınmayan şarkılar silinir (klasör sınırın %80'ine inene kadar). Tekrar çalınan şarkı "yeni" sayılır ve silinmez. Temizlik açılışta da yapılır.
- config.txt v3 → v4 otomatik güncellenir (eski ayarlar korunur, yedeği alınır).

## PLEBISTAN v0.1.7
- **"yt-dlp çıkış kodu=255" düzeltildi:** yt-dlp her çalışmada kendini geçici klasöre açar. Pterodactyl'de /tmp küçük (~100 MB) bir bellek diski olduğu için bot uzun süre açık kalınca (yarım kalan işlemlerin artıkları, aynı anda iki yt-dlp) /tmp doluyor ve yedek indirme anında başarısız oluyordu. Bu yüzden bir bot çalabildiği şarkıyı diğeri çalamıyordu. Artık geçici klasör bot klasöründeki `tmp` (disk) içinde; artıklar kendiliğinden temizleniyor ve 255 hatasında temizlenip bir kez daha deneniyor.
- **Önbellek sınırı:** `cache` klasörü sınırsız büyüyordu; artık 1 GB'ı geçince en eski dosyalar siliniyor.

## PLEBISTAN v0.1.6
- **Google hesabıyla YouTube girişi düzeltildi:** `youtubeoauth2 = true` ayarı eskiden hiçbir işe yaramıyordu (hesabı kullanabilen TV istemcisi listede yoktu). Artık açılınca konsolda bir kod çıkar, https://www.google.com/device adresinde girilir; bot girişi kendisi algılar ve anahtarı `youtube-oauth.txt` dosyasına kaydeder. Normal videolar yine hesapsız çalınır, hesap yalnızca yaş sınırlı / "giriş gerekli" videolarda devreye girer.
- **cookies.txt desteği:** bot klasörüne `cookies.txt` konursa yt-dlp yedeği YouTube'a o hesapla girer.
- **Bot artık sessizce gitmiyor:** çalınamayan şarkı için kanala sebebiyle birlikte mesaj yazılır (yaş sınırı, gizli video, ülke engeli, kaldırılmış video…).
- Konsolda yt-dlp'nin asıl hata mesajı gösterilir (eskiden yalnızca "çıkış kodu=1" yazıyordu).
- Google hesabına bağlanılamazsa bot açılışı çökmüyor.

## PLEBISTAN v0.1.5 (kararlı sürüm)
- **Başlıklar geri geldi:** YouTube akış vermeyip yt-dlp yedeğine düşen şarkılar kuyruğa başlıksız ("Unknown title", kaynak "Dosya") düşebiliyordu. Artık her zaman orijinal başlık ve YouTube linkiyle görünüyor.
- **Karışma düzeltildi:** yedek indirme sürerken eklenen şarkılar (ör. radyo listesinin devamı) hemen çalmaya başlayıp sırayı bozuyordu. Artık indirme bitene kadar kuyrukta bekliyorlar.
- **Önbellek takılması düzeltildi:** daha önce indirilmiş bir şarkı tekrar istendiğinde bot takılabiliyordu.
- **YouTube bileşeni v0.1.1'deki sürüme (1.18.0) geri alındı:** v0.1.3'teki güncelleme sonrası sorunlar yaşandığı için.
- **Radyo sınırı:** YouTube radyo/mix listelerinden toplam en fazla **75** şarkı ekleniyor.
- Şarkıyı isteyen bilgisi, yedekten çalınan şarkılarda da korunuyor (`/seek` ve kuyruk kaydı artık hata vermiyor).
- Bu sürüm, YouTube hatası, yedek indirme, önbellek, durdurma, radyo sınırı ve 30 şarkılık karışık kuyruk senaryolarıyla otomatik olarak test edildi.

## PLEBISTAN v0.1.4
- **Durdur artık gerçekten durduruyor:** radyo listesinin devamını ekleyen arka plan işi durdurduktan sonra da çalışmaya devam ediyor, bot yeniden kanala girince eski listenin şarkılarını yeni kuyruğa karıştırıyordu. Artık her durdurmada arka plandaki işler iptal ediliyor.
- Durdurma sırasında yt-dlp ile indirilmekte olan şarkı, indirme bitince çalınmıyor.
- Durdurunca "önceki şarkı" geçmişi, oylar ve duraklatma durumu da sıfırlanıyor.
- Durdururken otomatik çalma listesi (autoplaylist) tetiklenmiyor.
- Bot ses kanalından çıkarılırsa (biri bağlantısını keserse) kuyruk temizleniyor.

## PLEBISTAN v0.1.3
- **Takılma düzeltildi:** YouTube bir şarkının akışını vermeyince devreye giren yt-dlp yedeği başarısız olursa bot sessizce takılı kalıyordu; artık sıradaki şarkıya geçiyor.
- **"Aynı şarkıyı bir daha çalmıyor" düzeltildi:** bir kez yedeğe düşen şarkı, tekrar çalınınca yedek denenmiyor ve bot takılıyordu. Bu kısıt kaldırıldı.
- yt-dlp ile indirilen şarkılar `cache` klasöründe tutuluyor; aynı şarkı tekrar istendiğinde yeniden indirilmeden anında çalıyor.
- Yedek indirme sürerken `/np` artık "müzik çalmıyor" yerine "⏳ Şarkı hazırlanıyor" gösteriyor.
- Yedek indirme sürerken kullanıcı başka şarkı başlattıysa o şarkı kesilmiyor; indirilen şarkı kuyruğun başına ekleniyor.
- YouTube bileşeni (youtube-source) 1.18.0 → 1.18.2 güncellendi.

## PLEBISTAN v0.1.2
- Liste içeren linkler (YouTube Mix/Radyo `list=RD...&start_radio=1` ya da `list=...` içeren şarkı linki) artık onay sormuyor: şarkı ve listedeki tüm şarkılar doğrudan kuyruğa ekleniyor ("Ekle / İptal" düğmeleri kaldırıldı).
- YouTube radyo listeleri ilk yüklemede yalnızca ~25 şarkı verdiği için listenin devamı yt-dlp ile alınıp arka planda kuyruğa ekleniyor (toplam en fazla 100 şarkı). Bitince kanala "N şarkı daha eklendi" mesajı geliyor.
- Aynı şarkı listeye iki kez eklenmiyor.

## PLEBISTAN v0.1.1
- **Şarkı sözleri yeniden yazıldı:** artık LRCLIB (lrclib.net) kullanılıyor; anahtar gerekmez ve çok sayıda Türkçe şarkı içerir. Eski siteler (MusicMatch, AZLyrics…) yalnızca yedek. Çalan şarkının sanatçı, ad ve süresiyle eşleşir; YouTube başlıklarındaki "(Official Video)", "[Klip]" gibi ekler temizlenir. Uzun sözler sayfalara bölünür.
- `/lyrics` artık şarkı çalmıyorken de kullanılabilir: `/lyrics Tarkan - Şımarık`.
- **Yeni "Şimdi çalıyor" kartı:** kapak resmi, renkli kaynak (Spotify yeşil, YouTube kırmızı), ilerleme çubuğu, isteyen kişi, ses, tekrar modu, sıradaki şarkı ve kuyruk özeti.
- **Kart düğmeleri:** ⏮ önceki · ⏸/▶ duraklat/devam · ⏭ geç · ⏹ durdur · 🔁 tekrar · 🔉/🔊 ses · 🔀 karıştır · 📜 kuyruk · 🎤 sözler. Yetkiler komutlarla aynı; DJ olmayanlar geç düğmesine basınca oy verir.
- Kart, şarkı değişince ve 15 saniyede bir kendiliğinden güncellenir.
- "Önceki şarkı" desteği: son 25 şarkı hatırlanır.
- Konsol başlığı ve kart altbilgisi: PLEBISTAN v0.1.1.

## PLEBISTAN v0.1.0 (0.11.0-TR)

JMusicBot-JP 0.11.0 üzerine yapılan değişiklikler.

## Türkçeleştirme
- 92 dosyadaki yaklaşık 1.600 satır Japonca metin Türkçeye çevrildi: bot mesajları, komut açıklamaları, `config.txt` açıklamaları, konsol/günlük kayıtları, grafik arayüz, kod yorumları, README ve GitHub şablonları.
- Kategori adları Türkçe: Müzik, DJ, Genel, Yönetici, Bot sahibi.
- Ses kütüphanesinin sık görülen İngilizce hata mesajları (ör. "This video is unavailable") kullanıcıya Türkçe gösterilir.
- Konsol ve günlük dosyası UTF-8'e sabitlendi; Türkçe karakterler bozulmaz.
- Eski Japonca `config.txt`, ilk açılışta yedeklenip ayarlar korunarak Türkçe şablona taşınır.

## Türkçe komut adları
- Her komut hem İngilizce hem Türkçe adla çalışır. Örnekler: `/play` = `/oynat`, `/skip` = `/gec`, `/queue` = `/kuyruk`.
- Önekli kullanımda Türkçe karakterli yazımlar da çalışır: `!çal`, `!geç`, `!sıra`, `!karıştır`.
- Discord'u Türkçe kullananlar seçenek adlarını da Türkçe görür (`arama:`, `ad:`, `kullanici:` …).
- `/yardim` komutu, İngilizce ve Türkçe adları birlikte listeler.

## Spotify (yeniden yazıldı)
- Desteklenen linkler: şarkı, albüm, çalma listesi ve sanatçı (popüler şarkılar).
- `intl-tr` / `intl-xx` linkleri, `spotify:track:` URI'leri ve `spotify.link` kısa linkleri tanınır.
- Spotify linkleri `/play`, `!play`, `/playnext`, `/spotify`, kayıtlı çalma listesi dosyaları ve önbellekte çalışır.
- Doğru şarkı otomatik seçilir. Sıra: ISRC kodu → YouTube Music'te "sanatçı - şarkı" araması → YouTube araması. Puanlamada başlık, sanatçı ve süre uyumu kullanılır; canlı, cover, remix ve sped up sürümler cezalandırılır. Kullanıcıya seçim menüsü çıkmaz.
- Listeler anında kuyruğa eklenir; her şarkı, çalma sırası geldiğinde eşleştirilir. İlk şarkılar arka planda önceden eşleştirilir. Eşleşmeler önbelleğe alınır.
- YouTube akışı açılamazsa yt-dlp ile indirilip çalınır.
- API anahtarı isteğe bağlıdır. Anahtar yoksa Spotify'ın herkese açık embed sayfası kullanılır.
- Spotify'ın Şubat 2026 kısıtlamalarına uyum: başkasına ait listeler embed sayfasından okunur. Yeni `items` alan adı desteklenir ve kapatılan `audio-features` uç noktası artık kullanılmaz.
- Erişim anahtarı süresi dolunca kendiliğinden yenilenir. 401, 403, 404 ve 429 (istek sınırı / kota) durumları Türkçe mesajla bildirilir.
- `nowplaying` kartında Spotify albüm kapağı gösterilir.
- Podcast linkleri anlaşılır bir mesajla reddedilir.

## Hata düzeltmeleri
- `!spotify` her kullanımda çöküyordu (kapatılmış `audio-features` uç noktası).
- `/spotify`, Spotify yavaş yanıt verdiğinde "uygulama yanıt vermedi" hatasına düşüyordu.
- Kapak resmi olmayan albümde Spotify komutu çöküyordu.
- `/help` komutu hiç yanıt vermiyordu.
- Ses kanalı sohbetinde ya da thread'de müzik komutu kullanınca `NullPointerException` oluşuyordu (`getTextChannel()`).
- Komut kanalı kısıtlaması ses kanalı sohbetinde botu çökertiyordu.
- `config.txt` her açılışta yeniden "güncelleniyor" ve her seferinde yeni yedek oluşturuyordu (sürüm numarası eski dosyadan kopyalanıyordu).
- Discord'a bağlanılamadığında kapanış sırasında `NullPointerException` oluşuyordu.
- `/lyrics`: seçenek verilmezse çöküyordu, uzun sözlerde ikinci mesaj gönderilemiyordu ve zaman aşımı oluyordu.
- `/skip` oylaması: sunucunun geçme oranı ayarı yok sayılıyordu (sabit %55), mesaj iki kez yazılıyordu ve bazen boş mesaj gönderilmeye çalışılıyordu.
- `/playnext`: sonuç bulunamayınca kullanıcının adı arama terimi olarak kullanılıyordu.
- `/play`: varsayılan çalma listesi bulunamayınca çöküyordu. Hata ayrıntısı, sahip yerine botun kendisine göre gösteriliyordu.
- `!pause`: devam komutu `! play` olarak (boşluklu) gösteriliyordu.
- Hata ayrıntısı gönderilirken kısa hatalarda `StringIndexOutOfBoundsException` oluşuyordu.
- 22 karakterlik Spotify kimlikleri yanlışlıkla YouTube kimliği sanılıp yt-dlp'ye gönderiliyordu.
- `altprefix = "なし"` (eski varsayılan) gerçekten ikinci önek olarak kullanılıyordu.
- Bir komut açıklaması Discord'un 100 karakter sınırını aşarak tüm slash komutlarının kaydını bozabiliyordu; bu durum kontrol edildi.

## Diğer
- Hiçbir yerde kullanılmayan javacv (OpenCV/FFmpeg/OpenBLAS, tüm platformlar) bağımlılığı kaldırıldı. Jar boyutu yaklaşık 910 MB'tan 66 MB'a düştü.
- `pom.xml`: kapanmış Maven depoları (jcenter.bintray.com, m2.dv8tion.net) kaldırıldı ve sürüm `0.11.0-TR` yapıldı.
- Dockerfile, yerel jar ile çalışacak şekilde güncellendi.
