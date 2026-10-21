# BZK Lisans Üretici (Android)

Tasarım ve Geliştirme: Yasin HAN

**GNSS BOZOKLUHAN GPS** uygulaması için cihaz koduna özel lisans anahtarı üretir. Yalnız sizin telefonunuzda kalmalıdır.

## APK'yı alma (GitHub)

1. github.com'da **yeni bir depo** açın, adı örneğin `BZK_Lisans_Uretici`. **Mutlaka Private** seçin.
2. **uploading an existing file** ile bu klasörün içindekilerin hepsini yükleyin, **Commit changes**.
3. **Actions** sekmesinde derleme 5–8 dakikada biter. **Artifacts › BZK_Lisans_Uretici_APK** indirin.
4. Actions'ta "Get started with GitHub Actions" yazıyorsa `.github` klasörü yüklenmemiştir: **set up a workflow yourself** ile GNSS uygulamasındaki derleme metnini yapıştırın; yalnız son iki satırdaki adları `BZK_Lisans_Uretici` yapın.

## Kullanım

1. Kullanıcı GNSS BOZOKLUHAN GPS'te **Lisans** penceresindeki **cihaz kodunu** size gönderir (ör. `7F3A-91C2`).
2. Üreticide kodu yazıp **Lisans Anahtarı Üret**'e basın.
3. **Anahtarı Gönder** ile WhatsApp/SMS'le iletin; kullanıcı anahtarı yazıp **Etkinleştir**'e basar.

Ürettiğiniz anahtarlar uygulamada liste olarak tutulur. Ekran görüntüsü alınamaz (anahtarların sızmaması için).

## Güvenlik

- Bu uygulama lisans üretim sırrını içerir. APK'yı kimseyle paylaşmayın, depoyu **Private** tutun.
- Paket adı: `com.yhan.bzklisans` · imza: `app/lisans.keystore` (silmeyin).
