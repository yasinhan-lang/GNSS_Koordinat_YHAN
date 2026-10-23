# GNSS BOZOKLUHAN GPS (Android)

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

## Sürüm 4.5

- Silme işlemlerinde (iş, nokta, hedefler, kayıtlı parsel, DNS) Sil / Vazgeç onay penceresi. İş silinemiyordu (liste her konumda yenilendiği için) – düzeltildi.
- Bayrak da ana sayfaya götürür; ana sayfa İlk Kurulum hizası, Kurulum Tamam ve ana düğmeler daraltıldı.
- Parsel Sorgu uygulama içinde açılır (yeni ParselActivity): açılan ve Favorilerim'den gösterilen parseller "Haritaya Aktar" ile haritaya ve kayıtlı parsellere gelir; "Haritaya Dön" / geri tuşu ile dönülür.
- Kayıtlı parseller: tek tek kutu ile, toplu Tümünü Aç / Kapat, Hepsini Göster; çizgiler düz (kesik değil).
- Google Earth'e veya başka uygulamaya geçince "Haritaya geri dönmek için dokunun" bildirimi (Android 13+: bildirim izni).
- Aplikasyon: hedef listesi konum güncellemelerinde yeniden çizilmiyor (yanıp sönme giderildi).
- Ayarlar › Çıktı: kısa etiketler (Elipsoit (h) / Ortometrik (H) / Sıfır (0), 2-3-4 hane); WebView yazı ölçeği sabit (telefonun büyük yazı ayarı kutuları taşırmıyor).
- Gereksiz pano okuma özelliği kaldırıldı.

## Sürüm 4.4

- Harita: Kaydet / Nokta No iki satır, Nokta No çerçevesiz.
- Parsel: "Favori / İndirilen Parseller · Dosyadan Getir (KML · GeoJSON)" – birden çok dosya seçilebilir, tümü haritada birlikte çizilir (TKGM sorgu hakkı harcamaz).
- Android: Parsel Sorgu'dan KML / GeoJSON dosyası "Paylaş" ile de alınır (SEND / SEND_MULTIPLE); dosya seçicide çoklu seçim.

## Sürüm 4.3

- Kısıtlı modda (deneme bitince) renk temaları kilitli; yalnız Altın tema kullanılır. Seçilen tema saklanır, lisans girilince geri gelir. Açık/Koyu görünüm serbest.

## Sürüm 4.2

- Ayarlar › Görünüm · Renk Teması: Altın, Turkuaz, Zümrüt, Yakut, Lavanta, Gece Mavisi, Turuncu, Gümüş.
- Otomatik / Açık / Koyu görünüm seçimi.
- Başlık, ana sayfa, düğmeler, pusula ve logo seçilen temaya uyar.

## Sürüm 4.1

- Parsel penceresinde Ada ve Parsel kutuları her açılışta boş gelir (il/ilçe/mahalle hatırlanır).
- Harita: Kaydet · Nokta No düğmesi her durumda yeşil zeminli, siyah çerçeveli.

## Sürüm 4.0

- Harita: Kaydet düğmesi iki satır (KAYDET / çizgi / çerçeveli Nokta No).
- Parsel etiketi 105/12: ada ve parsel aynı renk, "/" beyaz, boşluksuz.
- Parsel penceresi: "Bulunduğum Yerdeki Parsel" ve yeni "Açık Olan Parselleri Getir" (iki satır).
- Açık olan parseller: TKGM Parsel Sorgu'dan Paylaş ile gelen metin veya panodaki bağlantı/bilgilerdeki tüm parseller haritaya çizilir; yoksa "Açık parsel yok".
- Android: uygulama "Paylaş" menüsünde metin alıcı olarak görünür (singleTask).

## Sürüm 3.9

