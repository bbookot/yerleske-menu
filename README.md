# Yerleşke Menü

YTÜ öğrenci yemek listesi için bağımsız Android uygulaması ve boyutlandırılabilir ana ekran widget'ı. Üniversitenin resmi uygulaması değildir.

## Özellikler

- Resmi SKS kaynağından aylık menü, öğle/akşam ve normal/vejetaryen seçimi.
- Tarih gezintisi, açık/koyu/sistem teması, çevrimdışı önbellek ve son kontrol zamanı.
- Widget bugünün menüsünü uygulamadaki öğün ve menü türü tercihiyle gösterir. Küçük boyutta özet, büyük boyutta liste sunar.
- Menü verisi uygulama sürümünden bağımsız yenilenir. Periyodik işler Android pil yönetimine tabidir.
- GitHub Releases üzerinden kullanıcı onaylı APK güncellemesi; paket, sürüm ve imza doğrulaması.
- Hesap, reklam ve analiz SDK'sı yoktur. Menü istekleri YTÜ'ye, güncelleme istekleri GitHub'a gider; bu hizmetler bağlantı bilgilerini görebilir.

## Geliştirme

Java 17, Android SDK 35 ve Build Tools 35.0.0 gerekir. `local.properties` içinde SDK yolunu ayarlayın.

```text
gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Geliştirme paketi `app.yerleske.menu.dev`, dağıtım paketi `app.yerleske.menu` olur. Eski projelerden bağımsızdır.

## İmzalama

İmza anahtarı ve şifreler Git'e eklenmez. Yerel `keystore.properties` dosyasının alanları: `storeFile`, `storePassword`, `keyAlias`, `keyPassword`. Kalıcı anahtarı güvenli ve ayrı bir yerde yedekleyin. Anahtar kaybı normal APK güncelleme devamlılığını keser.

## Sürüm protokolü

Etiket: `v0.1.0+1`. Artıdan sonraki sayı Android versionCode'dur ve her sürümde artmalıdır. APK dosyasının adı `yerleske-menu.apk` olmalıdır. Kararlı sürümler Releases üzerinden denetlenir; taslak ve ön sürümler kullanılmaz.

Yayımlama akışı varsayılan olarak taslak Release oluşturur. Test ve onay sonrasında taslağı yayımlayın. GitHub Secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_STORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. Base64 gizleme/şifreleme değildir; yalnızca GitHub Secrets içinde saklayın.

İlk kurulum ve güncellemeler Android'in sistem onayına tabidir. Geliştirme sürümü dağıtım APK'sına dönüşmez; güncelleme testi iki dağıtım APK'sıyla yapılmalıdır.
