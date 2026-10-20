# GNSS Koordinat Okuyucu - YHAN (Android)

Tasarım ve Geliştirme: Yasin HAN

Telefondan ITRF96 ve ED50 koordinatı okuyup iş adı altında kaydeden, NetCAD (.ncn), .txt ve Google Earth (.kml) dosyası üreten Android uygulaması.

## APK'yı almanın iki yolu

### Yol A — GitHub ile (bilgisayara bir şey kurmadan)

1. github.com'da ücretsiz hesap açın ve giriş yapın.
2. Sağ üstteki **+** › **New repository** seçin. Ad: `GNSS_Koordinat_YHAN`. **Private** seçebilirsiniz. **Create repository**'ye basın.
3. Açılan sayfada **uploading an existing file** bağlantısına tıklayın.
4. Bu klasörün **içindeki her şeyi** (app, gradle, .github, gradlew… hepsi) sürükleyip bırakın, **Commit changes**'e basın.
   - `.github` klasörü gizli görünebilir. Windows'ta Gezgin › Görünüm › **Gizli öğeler** işaretleyin.
5. Üstteki **Actions** sekmesine gidin. "APK Derle" çalışmaya başlar (yaklaşık 5–8 dakika).
   - Çalışmazsa **APK Derle** › **Run workflow** düğmesine basın.
6. Yeşil tik çıkınca çalışmanın içine girin. En altta **Artifacts** bölümünde **GNSS_Koordinat_YHAN_APK** dosyasını indirin.
7. İndirilen zip'in içindeki `GNSS_Koordinat_YHAN.apk` dosyasını telefona atın.

### Yol B — Android Studio ile

1. developer.android.com/studio adresinden Android Studio'yu kurun.
2. **Open** ile bu klasörü açın. İlk açılışta gerekli dosyaları kendisi indirir (birkaç dakika).
3. Menüden **Build › Build App Bundle(s) / APK(s) › Build APK(s)**.
4. Bitince çıkan **locate** bağlantısıyla APK'yı bulun: `app/build/outputs/apk/debug/app-debug.apk`

## Telefona kurulum

1. APK dosyasını telefona gönderin (WhatsApp, Drive, USB kablo).
2. Dosyaya dokunun. "Bilinmeyen uygulamalar" uyarısı çıkarsa **Ayarlar › Bu kaynağa izin ver** deyin.
3. **Yükle**'ye basın. Play Koruma uyarısı çıkarsa **Yine de yükle** seçin (uygulama Play Store'dan gelmediği için çıkar).

## Sürüm 2.1 — yeni ölçüm ekranı

- **Ölçüm:** "Anlık Koordinat" başlığı altında Y ve X yan yana ortalı; Enlem (E) ve Boylam (B) ondalık veya derece-dakika-saniye; h (elipsoit) ve H (ortometrik).
- **Sistem seçimi ölçüm ekranında:** ITRF96 TM3 / ED50 TM3 / ED50 UTM6.
- **Ortalama okuma sayısı** ölçüm ekranından − / + ile değiştirilir.
- **Otomatik kayıt:** kutu işaretliyken belirlenen aralıkla (sn) sıradaki numarayla kendiliğinden kaydeder; işaret kaldırılınca durur. RTK'da "yalnız Fix" kuralına uyar.
- **Liste sekmesi:** kaydedilen koordinatlar okunuş sırasıyla, eksiksiz (Y, X, E, B, h, H, hata, saat).
- **Harita:** canlı mavi nokta, hata dairesi, yürünen iz, anlık Y/X paneli ve **Koordinatı Kaydet** düğmesi.

## Sürüm 2.0 — RTK alıcı desteği

**Alıcı** sekmesinden üç konum kaynağı seçilir:

| Kaynak | Hassasiyet | Ne zaman |
| --- | --- | --- |
| Telefon | 1–5 m | Keşif, yer bulma |
| RTK Alıcı (Bluetooth) | Yatay 1–2 cm, düşey 2–3 cm (RTK Fix) | Kadastro hassasiyeti gereken ölçü |
| Deneme | — | Alıcı yanınızda yokken arayüzü öğrenmek |

