package com.baverelci.accessgrab.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun UrlInputCard(
    urlInput: String,
    onUrlChanged: (String) -> Unit,
    onClearInput: () -> Unit,
    onFetchMedia: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = urlInput,
            onValueChange = onUrlChanged,
            label = { Text("Bağlantı Adresi (URL)") },
            placeholder = { Text("Instagram, TikTok, YouTube, X, Facebook linki") },
            singleLine = false,
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            trailingIcon = {
                Row {
                    if (urlInput.isNotEmpty()) {
                        IconButton(
                            onClick = onClearInput,
                            modifier = Modifier.semantics {
                                this.contentDescription = "Metin alanını temizle düğmesi"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            if (clip.isNotBlank()) {
                                onUrlChanged(clip)
                            }
                        },
                        modifier = Modifier.semantics {
                            this.contentDescription = "Panodan yapıştır düğmesi"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    this.contentDescription = "Sosyal medya bağlantısı giriş kutusu. ${if (urlInput.isEmpty()) "Şu an boş" else "İçerik: $urlInput"}"
                }
        )

        Spacer(modifier = Modifier.height(14.dp))

        AccessibleButton(
            text = "Medyayı İncele",
            contentDesc = "Girilen bağlantıdaki medyayı incele ve indirme seçeneklerini getir",
            icon = Icons.Default.Search,
            onClick = onFetchMedia,
            enabled = !isLoading
        )
    }
}
