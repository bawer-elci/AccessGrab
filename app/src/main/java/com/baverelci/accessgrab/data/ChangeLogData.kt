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
            version = "v2.2.0",
            date = "26 Kasım 2026",
            isLatest = true,
            highlights = listOf(
                "Arama Sonuçlarında Doğrudan Görüntülü Video Oynatıcı Penceresi: Arama sonuçlarında bir videoya dokunduğunuzda 1 saniyede açılan görüntülü oynatıcı penceresi, ileri/geri sarma, duraklat/oynat, pencereden doğrudan MP4/MP3 indirme ve yarıda kalan videoya kaldığı saniyeden devam etme.",
                "YouTube Oynatma Listesi (Playlist) Toplu İndirme (#58): Oynatma listesi bağlantılarındaki tüm videoları listeleme ve 'Tümünü MP3 İndir' / 'Tümünü MP4 İndir' ile toplu indirme.",
                "Videonun Altyazısını / Konuşma Metnini Okuma Bilgi Kartı (#60): Videodaki konuşma metnini ve altyazıları tek dokunuşla bilgi kartında okuma ve panoya kopyalama.",
                "İzleme ve İndirme Geçmişim (#71) & Günün Trendleri / Popüler Müzikler (#73): İzlediğiniz veya indirdiğiniz videoları geçmişten tekrar açma ve günün trendlerini tek dokunuşla listeleme.",
                "WhatsApp Durumu İçin Videoyu 30 Saniyelik Parçalara Bölerek Paylaşma (#76) ve 8 saniyede anlık arka plan bildirim servisi."
            )
        ),
        ReleaseLog(
            version = "v2.1.0",
            date = "26 Kasım 2026",
            isLatest = false,
            highlights = listOf(
                "X (Twitter) Hızlı İndirme Motoru & Kısa Link (t.co) Desteği: X / Twitter videoları ve GIF'leri için doğrudan yüksek hızlı CDN motoru ve süre ayrıştırma düzeltmesi.",
                "3 Kat Daha Hızlı İndirme & 1 Saniyede Canlı Önizleme: Çoklu iş parçacıklı (8x paralel segment) indirme motoru, 128 KB yüksek hızlı tampon, çok çekirdekli MP3 dönüştürme ve beklemeden başlayan Canlı Önizleme akışı.",
                "Uygulama Kapalıyken Bile Özel Melodili Arka Plan Bildirimleri: Uygulama açık olmasa bile yeni duyuru veya güncelleme geldiğinde özel bestelenmiş kristal bildirim sesiyle durum çubuğuna bildirim düşmesi.",
                "Geliştirici Bildirim ve Mesaj Yönetim Paneli entegrasyonu."
            )
        ),
        ReleaseLog(
            version = "v2.0.0",
            date = "25 Kasım 2026",
            isLatest = false,
            highlights = listOf(
                "Kütüphane'de 'Tümünü Sil' & Zil/Alarm/Bildirim Sesi Yap: Kütüphanedeki tüm medyaları onay penceresiyle tek dokunuşla temizleme ve 'Diğer Seçenekler' menüsünden doğrudan Zil Sesi, Alarm Sesi ve Bildirim Sesi ayarlayıp kaldırma.",
                "%200 Ekstra Ses Güçlendirici & 30 Sn Zil Sesi Kırpıcı: Oynatıcı penceresinde sesi donanımsal olarak %200'e kadar artırma ve çalan şarkının tam o saniyesinden 30 saniyelik bölümü kesip doğrudan telefon zil sesi yapma.",
                "İndirmeden Önce Canlı Dinle (Önizle) & 20 Arama Sonucu: Arama sonuçlarında veya medya kartında indirmeden önce doğrudan önizleme dinleme ve 'Daha Fazla Sonuç Getir' ile 20 sonuca kadar listeleme.",
                "TalkBack İki Parmakla Çift Dokunma (MediaSession) & Telefon Araması Uyumu (AudioFocus): Kulaklık tuşu veya TalkBack iki parmak hareketiyle duraklat/oynat ve telefon araması geldiğinde otomatik duraklatıp görüşme bitince devam etme.",
                "Az Görenler İçin Ultra Yüksek Kontrast (Siyah-Sarı) & Dev Yazı Modu: Ayarlar'dan tek dokunuşla tam siyah zemin üzerine parlak sarı ve büyük puntolu görünüme geçiş.",
                "Kesintisiz İndirme Kilidi & Kaldığı Megabayttan Devam Etme: Ekran kapansa bile Wi-Fi'ın uyumasını önleme ve internet koparsa kaldığı bayttan otomatik devam etme (HTTP Range).",
                "Kullanım Rehberi Bilgi Kartı, Geliştiriciye Mesaj Gönder, Canlı Duyuru ve Melodili Güncelleme Bildirimi."
            )
        ),
        ReleaseLog(
            version = "v1.8.0",
            date = "25 Kasım 2026",
            isLatest = false,
            highlights = listOf(
                "Uygulamadan Çıkmadan Doğrudan Kelimeyle & Sesle Arama: Kutuya şarkı, sanatçı veya video adı yazarak ya da 'Sesle Ara (Mikrofon)' butonunu/eylemini kullanarak sonuçları anında listeleme ve tek dokunuşla MP3/MP4 indirme.",
                "Diğer Seçenekler Menüsü & Zil Sesi Yap / Kaldır: Kütüphane'de her dosyanın yanına 'Diğer Seçenekler' butonu ve TalkBack eylemleri eklendi; indirilen dosyayı doğrudan Telefon Zil Sesi Yapma ve tek dokunuşla eski zil sesine dönme.",
                "Kaldığım Yerden Devam Et & Ekran Kilitliyken Çalma: Uzun ders ve müzik kayıtlarında kalınan saniyeyi otomatik hatırlama, ekran kapalıyken kesintisiz çalma ve bildirim çubuğundan oynatıcı kontrolü.",
                "Telefonu Sallayarak Duraklat/Oynat & İndirme Onay Sesi: Oynatıcı açıkken telefonu hafifçe sallayarak duraklatıp başlatabilme ve indirme bittiğinde duyulabilir onay sesi."
            )
        ),
        ReleaseLog(
            version = "v1.7.1",
            date = "25 Kasım 2026",
            isLatest = false,
            highlights = listOf(
                "Görüntülü Video & Ses Oynatıcı Penceresi: Kütüphane'de bir dosyaya tıklandığında açılan tam boy pencerede hem gerçek video görüntüsü (SurfaceView) hem de büyük Duraklat/Oynat, 10 Sn İleri, 10 Sn Geri, Hızlandır (1x/1.25x/1.5x/2x), Önceki/Sonraki ve Kapat düğmeleri.",
                "Tek Dokunuşla Kesintisiz Güncelleme Kurulumu: Güncelleme indirildiğinde kurulum ekranının ilk seferde kapanmadan doğrudan açılması ve önbelleksiz APK indirme garantisi.",
                "Her Güncelleme Kontrolünde Özellik Açıklaması: 'Güncellemeleri Denetle'ye her basıldığında hem yeni sürümde hem de güncel sürümde özellik açıklamalarının ekranda gösterilmesi ve TalkBack ile seslendirilmesi."
            )
        ),
        ReleaseLog(
            version = "v1.7.0",
            date = "25 Kasım 2026",
            isLatest = false,
            highlights = listOf(
                "Uygulama İçi %100 Erişilebilir Medya & Podcast Oynatıcı: Kütüphane sekmesinde harici uygulamaya gerek kalmadan 10 sn ileri/geri sarma, oynatma hızı seçimi (1x, 1.25x, 1.5x, 2x), arka planda sırayla çalma ve TalkBack Eylemler menüsü desteği.",
                "İndirmeyi İptal Et & Canlı Bildirim Çubuğu Desteği: Devam eden indirmeyi anında iptal edebilme ve uygulama arka plandayken bildirim çubuğunda canlı indirme yüzdesi takibi.",
                "Toplu İndirme Kuyruğu (Multi-Link Queue): Birden fazla bağlantıyı aynı anda yapıştırarak veya indirme sürerken yeni medya ekleyerek sırayla otomatik indirme."
            )
        ),
        ReleaseLog(
            version = "v1.6.1",
            date = "24 Kasım 2026",
            isLatest = false,
            highlights = listOf(
                "Otomatik Klavye Kapatma: Bağlantı yapıştırıldığında, 'Medyayı İncele'ye basıldığında veya sekme değiştirildiğinde ekran klavyesinin otomatik kapanması sağlandı.",
                "Hata ve Tanılama Kayıtlarını Gönder (Log Gönder): Ayarlar sekmesine cihaz bilgilerini, uygulama içi olay günlüğünü ve sistem hatalarını tek dokunuşla geliştirici sunucusuna ileten tanılama sistemi eklendi.",
                "Android 7.0 – 16 ve Redmi/MIUI Doğrudan İndirme Motoru optimizasyonları."
            )
        ),
        ReleaseLog(
            version = "v1.6.0",
            date = "24 Kasım 2026",
            isLatest = false,
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