Sahada kullanım:

1. Alıcıyı açın, telefonun Bluetooth ayarlarından bir kez eşleştirin (PIN genelde 1234 veya 0000).
2. Alıcının kendi ayarında Bluetooth NMEA çıkışını açın: **GGA ve GST, 1 Hz**.
3. Alıcı sekmesinde **RTK Alıcı** › **Cihazları Göster** › alıcınıza **Bağlan**.
4. Düzeltme verisi: alıcı kendi SIM kartıyla alıyorsa atlayın. Yoksa NTRIP bilgilerini girin (TUSAGA-Aktif: kurumunuzun hesabı), **Noktaları Getir** ile en yakın VRS noktasını seçip **Bağlan**.
5. Çözüm ışığı yeşil **RTK FIX** olunca Ölçüm sekmesinde **Kaydet** veya **Ortalama**.

Notlar:

- "Yalnız RTK Fix'te kaydet" açıkken Float veya Tekil çözümde Kaydet düğmesi kilitlidir.
- RTK koordinatları TUSAGA-Aktif'in sistemi olan ITRF96 (2005.0) ile gelir; uygulama bu noktalara epok düzeltmesi ve bilinen nokta düzeltmesi uygulamaz.
- Z değeri olarak elipsoit yüksekliği veya alıcının verdiği ortometrik yükseklik (Ayarlar › Çıktı) seçilebilir. Jalon yüksekliğini alıcının kendi ayarına girin.
- Bluetooth Low Energy (BLE) kullanan alıcılar bu sürümde desteklenmez; çoğu ölçme alıcısı klasik Bluetooth (SPP) kullanır.

## Güncelleme (sürüm 1'den 2'ye)

Sürüm 2'den itibaren APK sabit bir anahtarla (`app/yhan.keystore`) imzalanır; sonraki güncellemeler eskisinin üzerine kurulur ve kayıtlar korunur. Sürüm 1 farklı anahtarla imzalandığı için **bir kerelik** eski uygulamayı kaldırmak gerekir. Kaldırmadan önce İşler sekmesinden noktalarınızı NCN/TXT olarak dışa aktarın.

## Uygulamanın tarayıcı sürümünden farkları

- **Konumu tek dokunuşla açma:** Konum kapalıysa Android'in "Konum açılsın mı?" penceresi çıkar, **Tamam** yeterli.
- **Ayarlar düğmeleri gerçekten açılır:** "Konum Ayarlarını Aç" ve "Uygulama İzinlerini Aç" doğrudan ilgili ayar sayfasına gider.
- **Ham GPS okuması:** Konum doğrudan GPS alıcısından okunur. GPS yokken (kapalı alan) ağ konumu kullanılır ve "Ağ konumu" yazar.
- **Uydu bilgisi:** Kullanılan/görünen uydu sayısı, GPS, GLONASS, Galileo ve BeiDou dağılımı, sinyal gücü ve çift frekans (L5) göstergesi.
- **Dosyalar telefona kaydedilir:** NCN, TXT ve KML dosyaları **İndirilenler/YHAN_GNSS** klasörüne yazılır.
- **İnternetsiz açılır:** Yalnızca uydu haritası için internet gerekir.
- **Harici RTK alıcısı:** Bluetooth RTK alıcınızın uygulaması Android'de "Sahte konum uygulaması" olarak seçilirse, bu uygulama RTK konumunu okur ve "Harici alıcı" yazar.

## Teknik bilgiler

- Paket adı: `com.yhan.gnsskoordinat` · Sürüm 2.1 · En düşük Android 7.0 (API 24) · Hedef API 34
- Arayüz: `app/src/main/assets/index.html`. Arayüzde değişiklik yapmak için bu dosyayı düzenleyip APK'yı yeniden derlemeniz yeterli.
- Yerel kod: `app/src/main/java/com/yhan/gnsskoordinat/MainActivity.kt`
- APK, `app/yhan.keystore` anahtarıyla imzalanır (kişisel kullanım). Bu dosyayı silmeyin; silinirse güncellemeler eskisinin üzerine kurulamaz. Play Store için ayrı ve gizli bir anahtar gerekir.
