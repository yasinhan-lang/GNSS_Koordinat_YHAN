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

## Uygulamanın tarayıcı sürümünden farkları

- **Konumu tek dokunuşla açma:** Konum kapalıysa Android'in "Konum açılsın mı?" penceresi çıkar, **Tamam** yeterli.
- **Ayarlar düğmeleri gerçekten açılır:** "Konum Ayarlarını Aç" ve "Uygulama İzinlerini Aç" doğrudan ilgili ayar sayfasına gider.
- **Ham GPS okuması:** Konum doğrudan GPS alıcısından okunur. GPS yokken (kapalı alan) ağ konumu kullanılır ve "Ağ konumu" yazar.
- **Uydu bilgisi:** Kullanılan/görünen uydu sayısı, GPS, GLONASS, Galileo ve BeiDou dağılımı, sinyal gücü ve çift frekans (L5) göstergesi.
- **Dosyalar telefona kaydedilir:** NCN, TXT ve KML dosyaları **İndirilenler/YHAN_GNSS** klasörüne yazılır.
- **İnternetsiz açılır:** Yalnızca uydu haritası için internet gerekir.
- **Harici RTK alıcısı:** Bluetooth RTK alıcınızın uygulaması Android'de "Sahte konum uygulaması" olarak seçilirse, bu uygulama RTK konumunu okur ve "Harici alıcı" yazar.

## Teknik bilgiler

- Paket adı: `com.yhan.gnsskoordinat` · En düşük Android 7.0 (API 24) · Hedef API 34
- Arayüz: `app/src/main/assets/index.html`. Arayüzde değişiklik yapmak için bu dosyayı düzenleyip APK'yı yeniden derlemeniz yeterli.
- Yerel kod: `app/src/main/java/com/yhan/gnsskoordinat/MainActivity.kt`
- APK, kişisel kullanım için hata ayıklama anahtarıyla imzalanır. Play Store'a yüklemek isterseniz kendi imza anahtarınızı oluşturmanız gerekir.
