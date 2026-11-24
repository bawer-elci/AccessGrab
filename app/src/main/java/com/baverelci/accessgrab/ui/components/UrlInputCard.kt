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
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun UrlInputCard(
    urlInput: String,
    onUrlChanged: (String) -> Unit,
    onClearInput: () -> Unit,
    onFetchMedia: () -> Unit,
    onPasteAndFetch: ((String) -> Unit)? = null,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun readClipboardText(): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        return clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
    }

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
                // Görsel ikonlar gören kullanıcılar için kutunun sağında korunur;
                // TalkBack kullanıcıları bu işlemlere doğrudan kutunun Eylemler menüsünden erişir.
                Row(modifier = Modifier.clearAndSetSemantics { }) {
                    if (urlInput.isNotEmpty()) {
                        IconButton(onClick = onClearInput) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val clip = readClipboardText()
                            if (clip.isNotBlank()) {
                                onUrlChanged(clip)
                            }
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
                    val actions = mutableListOf<CustomAccessibilityAction>()
                    actions.add(
                        CustomAccessibilityAction("Panodan Yapıştır ve Hemen İncele") {
                            val clip = readClipboardText()
                            if (clip.isNotBlank()) {
                                if (onPasteAndFetch != null) {
                                    onPasteAndFetch(clip)
                                } else {
                                    onUrlChanged(clip)
                                    onFetchMedia()
                                }
                            }
                            true
                        }
                    )
                    actions.add(
                        CustomAccessibilityAction("Panodan Yapıştır") {
                            val clip = readClipboardText()
                            if (clip.isNotBlank()) {
                                onUrlChanged(clip)
                            }
                            true
                        }
                    )
                    if (urlInput.isNotBlank()) {
                        actions.add(
                            CustomAccessibilityAction("Medyayı İncele") {
                                onFetchMedia()
                                true
                            }
                        )
                        actions.add(
                            CustomAccessibilityAction("Metni Temizle") {
                                onClearInput()
                                true
                            }
                        )
                    }
                    this.customActions = actions
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
