package com.baverelci.accessgrab.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun UserGuideDialog(
    onDismiss: () -> Unit
) {
    val guideItems = listOf(
        "1. Doğrudan Kelimeyle veya Sesle Arama" to "YouTube veya sosyal medyaya girip link kopyalamanıza gerek yoktur. İndir sekmesindeki kutuya doğrudan şarkı, sanatçı veya video adını yazabilir ya da 'Sesle Ara' butonuna basıp konuşarak sonuçları listeleyebilirsiniz.",
        "2. TalkBack Eylemler Menüsü ile Hızlı Kontrol" to "Arama sonuçlarında, medya bilgisi kartında veya Kütüphane'deki bir dosyanın üzerindeyken parmağınızı yukarı/aşağı kaydırarak (TalkBack Eylemler menüsüyle) doğrudan MP3/MP4 indirebilir, önizleme dinleyebilir, paylaşabilir veya zil sesi yapabilirsiniz.",
        "3. İndirmeden Önce Canlı Önizleme" to "Arama sonuçlarında 'Diğer Seçenekler' menüsünden veya TalkBack Eylemler'den 'İndirmeden Önce Önizle / Dinle' seçeneğiyle dosyayı telefona kaydetmeden önce dinleyip doğru kayıt olduğundan emin olabilirsiniz.",
        "4. İki Parmakla Çift Dokunma & Sallayarak Kontrol" to "Oynatıcı açıkken ekranda buton aramaya gerek kalmadan iki parmağınızla ekrana çift dokunarak, kulaklık tuşuna basarak veya telefonu elinizde hafifçe sallayarak medyayı duraklatıp başlatabilirsiniz.",
        "5. %200 Ses Güçlendirici ve Kaldığım Yerden Devam Et" to "Oynatıcı penceresindeki 'Ses (%150 / %200)' butonuyla kısık sesli kayıtları güçlendirebilirsiniz. Uzun ders ve sohbet kayıtlarını kapattığınızda uygulama kaldığınız saniyeyi hatırlar ve bir sonraki açılışta oradan devam eder.",
        "6. 30 Saniye Nakarat Kesici & Zil / Alarm / Bildirim Sesi" to "Oynatıcıda şarkıyı dinlerken tam sevdiğiniz nakarat saniyesinde 'Bu Saniyeden 30 Sn Zil Sesi Yap' butonuna basarak o bölümü kesip zil sesi yapabilirsiniz. Kütüphane'deki 'Diğer Seçenekler' menüsünden Zil Sesi, Alarm Sesi veya Bildirim Sesi atayabilir ve istediğiniz an tek dokunuşla eski sesinize dönebilirsiniz."
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessibilityNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Kullanım ve Kısayol Rehberi",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() }
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                guideItems.forEach { (title, desc) ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.semantics { heading() }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            AccessibleButton(
                text = "Kapat",
                contentDesc = "Kullanım rehberi bilgi penceresini kapat",
                icon = Icons.Default.Close,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = onDismiss
            )
        },
        shape = RoundedCornerShape(18.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
