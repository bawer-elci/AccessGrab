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
            version = "v1.4.0",
            date = "22 Kasım 2026",
            isLatest = true,
            highlights = listOf(
                "Video Çözünürlük Seçenekleri: 1080p (FHD), 720p (HD), 480p (SD) ve 360p arasında serbestçe seçim yapabilme.",
                "Ses Bit Hızı Seçenekleri: 320 kbps (Yüksek Kalite), 192 kbps (Standart) ve 128 kbps (Tasarruf) seçenekleri.",
                "Uygulama İçi Sürüm Notları (ChangeLog): Her sürümdeki yenilik ve değişiklikleri doğrudan uygulama içinden takip edebilme.",
                "Gelişmiş TalkBack Desteği: Seçilen format ve kalite için dinamik ve anlaşılır sesli geri bildirimler.",
                "İndirme motoru hız, kararlılık ve bellek optimizasyonları."
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