- Yeni logo: dünya haritası üzerinde içinde "yh" yazan konum pini (uygulama simgesi, başlık, ana sayfa).
- Her yerde Enlem solda, Boylam sağda (WGS84 işlerinde listeler, harita, aplikasyon ve dosyalar dahil). Aplikasyon: Kuzey (ΔX) | Doğu (ΔY).
- Harita: alt panelde koordinat ve hata ince siyah çizgili tabloda; kaydet satırı inceldi. + / − düğmeleri ve yeni pusula G. Earth sütununa hizalı.
- Parsel etiketi çerçevesiz, kalın ve konturlu (her zeminde okunur).
- PS GİT: telefonda TKGM Parsel Sorgu uygulaması (com.tkgm.parselsorgu) varsa onu, yoksa web sitesini açar.
- G. Earth: açık ve kayıtlı parseller de Google Earth'te gösterilir.
- Aplikasyon: hedefe 5 m'den yakınken bip (yaklaştıkça sıklaşır, 1 m altında titreşim); zemin rengi değişti.

## Sürüm 3.8

- Ölçüm: iş adı / DOM / sistem satırı artık hiç kapanmıyor (dar ekranda satırların sıkışma hatası giderildi); uzun iş adı iki satıra iner.
- Anlık koordinat: Boylam | Enlem (Y | X altında). Aplikasyon: Doğu (ΔY) | Kuzey (ΔX).
- Harita: kaydet düğmesi "Kaydet · Nokta No: …" sola yaslı. Pusula eklendi (yön sensörüyle gerçek kuzeyi gösterir).
- Parsel: altın sarısı sınır, lacivert etiket; parsel kaydetme, kayıtlı parseller listesi (Göster / Yol / Sil), Google Haritalar ile yol tarifi; il-ilçe-mahalle kutuları tam görünür.
- Aplikasyon: Hedef Nokta No kutusu küçük, TXT/NCN DOSYASI AÇ büyük; elle eklemede "Nokta No" etiketleri ortalı.
- Alt menü: Ölçüm, Harita, Aplikasyon, İşler, Liste, Alıcı, Ayarlar.
- Ayarlar: NetCAD .dns dönüşüm dosyası (Helmert / Afin, $AMAT ve $NOK) ED50 (TM3) işlerinde kullanılabilir.
- Sürüm numarası ana sayfada ve lisans penceresinde görünür.

## Sürüm 3.7

- Üst başlıktan "Yasin HAN · Tasarım ve Geliştirme" yazısı kaldırıldı; YH logosu, uygulama adı ve bayrak kaldı.

## Sürüm 3.6

- Ayarlar › Çıktı: Z değeri seçimi geniş sütunda, seçenekler kısaltıldı (Elipsoit yük. (h) / Ortometrik (H) / 0 yaz); değerler tam görünür.
- Bilinen nokta kutularının örnek yazıları dar ekranda taşmıyor.

## Sürüm 3.5

- Harita › Parsel: yeni "Parseli Haritaya Getir" penceresi.
  - Bulunduğum Yerdeki Parsel: konumdaki parseli çizer.
  - Ada / Parsel ile: İl › İlçe › Mahalle/Köy listelerinden seçip ada ve parsel numarasıyla parseli TKGM'den getirip haritada gösterir; seçimler hatırlanır, listeler telefonda saklanır.
- Haritada uzun basınca o yerin parseli gelir.
- Android: parselsorgu.tkgm.gov.tr (il listesi) adresine de izin verildi.

## Sürüm 3.4

- Aplikasyon: Hedef N.NO kutusu ve TXT/NCN Aç düğmesi alttaki Durdur / Kontrol Kaydet düğmeleriyle aynı boyda, aynı hizada ve yuvarlak.

## Sürüm 3.3

- Ölçüm: sistem düğmeleri, otomatik kayıt satırı ve kararlılık/uydu satırı daha ince.
- Anlık koordinat Y/X değerleri büyük ve mavi zeminli; diğer satırlardan ayrışır.
- Aplikasyon: Hedef N.NO solda, küçük "TXT/NCN Aç" düğmesi sağda; altında seçilen hedefin N.NO, Y, X (ve varsa Z) koordinatı tam olarak görünür.
- Aplikasyon: Durdur, Kontrol Kaydet ve Kayıtlılardan düğmeleri ince, yuvarlak, çerçeveli.
- NCN/TXT hedef dosyasındaki 4. sütun (Z) okunur.

