package com.baverelci.accessgrab.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.baverelci.accessgrab.R
import com.baverelci.accessgrab.ui.components.AccessibleButton
import com.baverelci.accessgrab.ui.components.ChangeLogDialog
import com.baverelci.accessgrab.ui.components.UserGuideDialog
import com.baverelci.accessgrab.viewmodel.MainViewModel

@Composable
fun AboutScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showChangeLog by remember { mutableStateOf(false) }
    var showUserGuide by remember { mutableStateOf(false) }

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "2.0.0"
        } catch (_: Exception) {
            "2.0.0"
        }
    }

    // Sürüm Notları (ChangeLog) Diyaloğu
    if (showChangeLog) {
        ChangeLogDialog(onDismiss = { showChangeLog = false })
    }

    // Kullanım ve Kısayol Rehberi Bilgi Kartı Diyaloğu (#33)
    if (showUserGuide) {
        UserGuideDialog(onDismiss = { showUserGuide = false })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Başlık ve Logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "AccessGrab uygulama logosu",
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Column {
                Text(
                    text = "AccessGrab",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "Sürüm $versionName (Erişilebilir Yapı)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        // Sürüm Notları, Kullanım Rehberi, Paylaşım ve Kaynak Kodu Kartı
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AccessibleButton(
                    text = "Kullanım ve Kısayol Rehberi",
                    contentDesc = "AccessGrab kullanım ve TalkBack kısayol rehberi bilgi kartını aç",
                    icon = Icons.Default.HelpOutline,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = { showUserGuide = true }
                )

                AccessibleButton(
                    text = "AccessGrab Uygulamasını Arkadaşına Paylaş",
                    contentDesc = "AccessGrab uygulamasının indirme bağlantısını WhatsApp veya diğer uygulamalarla arkadaşına paylaş",
                    icon = Icons.Default.Share,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    onClick = { viewModel.shareApp(context) }
                )

                AccessibleButton(
                    text = "Sürüm Notları & Yenilikler (ChangeLog)",
                    contentDesc = "Tüm sürümlerdeki yenilikleri ve değişiklik günlüğünü görüntüle",
                    icon = Icons.Default.Info,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = { showChangeLog = true }
                )

                AccessibleButton(
                    text = "Açık Kaynak Kodları (GitHub)",
                    contentDesc = "AccessGrab açık kaynak kod deposunu GitHub üzerinde aç",
                    icon = Icons.Default.Language,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        val gitHubIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bawer-elci/AccessGrab"))
                        context.startActivity(gitHubIntent)
                    }
                )
            }
        }

        // Geliştirici Kartı
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Geliştirici",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Baver Elçi",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Yazılım Geliştirici & Erişilebilirlik Savunucusu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(14.dp))
                AccessibleButton(
                    text = "Resmi Web Sitesini Ziyaret Et",
                    contentDesc = "Baver Elçi resmi web sitesini tarayıcıda aç: baverelci nokta com nokta tr",
                    icon = Icons.Default.Language,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://baverelci.com.tr"))
                        context.startActivity(browserIntent)
                    }
                )
            }
        }

        // #50 — Geliştiricinin Diğer Erişilebilir Projeleri Kartı
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Geliştiricinin Diğer Erişilebilir Projeleri",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.semantics { heading() }
                    )
                }

                val otherProjects = listOf(
                    "Erişilebilir APK Yükleyici (ADB Studio): Bilgisayardan Android cihazlara ekran okuyucu uyumlu tek tıkla APK kurma ve cihaz yönetim aracı.",
                    "Erişilebilir SSH Studio: Görme engelli sistem yöneticileri ve geliştiriciler için ekran okuyucu dostu Linux sunucu yönetim arayüzü.",
                    "Kod Öğren Akademisi: Görme engelli bireyler için sıfırdan erişilebilir yazılım ve algoritma eğitim platformu.",
                    "Akıllı Ulaşım Sesli Anons Sistemi: Toplu taşıma araçlarında görme engelliler için konum tabanlı durak ve güzergah sesli bilgilendirme çözümü."
                )

                otherProjects.forEach { proj ->
                    Text(
                        text = "• $proj",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                AccessibleButton(
                    text = "Tüm Erişilebilir Projeleri İncele",
                    contentDesc = "Baver Elçi'nin geliştirdiği tüm erişilebilir yazılım projelerini web sitesinde incele",
                    icon = Icons.Default.Language,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        val projectsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://baverelci.com.tr"))
                        context.startActivity(projectsIntent)
                    }
                )
            }
        }

        // Proje Misyonu & Erişilebilirlik Kartı
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessibilityNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Erişilebilirlik Misyonu",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "AccessGrab; görme engelli bireyler, TalkBack kullanıcıları ve sade bir deneyim arayan herkes için geliştirilmiştir. Uygulama içinde yanıltıcı reklamlar, labirent gibi menüler veya etiketsiz butonlar bulunmaz. Her kontrol öğesi ekran okuyucular ile %100 uyumludur.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Desteklenen Platformlar Kartı
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Desteklenen Platformlar",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                val platforms = listOf(
                    "Instagram: Reels, Gönderi ve Hikayeler",
                    "TikTok: Filigransız (Logosuz) HD Video ve MP3 Ses",
                    "YouTube & Shorts: Yüksek Kalite Video ve Podcast MP3",
                    "X (Twitter): Videolar ve Hareketli GIF'ler",
                    "Facebook: Herkese Açık Video ve Reels Paylaşımları"
                )

                platforms.forEach { item ->
                    Text(
                        text = "• $item",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
