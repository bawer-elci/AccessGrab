package com.baverelci.accessgrab.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun ClipboardPromptCard(
    url: String,
    onUseUrl: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onUseUrl)
            .semantics {
                this.contentDescription = "Panoda medya bağlantısı algılandı: $url."
                this.role = Role.Button
                this.customActions = listOf(
                    CustomAccessibilityAction("Bu Bağlantıyı Kullan") {
                        onUseUrl()
                        true
                    },
                    CustomAccessibilityAction("Öneriyi Kapat") {
                        onDismiss()
                        true
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .clearAndSetSemantics { }
        ) {
            Text(
                text = "Panoda medya bağlantısı algılandı:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = url,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AccessibleButton(
                    text = "Bu Bağlantıyı Kullan",
                    contentDesc = "Panodaki bağlantıyı kullan ve medyayı getir",
                    onClick = onUseUrl,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.weight(1f)
                )
                AccessibleButton(
                    text = "Kapat",
                    contentDesc = "Pano önerisini kapat",
                    onClick = onDismiss,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(0.5f)
                )
            }
        }
    }
}
