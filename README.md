# Yerleşke Menü

YTÜ öğrenci yemek listesi için bağımsız Android uygulaması ve boyutlandırılabilir ana ekran widget'ı. Üniversitenin resmi uygulaması değildir.

## Özellikler

- Uygulamadaki **Widget ayarları** ekranında canlı örnek önizleme ve varsayılana dönüş.
- Altı renk paleti; Kart / Açık / Koyu / Sistem görünümü; yıldız deseni ve belirginlik seçimi.
- Üç yazı boyutu, normal/vejetaryen menü, servis saati / kalori / son yenileme görünürlüğü ve dört saydamlık seviyesi. Ayarlar cihazdaki bütün widget'lara ortak uygulanır.
- Yemekler kısa widget'ta sığmadığında listeler ayrı ayrı kaydırılabilir. Sistem modu, destekleyen Android sürümlerinde yeniden ağ isteği olmadan açık/koyu renge uyum sağlar.

- Öğrenci kartından esinlenen mavi widget, koyu mavi yarım yıldız ve beyaz metin. Varsayılan boyut 4×2; tek öğün görünümü tüm içerik genişliğini kullanır.

- Resmi SKS kaynağından aylık menü, öğle/akşam ve normal/vejetaryen seçimi.
- Tarih gezintisi, açık/koyu/sistem teması, çevrimdışı önbellek ve son kontrol zamanı.
- Widget 14:30'a kadar öğle ve akşamı birlikte, 14:30–19:15 arasında yalnız akşamı, sonrasında sonraki hizmet gününün iki öğününü gösterir. Saat dilimi Europe/Istanbul'dur.
- Her widget'ta ‹ / › ile otomatik tarihten 3 takvim günü ileri bakılabilir. Ortadaki etikete dokunmak otomatik moda döner. Gelecek günlerde iki öğün birlikte gösterilir; kapalı günlerde servis olmadığı belirtilir.
- Hafta sonu ve resmî tatillerde yemek hizmeti yoktur. Otomatik görünüm sonraki açık güne geçer. 28 Ekim ve bayram arefeleri de yemek servisinin tatil saatine denk gelmesi nedeniyle kapalı sayılır.
- Yedi günlük pencerenin kapsadığı aylar önceden alınır; API yalnızca aylık yanıt verdiği için aylık ham veriler önbellekte korunur. Ay/yıl sınırında iki ay birlikte alınır.
- Dinî tatil takvimi 2026–2027 için Diyanet'in resmî listelerinden doğrulanmıştır. Sonraki yıllar için güncelleme uyarısı gösterilir. Sabit millî tatiller her yıl uygulanır; ek idari izinler otomatik tahmin edilmez.
- Menü verisi uygulama sürümünden bağımsız yenilenir. Periyodik işler Android pil yönetimine tabidir.
- 14:30, 19:15 ve gece yarısı için ayrıca yeniden çizim işi planlanır; Android'in arka plan sınırlamaları kesin saniyede geçiş garantisi vermez. ↻ düğmesi anlık yeniden değerlendirme yapar.
- GitHub Releases üzerinden kullanıcı onaylı APK güncellemesi; paket, sürüm ve imza doğrulaması.
- Hesap, reklam ve analiz SDK'sı yoktur. Menü istekleri YTÜ'ye, güncelleme istekleri GitHub'a gider; bu hizmetler bağlantı bilgilerini görebilir.

## Geliştirme

Java 17, Android SDK 35 ve Build Tools 35.0.0 gerekir. `local.properties` içinde SDK yolunu ayarlayın.

```text
gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Geliştirme paketi `app.yerleske.menu.dev`, dağıtım paketi `app.yerleske.menu` olur.
