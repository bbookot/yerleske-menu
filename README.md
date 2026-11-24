# Yerleşke Menü

YTÜ öğrenci yemek listesi için bağımsız Android uygulaması ve boyutlandırılabilir ana ekran widget'ı. Üniversitenin resmi uygulaması değildir.

## Özellikler

- Uygulamadaki **Widget ayarları** ekranında canlı örnek önizleme ve varsayılana dönüş.
- Altı renk paleti; Kart / Açık / Koyu / Sistem görünümü; Klasik veya Altın fırça yıldızı ve belirginlik seçimi. Altın fırça sabit, küçük bir çizimdir; animasyon içermez. Mevcut kurulumlarda Klasik stili korunur.
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

## Uygulama ayarları

- Minimal liste görünümü; Açık / Koyu / Sistem modu, altı vurgu rengi, üç yazı boyutu ve Rahat / Sıkı liste aralığı.
- İlk açılış ve **Bugün** düğmesi, saat ayarı açık veya kapalı olsa da otomatik hizmet tarihine döner. 19.15 sonrası sonraki hizmet gününün iki öğünü birlikte açılır. Ayar kapalıyken otomatik görünüm 14.30'a kadar iki öğün, ardından yalnız akşamı gösterir; Öğle / Akşam düğmeleriyle elle seçim yapılabilir. **Otomatik** veya **Bugün** düğmesi elle seçimi bırakır.
- Tarih gezintisinde ilk öğün: Öğle, Akşam veya Son seçilen. Kalori, servis saatleri ve son kontrol bilgisi ayrı açılıp kapatılır.
- İsteğe bağlı **Saate göre tek öğün göster**, uygulama ve widget için ortaktır. Hizmet günlerinde 00.00–06.00 iki öğün, 06.00–14.30 yalnız öğle, 14.30–19.15 yalnız akşam, 19.15 sonrası sonraki hizmet gününün iki öğünü gösterilir. Hafta sonu ve tatiller atlanır; elle geleceğe bakıldığında iki öğün görünür. Saat dilimi Europe/Istanbul'dur. Ayar kapalıyken önceki widget davranışı ve elle uygulama öğün seçimi korunur.
- Uygulama ve widget görünümü bağımsızdır; menü türü ve saat kuralı ortaktır. Güncelleme mevcut tema, öğün ve widget tercihlerini silmez. Yeni saat ayarı varsayılan olarak kapalıdır.
- Uygulama açıkken saat sınırları yeniden değerlendirilir; widget 06.00, 14.30, 19.15 ve gece yarısı için arka plan işi planlar. Widget geçişleri Android pil yönetimine tabidir; kesin saniyede çalışma garantisi yoktur.

## Geliştirme

Java 17, Android SDK 35 ve Build Tools 35.0.0 gerekir. `local.properties` içinde SDK yolunu ayarlayın.

```text
gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Geliştirme paketi `app.yerleske.menu.dev`, dağıtım paketi `app.yerleske.menu` olur.