## Sürüm 3.2

- **Başlık bandı:** koyu lacivert koordinat ağı deseni, altın alt çizgi, "BOZOKLUHAN" altın renkte, ad ve logo ince çerçevede; ana sayfa üst kartı aynı tasarımda.
- **Harita › İz göster** tiki: yürünen iz gizlenir/gösterilir; kayıtlı noktalar ve mavi konum işareti her zaman görünür.
- **Lisans:** ilk açılıştan itibaren **21 gün tam sürüm**. Süre dolunca kısıtlı mod: dışa aktarma, RTK alıcı/NTRIP, aplikasyon, ortalama, otomatik kayıt ve parsel sorgu kapanır; iş başına en fazla 5 nokta kaydedilir. Anlık koordinat, harita ve simülasyon açık kalır.
- Her telefonun **cihaz kodu** Lisans penceresinde görünür (Ana sayfa rozeti, Ayarlar › Lisans). Cihaza özel **lisans anahtarı** girilince tam sürüm açılır. Anahtarlar ayrı verilen **Lisans Üretici** dosyasıyla üretilir (bu dosya uygulama paketinde yoktur, gizli tutulmalıdır).
- Telefon saati bir günden fazla geri alınırsa deneme süresi biter. Uygulama kaldırılıp yeniden kurulursa deneme süresi yeniden başlar (çevrimdışı lisansın sınırı).

## Sürüm 3.1

- Başlık paneli ve anlık koordinat satırı daha ince; alt menü koyu renk.
- **Aplikasyon:** "TXT / NCN Dosyası Aç" ile telefondaki dosyadan hedef noktalar yüklenir (NetCAD NCN: N.NO Y X Z; TXT: boşluk, sekme veya ; ayrımlı; Türkçe karakterli eski dosyalar da okunur). Hedef, **N.NO yazarak** veya listeden seçilir.
- **Harita › PARSEL:** konumdaki (veya uzun basılan yerdeki) parselin sınırı TKGM'nin açık parsel servisinden alınıp haritaya çizilir; ada/parsel, il/ilçe/mahalle, alan, nitelik ve parsel içindeki ölçülen nokta sayısı gösterilir. TKGM günlük sorgu sınırı uygular.

## Sürüm 3.0 — GNSS BOZOKLUHAN GPS

- **Ana sayfa:** uygulama adı, kısa açıklama, ilk kurulum (tam konum izni, konumun açılması, Bluetooth izni) ve BAŞLAT, ÖLÇÜM, APLİKASYON, HARİTA, TKGM PARSEL SORGUYA GİT düğmeleri. Sol üstteki YH logosu ana sayfaya döner; geri tuşu ölçüm ekranından ana sayfaya gelir.
- **ÖLÇÜM:** önce DOM, sonra sistem — ITRF96 (TM3), ED50 (TM3), ITRF96 (UTM6), ED50 (UTM6), WGS84 Coğrafi — sonra İŞ ADI sorulur; noktalar bu iş adı altında kaydedilir.
- **Ölçüm ekranı:** DOM ve sistem doğrudan seçilir; okunacak nokta numarası (N.NO) ve kod en üstte, kayıt doğrudan bu numarayla yapılır ve numara kendiliğinden artar. Otomatik kayıt satırı düğmelerin üstünde. DURDUR'a basınca kaç nokta kaydedildiği yazılır. Tüm satırlar daha ince.
- **APLİKASYON:** hedef noktalar elle, NCN/TXT satırı yapıştırarak veya kayıtlı noktalardan eklenir; hedefe mesafe, Kuzey/Doğu farkı, semt açısı (grad) ve kuzey yukarı yön çizimi gösterilir; "Kontrol Kaydet" aplike edilen noktayı N.NO+"A" olarak kaydeder.
- **Harita:** küçük düğmeler (Konuma Git, Uydu/Sokak, PS GİT, Google Earth); daha küçük mavi konum noktası, 22. seviyeye kadar yakınlaştırma; kayıtlı noktaların numaraları kutusuz yazılır.
- **PS GİT / TKGM Parsel Sorgu:** Parsel Sorgu sitesini açar ve konumun enlem/boylamını panoya kopyalar ("Koordinat ile sorgu" alanına yapıştırılır).

