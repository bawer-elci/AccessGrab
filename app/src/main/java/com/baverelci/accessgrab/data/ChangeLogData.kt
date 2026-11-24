package com.baverelci.accessgrab.data

data class ReleaseLog(
    val version: String,
    val date: String,
    val highlights: List<String>,
    val isLatest: Boolean = false
)

object ChangeLogData {
    val releases = listOf(
        ReleaseLog(
            version = "v1.6.0",
            date = "24 Kasım 2026",
            isLatest = true,
            highlights = listOf(
                "Yeni Doğrudan İndirme Motoru (Redmi / MIUI & HyperOS Tam Uyumu): Sistem DownloadManager kısıtlamalarına ve 20 sn zaman aşımına (HTTP 499) takılmayan, uygulama içi yüksek hızlı doğrudan akış (OkHttp Streaming) motoruna geçildi.",
                "Android 7.0 – Android 16 (API 24 – 35+) Genişletilmiş Cihaz Desteği: Eski ve yeni tüm Android sürümlerinde çift katmanlı depolama (Uygulama Dizini + Genel İndirilenler / MediaStore) ve otomatik Galeri/Müzik tarayıcı entegrasyonu.",
                "Aşama Çubuğu Takılma Düzeltmesi: Sunucu hazırlık aşaması ('Sunucuda Hazırlanıyor…') ve anlık indirme yüzdesi ayrıştırıldı; bağlantı hatalarında sonsuz beklemek yerine anında 'Yeniden Dene' kartına geçiş sağlandı."
            )
        ),
        ReleaseLog(
            version = "v1.5.4",
            date = "24 Kasım 2026",
            isLatest = false,
            highlights = listOf(
                "İndir Sekmesi TalkBack Eylemler (Custom Actions) Entegrasyonu: Medya bilgisi kartı üzerinde Eylemler menüsüyle doğrudan 1080p/720p/480p/360p MP4 Video veya 320/192/128 kbps MP3 Ses olarak anında indirme başlatma.",
                "Hızlı Bağlantı Kutusu Eylemleri: URL giriş kutusu üzerinde Eylemler menüsüyle tek adımda 'Panodan Yapıştır ve Hemen İncele', 'Panodan Yapıştır' ve 'Metni Temizle' desteği.",
                "Pano, İndirme Tamamlandı ve Hata Kartı Eylemleri: Görsel ikonlar ve butonlar ekranda korunurken TalkBack Eylemler menüsüyle tek dokunuşla işlem yapabilme."
            )
        ),
        ReleaseLog(
            version = "v1.5.3",
            date = "24 Kasım 2026",
            isLatest = false,
            highlights = listOf(
                "TalkBack Eylemler (Custom Actions) Menüsü: Kütüphane sekmesinde her medya dosyası tek bir öğe haline getirildi. Dosya üzerinde TalkBack Eylemler menüsüyle doğrudan Oynat, Paylaş ve Sil işlemlerine hızlı erişim sağlandı.",
                "4 Kat Daha Hızlı Liste Gezinmesi: Görsel butonlar ekranda korunurken TalkBack taramasından gizlendi; sağa/sola kaydırmada butonlara takılmadan doğrudan dosyalar arasında geçiş yapılabilir.",
                "Temiz Dosya İsimlendirmesi ve Tekil Seslendirme: '.trashed' ön ek temizliği ve çift okuma düzeltmeleri."
            )
        ),
        ReleaseLog(
            version = "v1.5.2",
            date = "24 Kasım 2026",
            isLatest = false,
            highlights = listOf(
                "TalkBack Çift Okuma Düzeltmesi: Butonlarda hem erişilebilirlik açıklamasının hem de buton metninin ('sil, Sil' gibi) üst üste iki kez okunması hatası giderildi.",
                "Temiz Dosya İsimlendirmesi: Android sisteminin dosya adlarının başına eklediği '.trashed-numara-' ön ekleri otomatik temizlenerek yalnızca gerçek medya başlığının okunması sağlandı.",
                "Kütüphane Ekranı Yerleşim ve Otomatik Senkronizasyon: İndirilen dosyaların anında ve tam ekran listelenmesi optimize edildi."
            )
        ),
        ReleaseLog(
            version = "v1.5.1",
            date = "24 Kasım 2026",
            isLatest = false,
            highlights = listOf(
                "Kütüphane Ekranı Yerleşim ve Görünürlük Düzeltmesi: İndirilen dosyaların listede görünmesini engelleyen başlık/buton genişlik çakışması giderildi.",
                "Tam Otomatik Kütüphane Senkronizasyonu: Gereksiz manuel yenileme butonu kaldırılarak indirilen tüm medya dosyalarının anında otomatik listelenmesi sağlandı.",
                "Geliştirilmiş TalkBack Kart Gezinmesi: Her medya kartı için doğrudan çift dokunuşla oynatma ve bağımsız Oynat, Paylaş, Sil düğmeleri optimize edildi."
            )
        ),
        ReleaseLog(
            version = "v1.5.0",
            date = "24 Kasım 2026",
            isLatest = false,
            highlights = listOf(
                "Kütüphanem (İndirilen Dosyalar) Sekmesi: İndirilen tüm MP4 video ve MP3 ses dosyalarını uygulama içinden doğrudan listeleme, açma/oynatma, paylaşma ve silme.",
                "Akıllı Yeniden Deneme (Smart Retry): Bağlantı kopması veya indirme hatalarında tek dokunuşla aynı kalite ve ayarlarla yeniden indirme.",
                "Modüler ve Performanslı Mimari: Ekran bileşenleri modüler parçalara ayrılarak hem görsel akıcılık hem de TalkBack tepki hızı artırıldı.",
                "Erişilebilir Dosya Yönetimi: Dosyaların boyutları, indirilme tarihleri ve formatları için detaylı ekran okuyucu seslendirmeleri."
            )
        ),
        ReleaseLog(
            version = "v1.4.1",
            date = "22 Kasım 2026",
            highlights = listOf(
                "Uygulama İçi Sürüm Notları (ChangeLog): Ayarlar ve Hakkında ekranlarından geçmiş tüm sürüm yeniliklerine erişebilme.",
                "Video Çözünürlük Seçenekleri: 1080p (FHD), 720p (HD), 480p (SD) ve 360p arasında serbest seçim.",
                "Ses Bit Hızı Seçenekleri: 320 kbps (Yüksek Kalite), 192 kbps (Standart) ve 128 kbps (Tasarruf) seçenekleri.",
                "Gelişmiş TalkBack Desteği: Seçilen format ve kalite için dinamik sesli geri bildirimler.",
                "İndirme motoru hız, kararlılık ve bellek optimizasyonları."
            )
        ),
        ReleaseLog(
            version = "v1.4.0",
            date = "22 Kasım 2026",
            highlights = listOf(
                "Video Çözünürlük Seçenekleri: 1080p (FHD), 720p (HD), 480p (SD) ve 360p arasında serbestçe seçim yapabilme.",
                "Ses Bit Hızı Seçenekleri: 320 kbps (Yüksek Kalite), 192 kbps (Standart) ve 128 kbps (Tasarruf) seçenekleri.",
                "Gelişmiş TalkBack Desteği: Seçilen format ve kalite için dinamik ve anlaşılır sesli geri bildirimler."
            )
        ),
        ReleaseLog(
            version = "v1.3.0",
            date = "20 Kasım 2026",
            highlights = listOf(
                "Canlı İlerleme Çubuğu: İndirme esnasında hem anlık yüzde (%45) hem de dosya boyutu (MB) canlı göstergesi.",
                "TalkBack Canlı Seslendirme: İlerleme durumunu periyodik seslendiren liveRegion erişilebilirlik altyapısı.",
                "Yeni 3D Tasarım Logo: Modern, yüksek çözünürlüklü uygulama simgesi ve adaptive icon entegrasyonu.",
                "Güçlendirilmiş Paket Yükleyici: Uygulama içi güncelleme deneyimi optimize edildi."
            )
        ),
        ReleaseLog(
            version = "v1.2.0",
            date = "19 Kasım 2026",
            highlights = listOf(
                "Material 3 Tasarım: Tamamen Jetpack Compose ile sıfırdan inşa edilmiş modern ve sade arayüz.",
                "Akıllı Pano Algılama: Kopyalanan medya bağlantısını panodan otomatik algılama ve tek tıkla yapıştırma.",
                "Titreşimli (Haptic) Geri Bildirim: İşlemlerde hissedilebilir titreşim bildirimleri."
            )
        ),
        ReleaseLog(
            version = "v1.0.0",
            date = "18 Kasım 2026",
            highlights = listOf(
                "İlk Açık Kaynak Sürüm: YouTube, Instagram, TikTok, X ve Facebook medya indirme altyapısı.",
                "%100 TalkBack ekran okuyucu uyumluluğu ve reklamsız indirme motoru."
            )
        )
    )
}