## Sürüm 2.4

- Ölçüm ekranı daha ince: koordinat tablosu, düğmeler ve kararlılık satırı daraltıldı; kayıt penceresi sadeleştirildi.
- Liste: başlıkta iş adı ve "Dışa Aktar"; sütunlar N.NO, Y, X ve isteğe bağlı **Z, Hata, Saat** (tik). Tiklenmeyen sütunlar ekranda gösterilmez ve NCN/TXT/KML'e aktarılmaz.
- KML · Earth: dosyayı İndirilenler'e kaydeder ve Google Earth uygulamasında açar.
- Harita: "Google Earth'te Göster" kayıtlı noktaları Google Earth'te açar; alt bilgi satırı kaldırıldı; otomatik kayıt tiki eklendi.
- Alıcı: "Deneme" adı "Simülasyon" oldu ve açıklaması eklendi; bu modda kaydedilen noktalara SIM kodu yazılır.
- İşler: noktalar N.NO / Y / X / h tablosunda, etiketsiz.
- Ayarlar: TKGM ölçü standartları kartı ve "TKGM Varsayılanlarını Yükle" düğmesi; simetrik düzen.

## Sürüm 2.3

- İlk açılışta iş adı sorulmaz; iş "Ölçüm GG.AA.YYYY" adıyla oluşur. Yeni iş açarken ad sorulur.
- Ortalama okuma sayısı sabit 10.
- Anlık koordinat çizgili tabloda: Y/X, enlem/boylam, h/H. Durum bilgileri ortalı.
- Kayıtlı noktalar ve dışa aktarma (NCN, TXT, KML, paylaş, kopyala) Liste sekmesinde; liste kaydediliş sırasıyla, ekrana sığan küçük puntolu tablo.

## Sürüm 2.1 — yeni ölçüm ekranı

- **Ölçüm:** "Anlık Koordinat" başlığı altında Y ve X yan yana ortalı; Enlem (E) ve Boylam (B) ondalık veya derece-dakika-saniye; h (elipsoit) ve H (ortometrik).
- **Sistem seçimi ölçüm ekranında:** ITRF96 TM3 / ED50 TM3 / ED50 UTM6.
- **Otomatik kayıt:** kutu işaretliyken belirlenen aralıkla (sn) sıradaki numarayla kendiliğinden kaydeder; işaret kaldırılınca durur. RTK'da "yalnız Fix" kuralına uyar.
- **Liste sekmesi:** kaydedilen koordinatlar okunuş sırasıyla, eksiksiz (Y, X, h, H, hata, saat). Enlem/boylam yalnız ölçüm ekranında gösterilir.
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

- Paket adı: `com.yhan.gnsskoordinat` · Sürüm 3.2 · En düşük Android 7.0 (API 24) · Hedef API 34
- Arayüz: `app/src/main/assets/index.html`. Arayüzde değişiklik yapmak için bu dosyayı düzenleyip APK'yı yeniden derlemeniz yeterli.
- Yerel kod: `app/src/main/java/com/yhan/gnsskoordinat/MainActivity.kt`
- APK, `app/yhan.keystore` anahtarıyla imzalanır (kişisel kullanım). Bu dosyayı silmeyin; silinirse güncellemeler eskisinin üzerine kurulamaz. Play Store için ayrı ve gizli bir anahtar gerekir.
